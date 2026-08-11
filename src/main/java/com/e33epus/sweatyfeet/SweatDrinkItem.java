package com.e33epus.sweatyfeet;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import net.minecraft.entity.effect.StatusEffect;
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
 * 饮品（发酵靴 3 级倒汗产物；类型存 DRINK_TYPE 组件，架构按类型扩展）。
 * 喝 = 原版药水式（仰头动画 + 咕嘟音效 + 回空玻璃瓶）。
 */
public class SweatDrinkItem extends Item {
    public SweatDrinkItem(Item.Settings properties) {
        super(properties);
    }

    /** 读饮品类型，未知/空类型归一化到 speed（防脏数据，避免 descriptionId 生成不存在的 lang key） */
    static String readType(ItemStack stack) {
        String type = stack.get(ModDataComponents.DRINK_TYPE);
        return switch (type) {
            case "speed", "strength", "fermented" -> type;
            case null, default -> "speed";
        };
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.DRINK;
    }

    @Override
    public net.minecraft.util.TypedActionResult<ItemStack> use(
        World level, PlayerEntity player, net.minecraft.util.Hand hand) {
        player.setCurrentHand(hand);
        return net.minecraft.util.TypedActionResult.consume(player.getStackInHand(hand));
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World level, LivingEntity entity) {
        if (!level.isClient) {
            // 美醉了！：喝汗液饮品进度（consume_item 必须显式 trigger，且要在 stack.consume 前传本体）
            if (entity instanceof net.minecraft.server.network.ServerPlayerEntity sp) {
                net.minecraft.advancement.criterion.Criteria.CONSUME_ITEM.trigger(sp, stack);
            }
            int ticks = SfConfig.DRINK_BUFF_SECONDS * 20;
            if ("fermented".equals(readType(stack))) {
                // 汗液饮品（发酵靴 3 级产物）：迅捷 + 跳跃提升 + 力量 + 幸运
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, ticks, 0));
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, ticks, 0)); // 1.21.1 跳跃提升字段名 JUMP
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, ticks, 0));
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, ticks, 0));
            } else {
                RegistryEntry<StatusEffect> effect = effectForType(readType(stack));
                if (effect != null) {
                    entity.addStatusEffect(new StatusEffectInstance(effect, ticks, 0));
                }
            }
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.ENTITY_GENERIC_DRINK, SoundCategory.PLAYERS, 1.0F, 1.0F);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.ENTITY_PLAYER_BURP, SoundCategory.PLAYERS, 1.0F, 1.0F);
        }
        stack.decrement(1);
        return Items.GLASS_BOTTLE.getDefaultStack();
    }

    /** 类型 → buff 效果映射（后续加配方时在这里扩展） */
    private static RegistryEntry<StatusEffect> effectForType(String type) {
        return switch (type) {
            case "strength" -> StatusEffects.STRENGTH;
            case "speed" -> StatusEffects.SPEED;
            default -> null;
        };
    }

    @Override
    public String getTranslationKey(ItemStack stack) {
        return super.getTranslationKey(stack) + "." + readType(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, java.util.List<Text> tooltip,
                                TooltipType flag) {
        // 简介行 + 使用方式行（分行）
        SweatyTooltips.addIfPresent(tooltip, "item.sweatyfeet.sweat_drink.tooltip1");
        SweatyTooltips.addIfPresent(tooltip, "item.sweatyfeet.sweat_drink.tooltip2");
    }
}
