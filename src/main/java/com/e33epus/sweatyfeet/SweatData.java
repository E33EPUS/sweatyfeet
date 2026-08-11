package com.e33epus.sweatyfeet;

import org.jetbrains.annotations.Nullable;
import net.minecraft.text.Text;

/**
 * 汗液状态组件：靴子上存在此组件 = 已汗化。
 * level：汗化时的汗脚等级（0/1/2 = 1/2/3 级）——倒汗时按它产出对应等级汗液瓶，
 *         不随穿戴计时变化，脱鞋再穿也保持（靴子已汗化，等级固化在物品上）。
 * originalName：汗化前玩家的自定义名，倒汗时原样还原，不破坏玩家改名。
 */
public record SweatData(int level, @Nullable Text originalName) {
    public static final SweatData EMPTY = new SweatData(0, null);

    /** 汗化时把当前汗脚等级固化进组件（升级时也同步更新） */
    public SweatData withLevel(int newLevel) {
        return new SweatData(newLevel, originalName);
    }
}
