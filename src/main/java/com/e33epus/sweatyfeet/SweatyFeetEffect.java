package com.e33epus.sweatyfeet;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * 汗脚效果：穿靴子过久触发。1/2/3 级 = amplifier 0/1/2。
 * 整蛊向表现：脚边冒汗滴粒子，不造成实际伤害。
 */
public class SweatyFeetEffect extends MobEffect {
    public SweatyFeetEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (SfConfig.SWEAT_PARTICLES.get() && entity.level() instanceof ServerLevel serverLevel) {
            int count = (2 + amplifier) * SfConfig.SWEAT_PARTICLE_SCALE.get();
            serverLevel.sendParticles(
                ParticleTypes.SPLASH,
                entity.getX(), entity.getY() + 0.1, entity.getZ(),
                count, 0.15, 0.0, 0.15, 0.05);
        }
        return true;
    }
}
