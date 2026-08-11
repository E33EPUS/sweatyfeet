package com.e33epus.sweatyfeet;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * 汗液瓶（分级 1/2/3，等级存 SWEAT_LEVEL 组件，仿原版药水分级模式）：
 * - 1 级：喝回半格饱食度
 * - 2 级：+ 反胃 10 秒
 * - 3 级：+ 中毒 3 秒
 * 普通右键喝（仰头动画 + 咕嘟音效）。喝后回空玻璃瓶。
 * 来源：穿汗靴副手空瓶右键倒汗（等级 = 汗脚等级）。
 */
public class SweatBottleItem extends Item {
    public SweatBottleItem(Properties properties) {
        super(properties);
    }

    /** 读瓶等级（默认 1，越界钳制到 1-3） */
    static int getLevel(ItemStack stack) {
        Integer lvl = stack.get(ModDataComponents.SWEAT_LEVEL.get());
        return lvl == null ? 1 : clampLevel(lvl);
    }

    /** 纯逻辑：等级钳制到 1-3（供单元测试） */
    static int clampLevel(int lvl) {
        return Math.max(1, Math.min(3, lvl));
    }

    /** 按等级+风味构造药水效果列表（POTION_CONTENTS 显示 + finishUsing 应用共用，写入时固化） */
    public static List<MobEffectInstance> effectsFor(int lvl, String flavor) {
        List<MobEffectInstance> effects = new ArrayList<>();
        if (lvl >= 2) {
            effects.add(new MobEffectInstance(MobEffects.CONFUSION, SfConfig.BOTTLE_NAUSEA_SECONDS.get() * 20, 0));
        }
        if (lvl >= 3) {
            effects.add(new MobEffectInstance(MobEffects.POISON, SfConfig.DRINK_POISON_SECONDS.get() * 20, 0));
        }
        // 风味附加效果（0.1.2+）：材质特性演绎，叠在等级效果之上；无风味（plain）不加
        if (flavor != null) {
            switch (flavor) {
                case "iron" -> effects.add(new MobEffectInstance(MobEffects.WEAKNESS, 15 * 20, 0));          // 铁锈：锈到没力气
                case "gold" -> effects.add(new MobEffectInstance(MobEffects.LUCK, 60 * 20, 0));              // 金贵：金=好运
                case "diamond" -> effects.add(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 20, 0)); // 凛冽：硬=耐打
                case "netherite" -> effects.add(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 30 * 20, 0)); // 硫磺：下界火抗
                default -> { /* 未知/plain：无附加 */ }
            }
        }
        return effects;
    }

    /** 写药水+饱食度组件（tooltip 自动显示效果/时长/饥饿值；无效果的瓶不写药水组件） */
    public static void setPotionContents(ItemStack stack, int lvl, String flavor) {
        List<MobEffectInstance> effects = effectsFor(lvl, flavor);
        if (!effects.isEmpty()) {
            stack.set(DataComponents.POTION_CONTENTS,
                new PotionContents(Optional.empty(), Optional.empty(), effects));
        }
        // FOOD 组件仅驱动 tooltip 的"恢复 X 饥饿值"；饱食度应用仍在 finishUsing 手动（喝药水语义）
        stack.set(DataComponents.FOOD,
            new net.minecraft.world.food.FoodProperties(
                "leather".equals(flavor) ? 2 : 1, 0.1F, true, 1.6F, Optional.empty(), List.of()));
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    /** 必须返回 DRINK 才有原版"仰头喝"动画（动画由 getUseAnimation 决定，与物品标签/组件无关） */
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        int lvl = getLevel(stack);
        String flavor = stack.get(ModDataComponents.SWEAT_FLAVOR.get());
        if (entity instanceof Player player) {
            // 1 级：回半格饱食度；醇厚（皮革）发酵更顶饱 → 翻倍
            player.getFoodData().eat("leather".equals(flavor) ? 2 : 1, 0.1F);
        }
        if (!level.isClientSide) {
            // 有点咸...：喝汗液瓶进度（consume_item 必须显式 trigger，且要在 stack.consume 前传本体）
            if (entity instanceof net.minecraft.server.level.ServerPlayer sp) {
                net.minecraft.advancements.CriteriaTriggers.CONSUME_ITEM.trigger(sp, stack);
            }
            PotionContents pc = stack.get(DataComponents.POTION_CONTENTS);
            if (pc != null) {
                // 0.1.4+：效果在倒汗时固化进组件（与 tooltip 显示一致，配置改动只影响新瓶）
                pc.forEachEffect(entity::addEffect);
            } else {
                // fallback：旧瓶（0.1.3-）无组件，按等级老逻辑
                if (lvl >= 2) {
                    entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, SfConfig.BOTTLE_NAUSEA_SECONDS.get() * 20, 0));
                }
                if (lvl >= 3) {
                    entity.addEffect(new MobEffectInstance(MobEffects.POISON, SfConfig.DRINK_POISON_SECONDS.get() * 20, 0));
                }
                if (flavor != null) {
                    switch (flavor) {
                        case "iron" -> entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 15 * 20, 0));
                        case "gold" -> entity.addEffect(new MobEffectInstance(MobEffects.LUCK, 60 * 20, 0));
                        case "diamond" -> entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 20, 0));
                        case "netherite" -> entity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 30 * 20, 0));
                        default -> { /* 未知/plain：无附加 */ }
                    }
                }
            }
            // 音效：1 级像吃东西（回饱食度），2/3 级像喝药水
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                lvl == 1 ? SoundEvents.GENERIC_EAT : SoundEvents.GENERIC_DRINK,
                SoundSource.PLAYERS, 1.0F, 1.0F);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        stack.consume(1, entity);
        return Items.GLASS_BOTTLE.getDefaultInstance();
    }

    @Override
    public String getDescriptionId(ItemStack stack) {
        return super.getDescriptionId(stack) + "." + getLevel(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltip,
                                TooltipFlag flag) {
        // 药水效果行（0.1.4+：POTION_CONTENTS 自动显示效果+时长，与喝下去的效果一致）
        PotionContents pc = stack.get(DataComponents.POTION_CONTENTS);
        if (pc != null) {
            pc.addPotionTooltip(tooltip::add, 1.0F, 20.0F);
        }
        // 风味 lore（倒汗时按靴子材质写入组件；无组件 = 朴素瓶不显示）
        String flavor = stack.get(ModDataComponents.SWEAT_FLAVOR.get());
        if (flavor != null) {
            SweatyTooltips.addIfPresent(tooltip, "item.sweatyfeet.flavor_desc." + flavor);
        }
    }
}
