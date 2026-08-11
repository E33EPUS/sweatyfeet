package com.e33epus.sweatyfeet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.junit.jupiter.api.Disabled;
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
        assertEquals(120 * 20, SfConfig.LEVEL_1_SECONDS * 20);
        assertEquals(240 * 20, SfConfig.LEVEL_2_SECONDS * 20);
        assertEquals(360 * 20, SfConfig.LEVEL_3_SECONDS * 20);
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
        int retention = SfConfig.SLIDE_RETENTION_PERCENT;
        assertTrue(retention >= 1 && retention <= 100, "retention must be 1-100, was " + retention);
    }

    @Disabled("Fabric 纯 JUnit 无 Fabric Loader，ItemStack 静态初始化需完整官方 AW（NeoForge 端保留）")
    @Test
    void drinkTypeDefaultsToSpeed() {
        assertEquals("speed", SweatDrinkItem.readType(net.minecraft.item.ItemStack.EMPTY));
    }

    @Disabled("Fabric 纯 JUnit 无 Fabric Loader，ItemStack 静态初始化需完整官方 AW（NeoForge 端保留）")
    @Test
    void bootMaterialMapsToFlavorId() {
        assertEquals("leather", SweatyFeetHandler.flavorIdFor(new ItemStack(Items.LEATHER_BOOTS)));
        assertEquals("iron", SweatyFeetHandler.flavorIdFor(new ItemStack(Items.IRON_BOOTS)));
        assertEquals("gold", SweatyFeetHandler.flavorIdFor(new ItemStack(Items.GOLDEN_BOOTS)));
        assertEquals("diamond", SweatyFeetHandler.flavorIdFor(new ItemStack(Items.DIAMOND_BOOTS)));
        assertEquals("netherite", SweatyFeetHandler.flavorIdFor(new ItemStack(Items.NETHERITE_BOOTS)));
        // 非原版五材质靴子（如链甲无靴）→ plain（无风味组件）
        assertEquals("plain", SweatyFeetHandler.flavorIdFor(new ItemStack(Items.CHAINMAIL_BOOTS)));
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

    @Disabled("Fabric 纯 JUnit 无 Fabric Loader，ItemStack 静态初始化需完整官方 AW（NeoForge 端保留）")
    @Test
    void drinkTypeReadsCustomComponent() {
        ItemStack strength = new ItemStack(Items.GLASS_BOTTLE);
        strength.set(ModDataComponents.DRINK_TYPE, "strength");
        assertEquals("strength", SweatDrinkItem.readType(strength));

        ItemStack unknown = new ItemStack(Items.GLASS_BOTTLE);
        unknown.set(ModDataComponents.DRINK_TYPE, "bogus");
        assertEquals("speed", SweatDrinkItem.readType(unknown)); // 未知类型归一化到 speed
    }
    @Disabled("Fabric 纯 JUnit 无法初始化状态效果注册表（StatusEffect.<clinit> 需要 Fabric Loader；NeoForge 端保留）")
    @Test
    void bottleEffectsByLevelAndFlavor() {
        // 0.1.4 三级质变：1 级无效果；2 级反胃；3 级反胃+毒；3 级风味叠加风味效果
        assertTrue(SweatBottleItem.effectsFor(1, null).isEmpty());
        var lvl2 = SweatBottleItem.effectsFor(2, null);
        assertEquals(1, lvl2.size());
        assertEquals(net.minecraft.entity.effect.StatusEffects.NAUSEA, lvl2.get(0).getEffectType());
        var lvl3 = SweatBottleItem.effectsFor(3, null);
        assertEquals(2, lvl3.size());
        assertEquals(net.minecraft.entity.effect.StatusEffects.POISON, lvl3.get(1).getEffectType());
        var sulfur = SweatBottleItem.effectsFor(3, "netherite");
        assertEquals(3, sulfur.size());
        assertEquals(net.minecraft.entity.effect.StatusEffects.FIRE_RESISTANCE, sulfur.get(2).getEffectType());
        var iron = SweatBottleItem.effectsFor(3, "iron");
        assertEquals(net.minecraft.entity.effect.StatusEffects.WEAKNESS, iron.get(2).getEffectType());
    }

}
