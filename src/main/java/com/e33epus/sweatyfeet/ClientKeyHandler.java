package com.e33epus.sweatyfeet;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

/**
 * 客户端按键处理：J 键打开配置界面。
 * END_CLIENT_TICK 时输入状态已更新，wasPressed() 拿"按下瞬间"可靠；
 * 游戏中按 J 以当前画面为返回页，关闭后回游戏。
 */
public final class ClientKeyHandler {
    private ClientKeyHandler() {
    }

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (SweatyFeetClient.OPEN_CONFIG.wasPressed()) {
                // 已开配置界面就不要再叠一层；sweatyfeet 配置屏不含输入框，任意界面都能覆盖
                if (!(mc.currentScreen instanceof SfConfigScreen)) {
                    mc.setScreen(new SfConfigScreen(mc.currentScreen));
                }
            }
        });
    }
}
