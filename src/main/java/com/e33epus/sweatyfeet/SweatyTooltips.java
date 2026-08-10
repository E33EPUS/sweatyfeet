package com.e33epus.sweatyfeet;

import java.util.List;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

/**
 * tooltip 行工具：按 lang key 是否存在决定显示哪几行（tooltip1/2/3…）。
 * 行数由语言文件决定——删 key 自动少一行、加 key 自动多一行，不用改代码。
 * appendHoverText 只在客户端渲染路径调用，可安全用 I18n.exists。
 */
public final class SweatyTooltips {
    private SweatyTooltips() {
    }

    public static void addIfPresent(List<Component> tooltip, String key) {
        if (I18n.exists(key)) {
            tooltip.add(Component.translatable(key));
        }
    }
}
