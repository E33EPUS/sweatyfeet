package com.e33epus.sweatyfeet;

import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.lwjgl.glfw.GLFW;

/**
 * 客户端专属注册（@EventBusSubscriber value = CLIENT → 服务端不加载本类，不触达客户端类）。
 * 配置界面挂到 Mods 列表 → Config 按钮；泡脚动画层挂到每个客户端玩家；J 键打开配置界面。
 */
@EventBusSubscriber(modid = SweatyFeet.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class SweatyFeetClient {
    /** J 键打开配置界面（按下逻辑在 ClientKeyHandler 的 ClientTickEvent.Post） */
    public static final KeyMapping OPEN_CONFIG = new KeyMapping(
        "key.sweatyfeet.open_config", GLFW.GLFW_KEY_J, "key.categories.sweatyfeet");

    private SweatyFeetClient() {
    }

    @SubscribeEvent
    public static void onKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_CONFIG);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ModList.get().getModContainerById(SweatyFeet.MOD_ID).ifPresent(container ->
                container.registerExtensionPoint(IConfigScreenFactory.class,
                    (modContainer, parent) -> new SfConfigScreen(parent)));
        });
    }

    /** 盆里的清水面 tint 成主世界水蓝（vanilla water_still 是灰白贴图靠 tint 上色） */
    @SubscribeEvent
    public static void onBlockColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Block event) {
        net.minecraft.client.color.block.BlockColor waterTint = (state, level, pos, tintIndex) ->
            tintIndex == 0 ? 0xFF3F76E4 : 0xFFFFFFFF;
        event.register(waterTint, ModBlocks.WASH_BASIN.get());
    }
}
