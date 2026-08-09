package com.e33epus.sweatyfeet;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
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
 * 饮品（工作台合成：汗液瓶 + 材料 = 对应 buff 饮品，类型存 DRINK_TYPE 组件）。
 * 先验证速度/力量两种，架构按类型扩展（加类型 + 加配方 + 加 lang key）。
 * 喝 = 原版药水式（仰头动画 + 咕嘟音效 + 回空玻璃瓶）。
 */
public class SweatDrinkItem extends Item {
    public SweatDrinkItem(Properties properties) {
        super(properties);
    }

    /** 读饮品类型，未知/空类型归一化到 speed（防脏数据，避免 descriptionId 生成不存在的 lang key） */
    static String readType(ItemStack stack) {
        String type = stack.get(ModDataComponents.DRINK_TYPE.get());
        return switch (type) {
            case "speed", "strength", "fermented" -> type;
            case null, default -> "speed";
        };
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(
        Level level, Player player, net.minecraft.world.InteractionHand hand) {
        player.startUsingItem(hand);
        return net.minecraft.world.InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide) {
            // 美醉了！：喝汗液饮品进度（consume_item 必须显式 trigger，且要在 stack.consume 前传本体）
            if (entity instanceof net.minecraft.server.level.ServerPlayer sp) {
                net.minecraft.advancements.CriteriaTriggers.CONSUME_ITEM.trigger(sp, stack);
            }
            int ticks = SfConfig.DRINK_BUFF_SECONDS.get() * 20;
            if ("fermented".equals(readType(stack))) {
                // 汗液饮品（发酵靴 3 级产物）：迅捷 + 跳跃提升 + 力量 + 幸运
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, 0));
                entity.addEffect(new MobEffectInstance(MobEffects.JUMP, ticks, 0)); // 1.21.1 跳跃提升字段名 JUMP
                entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ticks, 0));
                entity.addEffect(new MobEffectInstance(MobEffects.LUCK, ticks, 0));
            } else {
                Holder<MobEffect> effect = effectForType(readType(stack));
                if (effect != null) {
                    entity.addEffect(new MobEffectInstance(effect, ticks, 0));
                }
            }
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 1.0F, 1.0F);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        stack.consume(1, entity);
        return Items.GLASS_BOTTLE.getDefaultInstance();
    }

    /** 类型 → buff 效果映射（后续加配方时在这里扩展） */
    private static Holder<MobEffect> effectForType(String type) {
        return switch (type) {
            case "strength" -> MobEffects.DAMAGE_BOOST;
            case "speed" -> MobEffects.MOVEMENT_SPEED;
            default -> null;
        };
    }

    @Override
    public String getDescriptionId(ItemStack stack) {
        return super.getDescriptionId(stack) + "." + readType(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltip,
                                TooltipFlag flag) {
        // 简介行 + 使用方式行（分行）
        tooltip.add(Component.translatable("item.sweatyfeet.sweat_drink.tooltip1"));
        tooltip.add(Component.translatable("item.sweatyfeet.sweat_drink.tooltip2"));
    }
}
