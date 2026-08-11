package com.e33epus.sweatyfeet;

import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.LivingEntity;

/**
 * 汗脚效果：穿靴子过久触发。1/2/3 级 = amplifier 0/1/2。
 * 整蛊向表现：脚边冒汗滴粒子，不造成实际伤害。
 */
public class SweatyFeetEffect extends StatusEffect {
    public SweatyFeetEffect(StatusEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return duration % 20 == 0;
    }

    @Override
    public boolean applyUpdateEffect(LivingEntity entity, int amplifier) {
        if (SfConfig.SWEAT_PARTICLES && entity.getWorld() instanceof ServerWorld serverLevel) {
            int count = (2 + amplifier) * SfConfig.SWEAT_PARTICLE_SCALE;
            serverLevel.spawnParticles(
                ParticleTypes.SPLASH,
                entity.getX(), entity.getY() + 0.1, entity.getZ(),
                count, 0.15, 0.0, 0.15, 0.05);
        }
        // 3 级（amp 2）额外冒绿色臭味粒子（整蛊表现）
        if (amplifier >= 2 && entity.getWorld() instanceof ServerWorld serverLevel) {
            serverLevel.spawnParticles(
                ParticleTypes.COMPOSTER,
                entity.getX(), entity.getY() + 0.1, entity.getZ(),
                2, 0.3, 0.0, 0.3, 0.0);
        }
        return true;
    }
}
