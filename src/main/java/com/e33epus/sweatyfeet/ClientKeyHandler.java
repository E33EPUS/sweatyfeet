package com.e33epus.sweatyfeet;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * 客户端按键处理（GAME 总线）：J 键打开配置界面。
 * ClientTickEvent.Post 时输入状态已更新，consumeClick() 拿"按下瞬间"可靠；
 * 游戏中按 J 以当前画面为返回页，关闭后回游戏。
 */
@EventBusSubscriber(modid = SweatyFeet.MOD_ID, value = Dist.CLIENT)
public final class ClientKeyHandler {
    private ClientKeyHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        while (SweatyFeetClient.OPEN_CONFIG.consumeClick()) {
            Minecraft mc = Minecraft.getInstance();
            // 已开配置界面就不要再叠一层；sweatyfeet 配置屏不含输入框，任意界面都能覆盖
            if (!(mc.screen instanceof SfConfigScreen)) {
                mc.setScreen(new SfConfigScreen(mc.screen));
            }
        }
    }
}
