package com.e33epus.sweatyfeet;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
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

        // 客户端：Cloth Config 注册（官方要求 init 时注册、服务端禁用 AutoConfig）
        if (FMLEnvironment.dist == Dist.CLIENT) {
            initClientConfig(container);
        }
    }

    private static void initClientConfig(ModContainer container) {
        AutoConfig.register(SfConfig.class, GsonConfigSerializer::new);
        SfConfig.INSTANCE = AutoConfig.getConfigHolder(SfConfig.class).getConfig();
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
