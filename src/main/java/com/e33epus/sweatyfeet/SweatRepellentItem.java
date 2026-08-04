package com.e33epus.sweatyfeet;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 花露水（Floral Water）：真菌的唯一快捷解药。
 * 配方：水瓶 + 铃兰 + 萤石粉（工作台）。
 * 用法：右键喷在身上——喷雾粒子 + 水花音效，清真菌。
 * 呼应现实梗：花露水其实治不了脚气（只有清凉止痒），游戏里却能治真菌 = 整蛊反差。
 * 泡水洗不掉真菌（用户明确：真菌只能靠花露水），但泡水能洗汗脚（见 Handler）。
 */
public class SweatRepellentItem extends Item {
    public SweatRepellentItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            // 喷在身上：清真菌（不影响汗脚）+ 喷雾粒子 + 水花音效 + 消耗 1
            player.removeEffect(ModEffects.FOOT_FUNGUS);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                    ParticleTypes.CLOUD,
                    player.getX(), player.getY() + player.getBbHeight() * 0.7, player.getZ(),
                    10, 0.3, 0.2, 0.3, 0.05);
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.0F, 1.0F);
            stack.consume(1, player);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
