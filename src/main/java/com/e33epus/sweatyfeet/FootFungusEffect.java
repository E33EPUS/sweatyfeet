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
        return duration % 60 == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (SfConfig.INSTANCE.sneeze_particles && entity.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                ParticleTypes.SNEEZE,
                entity.getX(), entity.getY() + entity.getBbHeight() * 0.8, entity.getZ(),
                12, 0.3, 0.2, 0.3, 0.05);
        }
        return true;
    }
}
