package com.e33epus.sweatyfeet;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * 客户端专属注册（@EventBusSubscriber value = CLIENT → 服务端不加载本类，不触达客户端类）。
 * 把配置界面挂到 Mods 列表 → Config 按钮。
 */
@EventBusSubscriber(modid = SweatyFeet.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class SweatyFeetClient {
    private SweatyFeetClient() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() ->
            ModList.get().getModContainerById(SweatyFeet.MOD_ID).ifPresent(container ->
                container.registerExtensionPoint(IConfigScreenFactory.class,
                    (modContainer, parent) -> new SfConfigScreen(parent))));
    }
}
