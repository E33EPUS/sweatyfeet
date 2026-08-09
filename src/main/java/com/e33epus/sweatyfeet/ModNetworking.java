package com.e33epus.sweatyfeet;

import io.netty.buffer.ByteBuf;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 跨端下半身颜色同步：本地玩家坐凳时把 SOAK_UNDRESS_TINT 上报服务端 → 服务端无状态转发广播
 * 给该维度所有玩家 → 其他客户端渲染该玩家时用广播来的色（syncedTint），保证所有人看到同一个颜色。
 * 协议是转发制（服务端不存状态）：坐凳玩家每 20 tick 重报一次 → 新登录玩家最多等 1 秒收到，
 * 无需登录全量推送；离凳停报（本来就不显示）。
 */
@EventBusSubscriber(modid = SweatyFeet.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class ModNetworking {
    /** 客户端收到的其他玩家 tint（uuid -> hex）；本地玩家自己的最终也走这里（广播回环后 == 自己配置） */
    private static final Map<UUID, String> SYNCED_TINT = new ConcurrentHashMap<>();

    private ModNetworking() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar("1");
        r.playToServer(ReportTint.TYPE, ReportTint.STREAM_CODEC, ReportTint::handle);
        r.playToClient(SyncTint.TYPE, SyncTint.STREAM_CODEC, SyncTint::handle);
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
        PacketDistributor.sendToServer(new ReportTint(hex));
    }

    /** C2S：本地玩家上报自己的 tint */
    public record ReportTint(String hex) implements CustomPacketPayload {
        public static final Type<ReportTint> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(SweatyFeet.MOD_ID, "report_tint"));
        public static final StreamCodec<ByteBuf, ReportTint> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, ReportTint::hex, ReportTint::new);

        @Override
        public Type<ReportTint> type() {
            return TYPE;
        }

        public static void handle(ReportTint payload, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                Player p = ctx.player();
                if (p == null || !(p.level() instanceof ServerLevel sl)) {
                    return;
                }
                // 无状态转发广播（含上报者自己：回环后本地 SYNCED_TINT 与自己配置一致）
                PacketDistributor.sendToPlayersInDimension(sl, new SyncTint(p.getUUID(), payload.hex()));
            });
        }
    }

    /** S2C：服务端广播某玩家的 tint */
    public record SyncTint(UUID playerId, String hex) implements CustomPacketPayload {
        public static final Type<SyncTint> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(SweatyFeet.MOD_ID, "sync_tint"));
        // 1.21.1 ByteBufCodecs 无 UUID → 自定义：两个 long（最/次有效位）
        public static final StreamCodec<ByteBuf, UUID> UUID_CODEC = StreamCodec.of(
            (buf, u) -> {
                buf.writeLong(u.getMostSignificantBits());
                buf.writeLong(u.getLeastSignificantBits());
            },
            buf -> new UUID(buf.readLong(), buf.readLong()));
        public static final StreamCodec<ByteBuf, SyncTint> STREAM_CODEC =
            StreamCodec.composite(UUID_CODEC, SyncTint::playerId,
                ByteBufCodecs.STRING_UTF8, SyncTint::hex, SyncTint::new);

        @Override
        public Type<SyncTint> type() {
            return TYPE;
        }

        public static void handle(SyncTint payload, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (payload.hex() == null || payload.hex().isBlank()) {
                    SYNCED_TINT.remove(payload.playerId());
                } else {
                    SYNCED_TINT.put(payload.playerId(), payload.hex());
                }
            });
        }
    }
}
