package com.e33epus.sweatyfeet;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * 汗液瓶（分级 1/2/3，等级存 SWEAT_LEVEL 组件，仿原版药水分级模式）：
 * - 1 级：喝回半格饱食度
 * - 2 级：+ 反胃 10 秒
 * - 3 级：+ 中毒 3 秒
 * 普通右键喝（仰头动画 + 咕嘟音效），潜行右键投掷（砸中挂汗脚 5 秒）。
 * 喝后回空玻璃瓶。
 */
public class SweatBottleItem extends Item implements ProjectileItem {
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
        ItemStack stack = player.getItemInHand(hand);
        if (player.isCrouching()) {
            throwBottle(level, player, stack);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        int lvl = getLevel(stack);
        if (entity instanceof Player player) {
            // 1 级：回半格饱食度
            player.getFoodData().eat(1, 0.1F);
        }
        if (!level.isClientSide) {
            if (lvl >= 2) {
                entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, SfConfig.BOTTLE_NAUSEA_SECONDS.get() * 20, 0));
            }
            if (lvl >= 3) {
                entity.addEffect(new MobEffectInstance(MobEffects.POISON, SfConfig.DRINK_POISON_SECONDS.get() * 20, 0));
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

    private void throwBottle(Level level, Player player, ItemStack stack) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        if (!level.isClientSide) {
            SweatBottleProjectile projectile = new SweatBottleProjectile(level, player);
            projectile.setItem(stack);
            projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
            level.addFreshEntity(projectile);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        stack.consume(1, player);
    }

    @Override
    public Projectile asProjectile(Level level, Position pos, ItemStack stack, Direction direction) {
        SweatBottleProjectile projectile = new SweatBottleProjectile(level, pos.x(), pos.y(), pos.z());
        projectile.setItem(stack);
        return projectile;
    }

    /** 按等级显示物品名（一级汗液瓶/二级汗液瓶/三级汗液瓶）；Item.getName 默认转发此方法 */
    @Override
    public String getDescriptionId(ItemStack stack) {
        return super.getDescriptionId(stack) + "." + getLevel(stack);
    }
}
