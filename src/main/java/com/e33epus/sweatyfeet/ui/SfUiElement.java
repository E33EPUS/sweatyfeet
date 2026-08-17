package com.e33epus.sweatyfeet.ui;

import com.e33epus.sweatyfeet.SweatyFeet;
import net.minecraft.resources.ResourceLocation;

/**
 * 配置界面 UI 纹理元素：路径约定 assets/sweatyfeet/textures/gui/dark/{path}.png。
 * 16×16 PNG，与 e33chat 的 gui/dark 资源一致。
 */
public enum SfUiElement {
    DIVIDER("divider"),
    SCROLLBAR_TRACK("scrollbar_track"),
    SCROLLBAR_THUMB("scrollbar_thumb"),
    HOVER_BG("hover_bg");

    private final String path;

    SfUiElement(String path) {
        this.path = path;
    }

    /** 纹理 ID（带 .png——SimpleTexture 原样查资源，不自动补后缀）。 */
    public ResourceLocation rl(SfTheme theme) {
        return ResourceLocation.fromNamespaceAndPath(SweatyFeet.MOD_ID,
            "textures/gui/" + theme.name().toLowerCase() + "/" + path + ".png");
    }
}
