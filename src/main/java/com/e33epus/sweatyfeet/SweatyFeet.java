package com.e33epus.sweatyfeet;

import me.shedaniel.autoconfig.AutoConfig;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(SweatyFeet.MOD_ID)
public class SweatyFeet {
    public static final String MOD_ID = "sweatyfeet";

    public SweatyFeet(IEventBus modBus, ModContainer container) {
        ModDataComponents.register(modBus);
        ModEffects.register(modBus);
        ModEntities.register(modBus);
        ModItems.register(modBus);

        // 客户端：把 Cloth Config 生成的配置界面挂到 Mods 列表的 Config 按钮
        if (FMLEnvironment.dist == Dist.CLIENT) {
            container.registerExtensionPoint(IConfigScreenFactory.class,
                (modContainer, parent) -> AutoConfig.getConfigScreen(SfConfig.class, parent).get());
        }
    }
}
