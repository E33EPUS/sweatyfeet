package com.e33epus.sweatyfeet;

import dev.kosmx.playerAnim.api.IPlayable;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 泡脚动画（纯客户端表现）：赤脚站在洗脚盆上 → 腿部交替划水循环。
 * 需要 player-animator 前置，没装只是没动画，玩法不受影响（ModList 守卫 + optional 依赖）。
 */
@EventBusSubscriber(modid = SweatyFeet.MOD_ID, value = Dist.CLIENT)
public final class SoakAnimationClient {
    private SoakAnimationClient() {
    }

    /** 动画层 id：与 SweatyFeetClient 里 registerFactory 的 key 一致 */
    private static final ResourceLocation LAYER_ID =
        ResourceLocation.fromNamespaceAndPath(SweatyFeet.MOD_ID, "soak_layer");

    /** 对应 assets/sweatyfeet/player_animations/soaking.json 的 name 字段 */
    private static final ResourceLocation SOAKING_ANIM =
        ResourceLocation.fromNamespaceAndPath(SweatyFeet.MOD_ID, "soaking");

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!ModList.get().isLoaded("playeranimator")) {
            return;
        }
        if (!(event.getEntity() instanceof AbstractClientPlayer player)) {
            return;
        }
        ModifierLayer<IAnimation> layer = (ModifierLayer<IAnimation>) PlayerAnimationAccess
            .getPlayerAssociatedData(player).get(LAYER_ID);
        if (layer == null) {
            return;
        }
        // 泡脚动画条件：赤脚 + 站在盆上 + 盆里有清水（与 3b 服务端泡脚会话语义一致：
        // 有水才泡，洗完水变浑/被舀走就停）
        var stateOn = player.getBlockStateOn();
        boolean soaking = stateOn.is(ModBlocks.WASH_BASIN.get())
            && stateOn.getValue(WashBasinBlock.FILLED) == WashBasinBlock.Filled.WATER
            && player.getItemBySlot(EquipmentSlot.FEET).isEmpty();
        if (soaking) {
            if (layer.getAnimation() == null) {
                IPlayable anim = PlayerAnimationRegistry.getAnimation(SOAKING_ANIM);
                if (anim != null) {
                    layer.setAnimation((KeyframeAnimationPlayer) anim.playAnimation());
                }
            }
        } else if (layer.getAnimation() != null) {
            layer.setAnimation(null);
        }
    }
}
