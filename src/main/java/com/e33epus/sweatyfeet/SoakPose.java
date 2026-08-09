package com.e33epus.sweatyfeet;

/**
 * 坐凳泡脚姿势（纯客户端观感，纯函数便于单测）。
 * 原 player-animator 版（assets/sweatyfeet/player_animations/soaking_sit.json，
 * 40 tick 循环）的关键帧全是 INOUTSINE 插值，等价于标准正弦：
 * - 双腿以 40 tick 为周期反相小幅摆动（pitch 基准 -75°，摆幅 ±10°）
 * - 身体恒定后仰 -10°、双臂恒定抬到 -20° 前摆（左右臂 roll 各 ∓5°）
 * 用三角函数直接算，不再需要 PA 的关键帧系统与前置。
 */
public final class SoakPose {
    /** 循环周期（tick），与原 40 tick 动画一致 */
    public static final int PERIOD_TICKS = 40;

    private SoakPose() {
    }

    /** 各部位弧度（Minecraft ModelPart 用弧度） */
    public static final class Parts {
        public float rightLegXRot;
        public float leftLegXRot;
        public float bodyXRot;
        public float rightArmXRot;
        public float leftArmXRot;
        public float rightArmZRot;
        public float leftArmZRot;
    }

    public static Parts at(float tickCount, float partialTick) {
        // 相位：40 tick 一个完整循环（2π）；partialTick 插值保证动画与帧率无关、不卡顿
        float phase = (float) (2.0 * Math.PI * (tickCount + partialTick) / PERIOD_TICKS);
        float swing = (float) Math.sin(phase);

        Parts p = new Parts();
        // 左右反相：tick 10（sin=1）右腿前伸 -65°、左腿收回 -85°；tick 30 互换
        p.rightLegXRot = DEG_TO_RAD(-75.0F + 10.0F * swing);
        p.leftLegXRot = DEG_TO_RAD(-75.0F - 10.0F * swing);
        // 恒定姿势
        p.bodyXRot = DEG_TO_RAD(-10.0F);
        p.rightArmXRot = DEG_TO_RAD(-20.0F);
        p.leftArmXRot = DEG_TO_RAD(-20.0F);
        p.rightArmZRot = DEG_TO_RAD(5.0F);
        p.leftArmZRot = DEG_TO_RAD(-5.0F);
        return p;
    }

    private static float DEG_TO_RAD(float deg) {
        return deg * (float) Math.PI / 180.0F;
    }
}
