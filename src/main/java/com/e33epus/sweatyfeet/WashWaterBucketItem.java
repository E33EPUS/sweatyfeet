package com.e33epus.sweatyfeet;

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
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * 洗脚水桶（"xxx的洗脚水"）：空桶右键浑水盆收集，名字带玩家名（CUSTOM_NAME）。
 * 整蛊向饮品：喝 = 反胃 + 扣血 + 概率感染真菌（脚气传染，幽默闭环）。
 * 喝完回空桶。
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
        if (!level.isClientSide) {
            // 恶心效果：反胃 + 魔法伤害（无视护甲）+ 50% 概率感染真菌（洗脚水传染脚气）
            entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, SfConfig.BOTTLE_NAUSEA_SECONDS.get() * 20, 1));
            entity.hurt(entity.damageSources().magic(), 2.0F);
            if (level.getRandom().nextFloat() < 0.5F) {
                entity.addEffect(new MobEffectInstance(ModEffects.FOOT_FUNGUS, MobEffectInstance.INFINITE_DURATION, 0, false, true));
            }
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 1.0F, 1.0F);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        stack.consume(1, entity);
        return new ItemStack(Items.BUCKET);
    }
}
