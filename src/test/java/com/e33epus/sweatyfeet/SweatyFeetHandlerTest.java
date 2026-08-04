package com.e33epus.sweatyfeet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * 汗脚等级推进的纯逻辑测试：锁定阈值边界，防止像"== 改成 >="这类回归
 * 再悄悄破坏触发条件。跑法：./gradlew test -PrunTests
 */
class SweatyFeetHandlerTest {
    private static final int L2 = 240 * 20; // 默认 4 分钟
    private static final int L3 = 360 * 20; // 默认 6 分钟

    @Test
    void freshWearIsLevelOne() {
        assertEquals(0, SweatyFeetHandler.computeAmplifier(0, L2, L3));
    }

    @Test
    void justBelowLevelTwoThresholdIsLevelOne() {
        assertEquals(0, SweatyFeetHandler.computeAmplifier(L2 - 1, L2, L3));
    }

    @Test
    void exactlyLevelTwoThresholdIsLevelTwo() {
        assertEquals(1, SweatyFeetHandler.computeAmplifier(L2, L2, L3));
    }

    @Test
    void justBelowLevelThreeThresholdIsLevelTwo() {
        assertEquals(1, SweatyFeetHandler.computeAmplifier(L3 - 1, L2, L3));
    }

    @Test
    void exactlyLevelThreeThresholdIsLevelThree() {
        assertEquals(2, SweatyFeetHandler.computeAmplifier(L3, L2, L3));
    }

    @Test
    void beyondLevelThreeStaysLevelThree() {
        assertEquals(2, SweatyFeetHandler.computeAmplifier(L3 * 10, L2, L3));
    }

    @Test
    void defaultConfigSecondsConvertToTicks() {
        SfConfig c = new SfConfig();
        assertEquals(120 * 20, c.level1_seconds * 20);
        assertEquals(240 * 20, c.level2_seconds * 20);
        assertEquals(360 * 20, c.level3_seconds * 20);
    }

    @Test
    void bottleLevelClampsToRange() {
        assertEquals(1, SweatBottleItem.clampLevel(0));
        assertEquals(1, SweatBottleItem.clampLevel(1));
        assertEquals(2, SweatBottleItem.clampLevel(2));
        assertEquals(3, SweatBottleItem.clampLevel(3));
        assertEquals(3, SweatBottleItem.clampLevel(99));
        assertEquals(1, SweatBottleItem.clampLevel(-5));
    }

    @Test
    void defaultSlideRetentionDoesNotExceedBounds() {
        SfConfig c = new SfConfig();
        int retention = c.slide_retention_percent;
        assertTrue(retention >= 1 && retention <= 100, "retention must be 1-100, was " + retention);
    }
}
