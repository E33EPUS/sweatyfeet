package com.e33epus.sweatyfeet;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * 真菌感染效果：汗脚 3 级后继续穿靴触发。整蛊向 = 间歇打喷嚏，
 * 效果全程持续减速（addAttributeModifier，脱鞋移除效果时自动撤销）。
 * 不播音效（村民音效难听，粒子就够）。
 */
public class FootFungusEffect extends MobEffect {
    public FootFungusEffect(MobEffectCategory category, int color) {
        super(category, color);
        // 持续移速 -15%（amplifier 越高减越多，MobEffect 内部按 amplifier 缩放）
        addAttributeModifier(Attributes.MOVEMENT_SPEED,
            ResourceLocation.fromNamespaceAndPath(SweatyFeet.MOD_ID, "foot_fungus_slow"),
            -0.15D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        // 每 tick 都跑调度（applyEffectTick 内部用 tickCount 分流），
        // 保证扣血在 duration % 3s == 0 的 tick 上必定执行（之前 % 20 唤醒会漏掉部分命中）
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        int t = entity.tickCount;
        // 喷嚏粒子：每 60 tick 一次（表现）
        if (SfConfig.SNEEZE_PARTICLES.get()
            && t % 60 == 0
            && entity.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                ParticleTypes.SNEEZE,
                entity.getX(), entity.getY() + entity.getBbHeight() * 0.8, entity.getZ(),
                12, 0.3, 0.2, 0.3, 0.05);
        }
        // 缓慢扣血：magic 伤害（无视护甲），按配置间隔，可致死
        if (SfConfig.FUNGUS_DAMAGE_ENABLED.get()
            && !entity.level().isClientSide
            && t % (SfConfig.FUNGUS_DAMAGE_INTERVAL_SECONDS.get() * 20) == 0) {
            entity.hurt(entity.damageSources().magic(), 1.0F);
        }
        return true;
    }
}
