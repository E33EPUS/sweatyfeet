package com.e33epus.sweatyfeet.ui;

import net.minecraft.util.Identifier;

/** 配置界面纹理管理：固定 DARK 主题（无主题切换）。 */
public final class SfUiTextureManager {

    private SfUiTextureManager() {}

    public static Identifier rl(SfUiElement el) {
        return el.rl(SfTheme.DARK);
    }
}
