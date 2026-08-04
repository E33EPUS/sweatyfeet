package com.e33epus.sweatyfeet;

import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;

/**
 * 汗液状态组件：靴子上存在此组件 = 已汗化。
 * 只存汗化前玩家的自定义名，倒汗时原样还原，不破坏玩家改名。
 */
public record SweatData(@Nullable Component originalName) {
    public static final SweatData EMPTY = new SweatData(null);
}
