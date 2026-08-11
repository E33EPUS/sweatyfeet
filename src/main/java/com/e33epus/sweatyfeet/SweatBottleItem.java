package com.e33epus.sweatyfeet;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.text.Text;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

/**
 * 汗液瓶（分级 1/2/3，等级存 SWEAT_LEVEL 组件，仿原版药水分级模式）：
 * - 1 级：喝回半格饱食度
 * - 2 级：+ 反胃 10 秒
 * - 3 级：+ 中毒 3 秒
 * 普通右键喝（仰头动画 + 咕嘟音效）。喝后回空玻璃瓶。
 * 来源：穿汗靴副手空瓶右键倒汗（等级 = 汗脚等级）。
 */
public class SweatBottleItem extends Item {
    public SweatBottleItem(Item.Settings properties) {
        super(properties);
    }

    /** 读瓶等级（默认 1，越界钳制到 1-3） */
    static int getLevel(ItemStack stack) {
        Integer lvl = stack.get(ModDataComponents.SWEAT_LEVEL);
        return lvl == null ? 1 : clampLevel(lvl);
    }

    /** 纯逻辑：等级钳制到 1-3（供单元测试） */
    static int clampLevel(int lvl) {
        return Math.max(1, Math.min(3, lvl));
    }

    /** 按等级+风味构造药水效果列表（POTION_CONTENTS 显示 + finishUsing 应用共用，写入时固化） */
    public static List<StatusEffectInstance> effectsFor(int lvl, String flavor) {
        List<StatusEffectInstance> effects = new ArrayList<>();
        if (lvl >= 2) {
            effects.add(new StatusEffectInstance(StatusEffects.NAUSEA, SfConfig.BOTTLE_NAUSEA_SECONDS * 20, 0));
        }
        if (lvl >= 3) {
            effects.add(new StatusEffectInstance(StatusEffects.POISON, SfConfig.DRINK_POISON_SECONDS * 20, 0));
        }
        // 风味附加效果（0.1.2+）：材质特性演绎，叠在等级效果之上；无风味（plain）不加
        if (flavor != null) {
            switch (flavor) {
                case "iron" -> effects.add(new StatusEffectInstance(StatusEffects.WEAKNESS, 15 * 20, 0));          // 铁锈：锈到没力气
                case "gold" -> effects.add(new StatusEffectInstance(StatusEffects.LUCK, 60 * 20, 0));              // 金贵：金=好运
                case "diamond" -> effects.add(new StatusEffectInstance(StatusEffects.RESISTANCE, 20 * 20, 0)); // 凛冽：硬=耐打
                case "netherite" -> effects.add(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 30 * 20, 0)); // 硫磺：下界火抗
                default -> { /* 未知/plain：无附加 */ }
            }
        }
        return effects;
    }

    /** 写药水组件（tooltip 自动显示效果；无效果的瓶不写，保持原色） */
    public static void setPotionContents(ItemStack stack, int lvl, String flavor) {
        List<StatusEffectInstance> effects = effectsFor(lvl, flavor);
        if (!effects.isEmpty()) {
            stack.set(DataComponentTypes.POTION_CONTENTS,
                new PotionContentsComponent(Optional.empty(), Optional.empty(), effects));
        }
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    /** 必须返回 DRINK 才有原版"仰头喝"动画（动画由 getUseAnimation 决定，与物品标签/组件无关） */
    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.DRINK;
    }

    @Override
    public TypedActionResult<ItemStack> use(World level, PlayerEntity player, Hand hand) {
        player.setCurrentHand(hand);
        return TypedActionResult.consume(player.getStackInHand(hand));
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World level, LivingEntity entity) {
        int lvl = getLevel(stack);
        String flavor = stack.get(ModDataComponents.SWEAT_FLAVOR);
        if (entity instanceof PlayerEntity player) {
            // 1 级：回半格饱食度；醇厚（皮革）发酵更顶饱 → 翻倍
            player.getHungerManager().add("leather".equals(flavor) ? 2 : 1, 0.1F);
        }
        if (!level.isClient) {
            // 有点咸...：喝汗液瓶进度（consume_item 必须显式 trigger，且要在 stack.consume 前传本体）
            if (entity instanceof net.minecraft.server.network.ServerPlayerEntity sp) {
                net.minecraft.advancement.criterion.Criteria.CONSUME_ITEM.trigger(sp, stack);
            }
            PotionContentsComponent pc = stack.get(DataComponentTypes.POTION_CONTENTS);
            if (pc != null) {
                // 0.1.4+：效果在倒汗时固化进组件（与 tooltip 显示一致，配置改动只影响新瓶）
                pc.forEachEffect(entity::addStatusEffect);
            } else {
                // fallback：旧瓶（0.1.3-）无组件，按等级老逻辑
                if (lvl >= 2) {
                    entity.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, SfConfig.BOTTLE_NAUSEA_SECONDS * 20, 0));
                }
                if (lvl >= 3) {
                    entity.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, SfConfig.DRINK_POISON_SECONDS * 20, 0));
                }
                if (flavor != null) {
                    switch (flavor) {
                        case "iron" -> entity.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 15 * 20, 0));
                        case "gold" -> entity.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, 60 * 20, 0));
                        case "diamond" -> entity.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 20 * 20, 0));
                        case "netherite" -> entity.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 30 * 20, 0));
                        default -> { /* 未知/plain：无附加 */ }
                    }
                }
            }
            // 音效：1 级像吃东西（回饱食度），2/3 级像喝药水
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                lvl == 1 ? SoundEvents.ENTITY_GENERIC_EAT : SoundEvents.ENTITY_GENERIC_DRINK,
                SoundCategory.PLAYERS, 1.0F, 1.0F);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.ENTITY_PLAYER_BURP, SoundCategory.PLAYERS, 1.0F, 1.0F);
        }
        stack.decrement(1);
        return Items.GLASS_BOTTLE.getDefaultStack();
    }

    @Override
    public String getTranslationKey(ItemStack stack) {
        return super.getTranslationKey(stack) + "." + getLevel(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, java.util.List<Text> tooltip,
                                TooltipType flag) {
        // 药水效果行（0.1.4+：POTION_CONTENTS 自动显示效果+时长，与喝下去的效果一致）
        PotionContentsComponent pc = stack.get(DataComponentTypes.POTION_CONTENTS);
        if (pc != null) {
            pc.buildTooltip(tooltip::add, 1.0F, 20.0F);
        }
        // 风味 lore（倒汗时按靴子材质写入组件；无组件 = 朴素瓶不显示）
        String flavor = stack.get(ModDataComponents.SWEAT_FLAVOR);
        if (flavor != null) {
            SweatyTooltips.addIfPresent(tooltip, "item.sweatyfeet.flavor_desc." + flavor);
        }
        // 简介行 + 使用方式行（分行）
        SweatyTooltips.addIfPresent(tooltip, "item.sweatyfeet.sweat_bottle.tooltip1");
        SweatyTooltips.addIfPresent(tooltip, "item.sweatyfeet.sweat_bottle.tooltip2");
    }
}
