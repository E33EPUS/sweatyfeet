package com.e33epus.sweatyfeet;

import java.util.List;
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
 * 稀释的花露水：药水洗脚水（MEDICINAL 盆）用空桶接出，可倒回洗脚盆变药水洗脚水。
 * 整蛊梗：稀释了 = 变淡了，喝 = 只弹"有点苦..."提示（无实际 buff），喝完回空桶。
 */
public class DilutedFloralWaterItem extends Item {
    public DilutedFloralWaterItem(Properties properties) {
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
            // 有点苦...：纯提示，无实际效果
            player.displayClientMessage(Component.translatable("sweatyfeet.msg.diluted_aroma"), true);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 1.0F, 1.0F);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        stack.consume(1, entity);
        return new ItemStack(Items.BUCKET);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        // 简介行（用户定：1 行即可，使用方式并入简介）
        SweatyTooltips.addIfPresent(tooltip, "item.sweatyfeet.diluted_floral_water.tooltip1");
    }
}
