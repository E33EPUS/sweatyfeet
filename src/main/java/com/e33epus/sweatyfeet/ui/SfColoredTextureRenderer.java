package com.e33epus.sweatyfeet.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * 带整体透明度/tint 的纹理渲染（1.21.1 版，对应 e33chat 1.20.1 的 ColoredTextureRenderer）：
 * 1.21.1 移除了公开的 positionColorTexShader，改走 RenderSystem.setShaderColor + GuiGraphics.blit，
 * 每次调用后 g.flush() 强制按当时颜色上屏（batch 延迟提交会串色）。
 */
public final class SfColoredTextureRenderer {

    private SfColoredTextureRenderer() {}

    public static void drawWithAlpha(GuiGraphics g, ResourceLocation tex,
                                     int x, int y, int w, int h, float alpha) {
        if (w <= 0 || h <= 0 || alpha <= 0.003f) return;
        // 用 GuiGraphics.setColor（内部 flushIfManaged 保序）而非裸 RenderSystem.setShaderColor，
        // 否则 alpha 会被 batch 提交时序吞掉（见 1.21.1 innerBlit 的 POSITION_TEX shader 读 COLOR_MODULATOR）
        g.setColor(1f, 1f, 1f, alpha);
        g.blit(tex, x, y, w, h, 0f, 0f, 16, 16, 16, 16);
        g.flush();
        g.setColor(1f, 1f, 1f, 1f);
    }

    public static void drawTinted(GuiGraphics g, ResourceLocation tex,
                                  int x, int y, int w, int h, int argb) {
        if (w <= 0 || h <= 0) return;
        float a = (argb >>> 24) / 255f;
        float r = (argb >> 16 & 0xFF) / 255f;
        float gr = (argb >> 8 & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        g.setColor(r, gr, b, a);
        g.blit(tex, x, y, w, h, 0f, 0f, 16, 16, 16, 16);
        g.flush();
        g.setColor(1f, 1f, 1f, 1f);
    }
}
