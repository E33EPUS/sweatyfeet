package com.e33epus.sweatyfeet;

import net.fabricmc.api.ModInitializer;

public class SweatyFeet implements ModInitializer {
    public static final String MOD_ID = "sweatyfeet";

    @Override
    public void onInitialize() {
        // 注册顺序：数据组件 → 效果 → 实体 → 物品 → 方块（方块物品注册在 ModBlocks）
        ModDataComponents.init();
        ModEffects.init();
        ModEntities.init();
        ModItems.init();
        ModBlocks.init();
        // 服务端玩法逻辑 + 配置 + 网络
        SfConfig.init();
        ModNetworking.init();
        SweatyFeetHandler.init();
    }
}
