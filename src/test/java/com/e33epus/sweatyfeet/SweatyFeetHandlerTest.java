package com.e33epus.sweatyfeet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
        assertEquals(120 * 20, SfConfig.LEVEL_1_SECONDS.getDefault() * 20);
        assertEquals(240 * 20, SfConfig.LEVEL_2_SECONDS.getDefault() * 20);
        assertEquals(360 * 20, SfConfig.LEVEL_3_SECONDS.getDefault() * 20);
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
        int retention = SfConfig.SLIDE_RETENTION_PERCENT.getDefault();
        assertTrue(retention >= 1 && retention <= 100, "retention must be 1-100, was " + retention);
    }

    @Test
    void drinkTypeDefaultsToSpeed() {
        assertEquals("speed", SweatDrinkItem.readType(net.minecraft.world.item.ItemStack.EMPTY));
    }

    @Test
    void degradeCountsDownSameLevel() {
        // 3 级（amp 2）脱鞋：倒计时中，等级不变
        SweatyFeetHandler.DegradeResult r = SweatyFeetHandler.nextDegradeState(2, 60, 60);
        assertEquals(2, r.amplifier());
        assertEquals(59, r.ticksLeft());
    }

    @Test
    void degradeStepsDownOneLevelAtZero() {
        // 3 级（amp 2）倒计时到头 → 降 2 级（amp 1），重置一个阶段
        SweatyFeetHandler.DegradeResult r = SweatyFeetHandler.nextDegradeState(2, 1, 60);
        assertEquals(1, r.amplifier());
        assertEquals(60, r.ticksLeft());
    }

    @Test
    void degradeKeepsLevelOneInsteadOfRemoving() {
        // 1 级（amp 0）倒计时到头 → 保留 1 级并重置满时长（汗脚只能洗脚彻底清，脱鞋永远清不干净）
        SweatyFeetHandler.DegradeResult r = SweatyFeetHandler.nextDegradeState(0, 1, 60);
        assertEquals(0, r.amplifier());
        assertEquals(60, r.ticksLeft());
    }

    @Test
    void degradeFullChainFromLevelThree() {
        // 完整链路：3 级(amp2) 60 tick → 2 级(amp1) 60 tick → 1 级(amp0) 60 tick → 保留 1 级
        var r = SweatyFeetHandler.nextDegradeState(2, 1, 60); // → 2级
        assertEquals(1, r.amplifier());
        r = SweatyFeetHandler.nextDegradeState(r.amplifier(), 1, 60); // → 1级
        assertEquals(0, r.amplifier());
        r = SweatyFeetHandler.nextDegradeState(r.amplifier(), 1, 60); // → 保留 1 级
        assertEquals(0, r.amplifier());
        assertEquals(60, r.ticksLeft());
    }

    @Test
    void degradeStepsDownAtZeroTicks() {
        // 倒计时归零那一瞬间也降级（ticksLeft=0 与 1 等价，防边界漏掉）
        SweatyFeetHandler.DegradeResult r = SweatyFeetHandler.nextDegradeState(2, 0, 60);
        assertEquals(1, r.amplifier());
        assertEquals(60, r.ticksLeft());
    }

    @Test
    void drinkTypeReadsCustomComponent() {
        ItemStack strength = new ItemStack(Items.GLASS_BOTTLE);
        strength.set(ModDataComponents.DRINK_TYPE.get(), "strength");
        assertEquals("strength", SweatDrinkItem.readType(strength));

        ItemStack unknown = new ItemStack(Items.GLASS_BOTTLE);
        unknown.set(ModDataComponents.DRINK_TYPE.get(), "bogus");
        assertEquals("speed", SweatDrinkItem.readType(unknown)); // 未知类型归一化到 speed
    }
}
