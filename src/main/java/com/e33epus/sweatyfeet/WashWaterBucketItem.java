package com.e33epus.sweatyfeet;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * 洗脚水桶（"xxx的洗脚水"）：空桶右键浑水盆收集，名字带玩家名（CUSTOM_NAME）。
 * 整蛊梗：喝 = 只弹"醇香"提示（纯效果名无实际 buff，文档拍板），喝完回空桶。
 */
public class WashWaterBucketItem extends Item {
    public WashWaterBucketItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    /** 必须返回 DRINK 才有原版"仰头喝"动画 */
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
        if (!level.isClientSide && entity instanceof Player player) {
            // consume_item 进度触发器必须显式调用（vanilla 由消费物手动 trigger），
            // 不调"味真足！"进度永远不触发
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                net.minecraft.advancements.CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);
            }
            // 醇香：纯提示，无实际效果
            player.displayClientMessage(Component.translatable("sweatyfeet.msg.aroma"), true);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 1.0F, 1.0F);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        stack.consume(1, entity);
        return new ItemStack(Items.BUCKET);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltip,
                                TooltipFlag flag) {
        // 简介行 + 使用方式行（分行）
        tooltip.add(Component.translatable("item.sweatyfeet.wash_water_bucket.tooltip1"));
        tooltip.add(Component.translatable("item.sweatyfeet.wash_water_bucket.tooltip2"));
    }
}
