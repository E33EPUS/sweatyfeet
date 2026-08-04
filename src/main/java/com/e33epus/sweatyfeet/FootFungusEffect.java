package com.e33epus.sweatyfeet;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * 真菌感染效果：汗脚 3 级后继续穿靴触发。整蛊向 = 间歇打喷嚏，
 * 喷嚏时撒粒子 + 短暂强减速停顿，不致命，脱鞋即愈。
 */
public class FootFungusEffect extends MobEffect {
    public FootFungusEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 60 == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level() instanceof ServerLevel serverLevel) {
            if (SfConfig.INSTANCE.sneeze_particles) {
                serverLevel.sendParticles(
                    ParticleTypes.SNEEZE,
                    entity.getX(), entity.getY() + entity.getBbHeight() * 0.8, entity.getZ(),
                    12, 0.3, 0.2, 0.3, 0.05);
            }
            if (SfConfig.INSTANCE.sneeze_sound) {
                serverLevel.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    SoundEvents.VILLAGER_HURT, SoundSource.PLAYERS, 0.6F, 0.8F + serverLevel.random.nextFloat() * 0.4F);
            }
        }
        // 喷嚏停顿：强减速半秒，模拟打喷嚏时身体顿住
        entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 5, false, false));
        return true;
    }
}
