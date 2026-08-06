package com.e33epus.sweatyfeet;

import java.nio.ByteBuffer;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.opengl.GL11;

/**
 * 滴管取色器（借鉴 needsofnature 的皮肤展开图取色）：把玩家皮肤展开图放大渲染，
 * 点击任意像素 → glReadPixels 读屏取色 → 写进 soak_undress_tint 配置。
 * Esc 取消。纯客户端。
 */
public class SoakSkinPickerScreen extends Screen {
    private final Player player;
    private final Screen backTo;
    private ResourceLocation skinRl;
    private NativeImage skinImg;
    private int sqX, sqY, sqSize;
    private int hoverHex;
    private boolean hoverValid;

    public SoakSkinPickerScreen(Player player, Screen backTo) {
        super(Component.translatable("sweatyfeet.picker.title"));
        this.player = player;
        this.backTo = backTo;
    }

    @Override
    protected void init() {
        super.init();
        // 直接解码磁盘缓存的原始皮肤图（不经屏幕渲染/后处理），点击读的是原始色号
        skinImg = SoakSkinClient.loadBaseImageSync(player);
        if (skinImg != null) {
            skinRl = ResourceLocation.fromNamespaceAndPath(SweatyFeet.MOD_ID,
                "picker_skin/" + player.getUUID());
            Minecraft.getInstance().getTextureManager().register(skinRl,
                new net.minecraft.client.renderer.texture.DynamicTexture(skinImg));
        }
        sqSize = Math.min(width - 40, height - 80);
        sqSize -= sqSize % 64;
        sqX = (width - sqSize) / 2;
        sqY = (height - sqSize) / 2 - 10;
        addRenderableWidget(Button.builder(Component.translatable("sweatyfeet.picker.cancel"), b -> onClose())
            .bounds(width / 2 - 50, sqY + sqSize + 8, 100, 20).build());
    }

    /** 置空：1.21.1 的 renderBackground 会调 renderBlurredBackground 全屏后处理模糊，
     *  皮肤展开图全糊（e33chat 已验证的修复方案）。背景在 render 里手画 */
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xB0000000);
        g.drawCenteredString(font, title, width / 2, sqY - 24, 0xFFFFFF);
        g.drawCenteredString(font, Component.translatable("sweatyfeet.picker.hint"), width / 2, sqY - 12, 0xAAAAAA);
        if (skinRl == null) {
            g.drawCenteredString(font, Component.translatable("sweatyfeet.picker.noskin"), width / 2, height / 2, 0xFF5555);
            return;
        }
        RenderSystem.enableBlend();
        // 整张皮肤展开图放大铺满方块（纹理 64x64 → sqSize）
        g.blit(skinRl, sqX, sqY, sqSize, sqSize, 0, 0, 64, 64, 64, 64);
        RenderSystem.disableBlend();
        g.renderOutline(sqX, sqY, sqSize, sqSize, 0xFFFFFFFF);

        hoverValid = mouseX >= sqX && mouseX < sqX + sqSize && mouseY >= sqY && mouseY < sqY + sqSize;
        if (hoverValid) {
            hoverHex = pickRawPixel(mouseX, mouseY);
            int px = sqX + sqSize + 6;
            g.fill(px, mouseY - 8, px + 16, mouseY + 8, 0xFF000000 | hoverHex);
            g.renderOutline(px, mouseY - 8, 16, 16, 0xFFFFFFFF);
            g.drawString(font, String.format("#%06X", hoverHex), px + 22, mouseY - 4, 0xFFFFFF);
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    /** 鼠标逻辑坐标 → 皮肤图原始像素（blit 左上原点 = NativeImage 左上原点，直接换算）。
     *  读 NativeImage 原始色号，绕过屏幕渲染/后处理/gamma 加工（实测读屏会偏色） */
    private int pickRawPixel(int logicalX, int logicalY) {
        if (skinImg == null) {
            return 0;
        }
        int px = (logicalX - sqX) * 64 / sqSize;
        int py = (logicalY - sqY) * 64 / sqSize;
        if (px < 0 || px >= 64 || py < 0 || py >= skinImg.getHeight()) {
            return 0;
        }
        int c = skinImg.getPixelRGBA(px, py);
        return ((c >>> 24) & 0xFF) << 16 | ((c >>> 16) & 0xFF) << 8 | ((c >>> 8) & 0xFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // 点击现算像素——不能用渲染帧缓存的 hoverHex：鼠标移到目标上立即点击时，
            // hoverHex 还是上一帧悬停在旧位置（如粉色/紫色区）的色号，
            // 导致"显示蓝色、提取紫红"（实测根因）。
            int px = (int) mouseX;
            int py = (int) mouseY;
            if (px >= sqX && px < sqX + sqSize && py >= sqY && py < sqY + sqSize) {
                int rgb = pickRawPixel(px, py) & 0xFFFFFF;
                if (SfConfig.DEBUG_UNDRESS.get()) {
                    com.mojang.logging.LogUtils.getLogger().info(
                        "[SF] picker click ({},{}) -> #{:06X}", px, py, rgb);
                }
                SfConfig.SOAK_UNDRESS_TINT.set(String.format("#%06X", rgb));
                SfConfig.SERVER_SPEC.save(); // 持久化：ModConfigSpec.save() 写盘
                onClose();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(backTo);
    }

    @Override
    public void removed() {
        if (skinImg != null) {
            skinImg.close();
            skinImg = null;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
