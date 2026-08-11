package com.e33epus.sweatyfeet;

import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.option.KeyBinding;
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
    }
}
