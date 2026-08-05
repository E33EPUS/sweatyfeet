package com.e33epus.sweatyfeet;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(SweatyFeet.MOD_ID)
public class SweatyFeet {
    public static final String MOD_ID = "sweatyfeet";

    public SweatyFeet(IEventBus modBus, ModContainer container) {
        ModDataComponents.register(modBus);
        ModEffects.register(modBus);
        ModEntities.register(modBus);
        ModItems.register(modBus);
        ModBlocks.register(modBus);

        // 服务端配置（玩法逻辑全在服务端），NeoForge 内置 ModConfigSpec，零前置
        container.registerConfig(ModConfig.Type.SERVER, SfConfig.SERVER_SPEC);
        // 客户端界面注册在 SweatyFeetClient（@EventBusSubscriber Dist.CLIENT）——主类是双端必加载，
        // 不能在这里 new 客户端类（Screen），否则服务端 RuntimeDistCleaner 崩
    }
}
