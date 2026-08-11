package com.e33epus.sweatyfeet;

import java.util.Comparator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;

/**
 * 汗脚等级角标：vanilla 效果图标渲染完后，在汗脚图标右上角叠画 I/II/III。
 * 布局与 vanilla InGameHud.renderStatusEffectOverlay 一致：有益效果右上第一行（每项 25px），
 * 有害效果第二行（y+26），图标 18x18 画于背景 (x+3, y+3)。
 */
@EventBusSubscriber(modid = SweatyFeet.MOD_ID, value = Dist.CLIENT)
public final class SweatyHudOverlay {
    private SweatyHudOverlay() {
    }

    @SubscribeEvent
    public static void onHud(RenderGuiLayerEvent.Post event) {
        drawSweatLevelBadge(event.getGuiGraphics());
    }

    private static void drawSweatLevelBadge(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        Player p = mc.player;
        if (p == null) {
            return;
        }
        MobEffectInstance sf = p.getEffect(ModEffects.SWEATY_FEET);
        if (sf == null || !sf.showIcon()) {
            return;
        }
        // 复制 vanilla 遍历序（时长降序）与布局
        int beneficial = 0;
        int harmful = 0;
        int badgeX = -1;
        int badgeY = -1;
        for (MobEffectInstance e : p.getActiveEffects().stream().sorted(Comparator.reverseOrder()).toList()) {
            if (!e.showIcon()) {
                continue;
            }
            if (e.getEffect().value().isBeneficial()) {
                badgeX = g.guiWidth() - 25 * (++beneficial);
                badgeY = 1;
            } else {
                badgeX = g.guiWidth() - 25 * (++harmful);
                badgeY = 1 + 26;
            }
            if (e == sf) {
                break;
            }
        }
        if (badgeX < 0) {
            return;
        }
        String badge = sf.getAmplifier() == 2 ? "III" : sf.getAmplifier() == 1 ? "II" : "I";
        int w = mc.font.width(badge);
        g.drawString(mc.font, badge, badgeX + 21 - w, badgeY + 4, 0xFFFFFF, true);
    }
}
