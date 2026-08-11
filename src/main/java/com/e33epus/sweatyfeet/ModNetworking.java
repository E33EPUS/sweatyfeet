package com.e33epus.sweatyfeet;

import io.netty.buffer.ByteBuf;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

/**
 * 跨端下半身颜色同步：本地玩家坐凳时把 SOAK_UNDRESS_TINT 上报服务端 → 服务端无状态转发广播
 * 给该维度所有玩家 → 其他客户端渲染该玩家时用广播来的色（syncedTint），保证所有人看到同一个颜色。
 * 协议是转发制（服务端不存状态）：坐凳玩家每 20 tick 重报一次 → 新登录玩家最多等 1 秒收到，
 * 无需登录全量推送；离凳停报（本来就不显示）。
 */
public final class ModNetworking {
    /** 客户端收到的其他玩家 tint（uuid -> hex）；本地玩家自己的最终也走这里（广播回环后 == 自己配置） */
    private static final Map<UUID, String> SYNCED_TINT = new ConcurrentHashMap<>();

    private ModNetworking() {
    }

    /** 双端注册：play 包类型 + 服务端 C2S 接收（client 环境 registerGlobalReceiver 无副作用） */
    public static void init() {
        PayloadTypeRegistry.playC2S().register(ReportTint.TYPE, ReportTint.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncTint.TYPE, SyncTint.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ReportTint.TYPE, ReportTint::handle);
    }

    /** 客户端注册 S2C 接收（只在 client entrypoint 调） */
    public static void initClient() {
        ClientPlayNetworking.registerGlobalReceiver(SyncTint.TYPE, SyncTint::handle);
    }

    /** 渲染某玩家时优先用广播来的色；没有返回 null（走本地配置） */
    public static String syncedTint(UUID playerId) {
        return SYNCED_TINT.get(playerId);
    }

    /** 客户端上报自己的 tint（坐凳时 20-tick 节流 + 滴管选色后立即发） */
    public static void reportTint(String hex) {
        if (hex == null || hex.isBlank()) {
            return;
        }
        ClientPlayNetworking.send(new ReportTint(hex));
    }

    /** C2S：本地玩家上报自己的 tint */
    public record ReportTint(String hex) implements CustomPayload {
        public static final CustomPayload.Id<ReportTint> TYPE = new CustomPayload.Id<>(Identifier.of(SweatyFeet.MOD_ID, "report_tint"));
        public static final PacketCodec<ByteBuf, ReportTint> STREAM_CODEC =
            PacketCodec.tuple(PacketCodecs.STRING, ReportTint::hex, ReportTint::new);

        @Override
        public CustomPayload.Id<ReportTint> getId() {
            return TYPE;
        }

        public static void handle(ReportTint payload, ServerPlayNetworking.Context ctx) {
            ctx.server().execute(() -> {
                ServerPlayerEntity p = ctx.player();
                ServerWorld sl = p.getServerWorld();
                // 无状态转发广播（含上报者自己：回环后本地 SYNCED_TINT 与自己配置一致）
                for (ServerPlayerEntity target : sl.getPlayers()) {
                    ServerPlayNetworking.send(target, new SyncTint(p.getUuid(), payload.hex()));
                }
            });
        }
    }

    /** S2C：服务端广播某玩家的 tint */
    public record SyncTint(UUID playerId, String hex) implements CustomPayload {
        public static final CustomPayload.Id<SyncTint> TYPE = new CustomPayload.Id<>(Identifier.of(SweatyFeet.MOD_ID, "sync_tint"));
        // 1.21.1 PacketCodecs 无 UUID → 自定义：两个 long（最/次有效位）
        public static final PacketCodec<ByteBuf, UUID> UUID_CODEC = PacketCodec.of(
            (u, buf) -> {
                buf.writeLong(u.getMostSignificantBits());
                buf.writeLong(u.getLeastSignificantBits());
            },
            buf -> new UUID(buf.readLong(), buf.readLong()));
        public static final PacketCodec<ByteBuf, SyncTint> STREAM_CODEC =
            PacketCodec.tuple(UUID_CODEC, SyncTint::playerId,
                PacketCodecs.STRING, SyncTint::hex, SyncTint::new);

        @Override
        public CustomPayload.Id<SyncTint> getId() {
            return TYPE;
        }

        public static void handle(SyncTint payload, ClientPlayNetworking.Context ctx) {
            ctx.client().execute(() -> {
                if (payload.hex() == null || payload.hex().isBlank()) {
                    SYNCED_TINT.remove(payload.playerId());
                } else {
                    SYNCED_TINT.put(payload.playerId(), payload.hex());
                }
            });
        }
    }
}
