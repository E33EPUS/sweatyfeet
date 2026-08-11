package com.e33epus.sweatyfeet.integration;

import com.e33epus.sweatyfeet.SfConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** ModMenu 配置入口：Mods 列表 → Sweaty Feet → Config 按钮打开自绘配置屏 */
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new SfConfigScreen(parent);
    }
}
