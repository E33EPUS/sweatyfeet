package com.e33epus.sweatyfeet;

import java.util.List;
import net.minecraft.text.Text;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

/**
 * 稀释的花露水：药水洗脚水（MEDICINAL 盆）用空桶接出，可倒回洗脚盆变药水洗脚水。
 * 整蛊梗：稀释了 = 变淡了，喝 = 只弹"有点苦..."提示（无实际 buff），喝完回空桶。
 */
public class DilutedFloralWaterItem extends Item {
    public DilutedFloralWaterItem(Item.Settings properties) {
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
            // 有点苦...：纯提示，无实际效果
            player.sendMessage(Text.translatable("sweatyfeet.msg.diluted_aroma"), true);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.ENTITY_GENERIC_DRINK, SoundCategory.PLAYERS, 1.0F, 1.0F);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.ENTITY_PLAYER_BURP, SoundCategory.PLAYERS, 1.0F, 1.0F);
        }
        stack.decrement(1);
        return new ItemStack(Items.BUCKET);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip,
                                TooltipType flag) {
        // 简介行（用户定：1 行即可，使用方式并入简介）
        SweatyTooltips.addIfPresent(tooltip, "item.sweatyfeet.diluted_floral_water.tooltip1");
    }
}
