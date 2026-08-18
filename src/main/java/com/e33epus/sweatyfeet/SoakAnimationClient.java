package com.e33epus.sweatyfeet;

import dev.kosmx.playerAnim.api.IPlayable;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.util.Identifier;

/**
 * 坐姿泡脚动画（纯客户端表现）：骑在泡脚椅座位上 + 赤脚 → 腿小幅摆动、身体后仰轻晃。
 * 座位 NBT（basinPos）不同步客户端，这里只判"骑着座位 + 赤脚"，洗没洗由服务端算。
 * 需要 player-animator 前置（required 依赖，没装客户端不加载）。
 */
public final class SoakAnimationClient {
    private SoakAnimationClient() {
    }

    /** 动画层 id：与 SweatyFeetClient 里 registerFactory 的 key 一致 */
    private static final Identifier LAYER_ID =
        Identifier.of(SweatyFeet.MOD_ID, "soak_layer");

    /** 对应 assets/sweatyfeet/player_animations/soaking_sit.json 的 name 字段 */
    private static final Identifier SOAKING_SIT_ANIM =
        Identifier.of(SweatyFeet.MOD_ID, "soaking_sit");

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (!FabricLoader.getInstance().isModLoaded("playeranimator")) {
                return;
            }
            AbstractClientPlayerEntity player = mc.player;
            if (player == null) {
                return;
            }
            ModifierLayer<IAnimation> layer = (ModifierLayer<IAnimation>) PlayerAnimationAccess
                .getPlayerAssociatedData(player).get(LAYER_ID);
            if (layer == null) {
                return;
            }
            boolean soaking = player.getVehicle() instanceof SeatEntity
                && player.getEquippedStack(EquipmentSlot.FEET).isEmpty();
            if (soaking) {
                if (layer.getAnimation() == null) {
                    IPlayable anim = PlayerAnimationRegistry.getAnimation(SOAKING_SIT_ANIM);
                    if (anim != null) {
                        layer.setAnimation((KeyframeAnimationPlayer) anim.playAnimation());
                    }
                }
            } else if (layer.getAnimation() != null) {
                layer.setAnimation(null);
            }
        });

        // 玩家下线：清掉 SoakSkinClient 的皮肤缓存（UUID 复用防 FAILED/PENDING 卡死）
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (client.player != null) {
                SoakSkinClient.clearFor(client.player.getUuid());
            }
        });

        // 进世界预热：后台拉取本地玩家皮肤，坐下时改图通常已就绪（脱裤不再等首次下载）
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            SoakSkinClient.prefetch(client.player);
        });
    }
}
