package com.e33epus.sweatyfeet;

import me.shedaniel.autoconfig.AutoConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(SweatyFeet.MOD_ID)
public class SweatyFeet {
    public static final String MOD_ID = "sweatyfeet";
    private static final Logger LOGGER = LoggerFactory.getLogger(SweatyFeet.class);

    public SweatyFeet(IEventBus modBus, ModContainer container) {
        ModDataComponents.register(modBus);
        ModEffects.register(modBus);
        ModEntities.register(modBus);
        ModItems.register(modBus);

        // 把 Cloth Config 生成的配置界面挂到 Mods 列表的 Config 按钮。
        // 不判 dist：registerExtensionPoint 只是存 Supplier，服务端不调用 createScreen，安全。
        container.registerExtensionPoint(IConfigScreenFactory.class,
            (modContainer, parent) -> {
                try {
                    return AutoConfig.getConfigScreen(SfConfig.class, parent).get();
                } catch (Exception e) {
                    LOGGER.error("Failed to open Sweaty Feet config screen", e);
                    return parent;
                }
            });
    }
}
