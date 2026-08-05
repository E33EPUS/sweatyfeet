package com.e33epus.sweatyfeet;

import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * 客户端专属注册（@EventBusSubscriber value = CLIENT → 服务端不加载本类，不触达客户端类）。
 * 配置界面挂到 Mods 列表 → Config 按钮；泡脚动画层挂到每个客户端玩家。
 */
@EventBusSubscriber(modid = SweatyFeet.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class SweatyFeetClient {
    private SweatyFeetClient() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ModList.get().getModContainerById(SweatyFeet.MOD_ID).ifPresent(container ->
                container.registerExtensionPoint(IConfigScreenFactory.class,
                    (modContainer, parent) -> new SfConfigScreen(parent)));

            // 泡脚动画层（每个客户端玩家一个，key = soak_layer，SoakAnimationClient 用它触发/停止）
            // 没装 player-animator 不注册 → SoakAnimationClient 里 ModList 守卫直接 return
            if (ModList.get().isLoaded("playeranimator")) {
                PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
                    ResourceLocation.fromNamespaceAndPath(SweatyFeet.MOD_ID, "soak_layer"),
                    42,
                    player -> new ModifierLayer<>());
            }
        });
    }
}
