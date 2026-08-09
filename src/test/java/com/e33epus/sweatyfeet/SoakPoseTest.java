package com.e33epus.sweatyfeet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 坐凳泡脚姿势纯逻辑：周期闭合、左右反相、摆幅点位、恒定姿势值。
 * 期望角度一律从度数换算（与 SoakPose 同源），锁定原 PA soaking_sit.json 的关键帧行为：
 * tick0/20/40 双腿 -75°，tick10 右 -65° 左 -85°，tick30 右 -85° 左 -65°，body -10°，臂 -20°/roll ∓5°。
 */
class SoakPoseTest {

    private static float deg(float d) {
        return d * (float) Math.PI / 180.0F;
    }

    private static final float DELTA = 1e-5F;

    @Test
    void basePoseAtPhaseZero() {
        SoakPose.Parts p = SoakPose.at(0, 0);
        assertEquals(deg(-75), p.rightLegXRot, DELTA);
        assertEquals(deg(-75), p.leftLegXRot, DELTA);
        assertEquals(deg(-10), p.bodyXRot, DELTA);
        assertEquals(deg(-20), p.rightArmXRot, DELTA);
        assertEquals(deg(-20), p.leftArmXRot, DELTA);
        assertEquals(deg(5), p.rightArmZRot, DELTA);
        assertEquals(deg(-5), p.leftArmZRot, DELTA);
    }

    @Test
    void keyframePointsMatchOriginalAnimation() {
        // tick 10（θ=π/2，sin=1）：右腿前伸 -65°，左腿收回 -85°
        SoakPose.Parts t10 = SoakPose.at(10, 0);
        assertEquals(deg(-65), t10.rightLegXRot, DELTA);
        assertEquals(deg(-85), t10.leftLegXRot, DELTA);
        // tick 20 回到基准
        SoakPose.Parts t20 = SoakPose.at(20, 0);
        assertEquals(deg(-75), t20.rightLegXRot, DELTA);
        assertEquals(deg(-75), t20.leftLegXRot, DELTA);
        // tick 30（θ=3π/2，sin=-1）：左右互换
        SoakPose.Parts t30 = SoakPose.at(30, 0);
        assertEquals(deg(-85), t30.rightLegXRot, DELTA);
        assertEquals(deg(-65), t30.leftLegXRot, DELTA);
    }

    @Test
    void cycleClosesAfterFullPeriod() {
        SoakPose.Parts a = SoakPose.at(0, 0);
        SoakPose.Parts b = SoakPose.at(SoakPose.PERIOD_TICKS, 0);
        assertEquals(a.rightLegXRot, b.rightLegXRot, DELTA);
        assertEquals(a.leftLegXRot, b.leftLegXRot, DELTA);
        // partialTick 插值等价于推进相位：at(0,20) == at(20,0)
        SoakPose.Parts c = SoakPose.at(0, 20);
        assertEquals(a.rightLegXRot, c.rightLegXRot, DELTA);
        assertEquals(a.leftLegXRot, c.leftLegXRot, DELTA);
    }

    @Test
    void legsSwingInOppositePhaseAroundSameBase() {
        SoakPose.Parts p = SoakPose.at(10, 0);
        // 反相：右腿前伸时左腿后收
        assertTrue(p.rightLegXRot > p.leftLegXRot);
        // 均值守恒：两腿始终围绕 -75° 对称
        float avg = (p.rightLegXRot + p.leftLegXRot) / 2.0F;
        assertEquals(deg(-75), avg, DELTA);
    }

    @Test
    void staticPosesDoNotChangeOverTime() {
        for (int tick : new int[]{0, 5, 13, 27, 39}) {
            SoakPose.Parts p = SoakPose.at(tick, 0.5F);
            assertEquals(deg(-10), p.bodyXRot, DELTA);
            assertEquals(deg(-20), p.rightArmXRot, DELTA);
            assertEquals(deg(-20), p.leftArmXRot, DELTA);
            assertEquals(deg(5), p.rightArmZRot, DELTA);
            assertEquals(deg(-5), p.leftArmZRot, DELTA);
        }
    }
}
