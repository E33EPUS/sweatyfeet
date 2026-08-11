package com.e33epus.sweatyfeet;

import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import java.util.Comparator;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * 客户端专属注册（fabric client entrypoint，服务端不加载本类）。
 * J 键打开配置界面（ModMenu → Config 按钮也进同一界面）；泡脚动画层挂到每个客户端玩家。
 */
public final class SweatyFeetClient implements ClientModInitializer {
    /** J 键打开配置界面（按下逻辑在 ClientKeyHandler） */
    public static final KeyBinding OPEN_CONFIG = new KeyBinding(
        "key.sweatyfeet.open_config", GLFW.GLFW_KEY_J, "key.categories.sweatyfeet");

    public SweatyFeetClient() {
    }

    @Override
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(OPEN_CONFIG);
        ClientKeyHandler.init();
        ModRenderers.init();
        ModNetworking.initClient();

        // 汗脚等级角标：vanilla 效果图标渲染完后叠画 I/II/III（图标右上角）
        HudRenderCallback.EVENT.register((ctx, tickCounter) -> drawSweatLevelBadge(ctx));

        // 泡脚动画层（每个客户端玩家一个，key = soak_layer，SoakAnimationClient 用它触发/停止）
        // 没装 player-animator 不注册 → SoakAnimationClient 里守卫直接 return
        if (FabricLoader.getInstance().isModLoaded("playeranimator")) {
            PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
                Identifier.of(SweatyFeet.MOD_ID, "soak_layer"),
                42,
                player -> new ModifierLayer<>());
        }

        // 盆里的清水面 tint 成主世界水蓝（vanilla water_still 是灰白贴图靠 tint 上色）
        ColorProviderRegistry.BLOCK.register((state, world, pos, tintIndex) ->
            tintIndex == 0 ? 0xFF3F76E4 : 0xFFFFFFFF, ModBlocks.WASH_BASIN);

        // 风味瓶/饮品药水色（tintIndex 0 = layer0，vanilla 药水语义）：
        // 醇厚=棕、铁锈=红褐、金贵=亮黄、凛冽=青蓝、硫磺=橙金(火抗药水色)、饮品=麦金
        ColorProviderRegistry.ITEM.register((stack, tintIndex) -> {
            if (tintIndex != 0) {
                return -1;
            }
            String flavor = stack.get(ModDataComponents.SWEAT_FLAVOR);
            if (flavor != null) {
                return switch (flavor) {
                    case "leather" -> 0xFF8B5A2B;
                    case "iron" -> 0xFFB7410E;
                    case "gold" -> 0xFFFFD700;
                    case "diamond" -> 0xFF4FC3F7;
                    case "netherite" -> 0xFFE49A3A;
                    default -> -1;
                };
            }
            if (stack.isOf(ModItems.SWEAT_DRINK)) {
                return 0xFFF5C542; // 汗液饮品：麦金
            }
            return -1;
        }, ModItems.SWEAT_BOTTLE, ModItems.SWEAT_DRINK);
    }

    /** 汗脚等级角标：与 vanilla renderStatusEffectOverlay 同一套布局重算图标位置，在图标右上角叠画 I/II/III */
    private static void drawSweatLevelBadge(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        PlayerEntity p = mc.player;
        if (p == null) {
            return;
        }
        StatusEffectInstance sf = p.getStatusEffect(ModEffects.SWEATY_FEET);
        if (sf == null || !sf.shouldShowIcon()) {
            return;
        }
        // 复制 vanilla 遍历序（时长降序）与布局（有益右上第一行，有害第二行 y+26，每项 25px）
        int beneficial = 0;
        int harmful = 0;
        int badgeX = -1;
        int badgeY = -1;
        for (StatusEffectInstance e : p.getStatusEffects().stream().sorted(Comparator.reverseOrder()).toList()) {
            if (!e.shouldShowIcon()) {
                continue;
            }
            if (e.getEffectType().value().isBeneficial()) {
                badgeX = ctx.getScaledWindowWidth() - 25 * (++beneficial);
                badgeY = 1;
            } else {
                badgeX = ctx.getScaledWindowWidth() - 25 * (++harmful);
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
        int w = mc.textRenderer.getWidth(badge);
        // 图标 18x18 画于 (x+3, y+3)，角标贴右上角
        ctx.drawText(mc.textRenderer, badge, badgeX + 21 - w, badgeY + 4, 0xFFFFFF, true);
    }
}
