package com.e33epus.sweatyfeet;

import net.minecraft.text.Text;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

/**
 * 洗脚水桶（"xxx的洗脚水"）：空桶右键浑水盆收集，名字带玩家名（CUSTOM_NAME）。
 * 整蛊梗：喝 = 只弹"醇香"提示（纯效果名无实际 buff，文档拍板），喝完回空桶。
 */
public class WashWaterBucketItem extends Item {
    public WashWaterBucketItem(Item.Settings properties) {
        super(properties);
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    /** 必须返回 DRINK 才有原版"仰头喝"动画 */
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
        if (!level.isClient && entity instanceof PlayerEntity player) {
            // consume_item 进度触发器必须显式调用（vanilla 由消费物手动 trigger），
            // 不调"味真足！"进度永远不触发
            if (player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
                net.minecraft.advancement.criterion.Criteria.CONSUME_ITEM.trigger(serverPlayer, stack);
            }
            // 醇香：纯提示，无实际效果
            player.sendMessage(Text.translatable("sweatyfeet.msg.aroma"), true);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.ENTITY_GENERIC_DRINK, SoundCategory.PLAYERS, 1.0F, 1.0F);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.ENTITY_PLAYER_BURP, SoundCategory.PLAYERS, 1.0F, 1.0F);
        }
        stack.decrement(1);
        return new ItemStack(Items.BUCKET);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, java.util.List<Text> tooltip,
                                TooltipType flag) {
        // 简介行 + 使用方式行（分行）
        SweatyTooltips.addIfPresent(tooltip, "item.sweatyfeet.wash_water_bucket.tooltip1");
        SweatyTooltips.addIfPresent(tooltip, "item.sweatyfeet.wash_water_bucket.tooltip2");
    }
}
