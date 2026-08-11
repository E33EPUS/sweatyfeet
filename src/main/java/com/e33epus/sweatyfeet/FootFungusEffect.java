package com.e33epus.sweatyfeet;

import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.Identifier;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;

/**
 * 真菌感染效果：汗脚 3 级后继续穿靴触发。整蛊向 = 间歇打喷嚏，
 * 效果全程持续减速（addAttributeModifier，脱鞋移除效果时自动撤销）。
 * 不播音效（村民音效难听，粒子就够）。
 */
public class FootFungusEffect extends StatusEffect {
    public FootFungusEffect(StatusEffectCategory category, int color) {
        super(category, color);
        // 持续移速 -15%（amplifier 越高减越多，StatusEffect 内部按 amplifier 缩放）
        addAttributeModifier(EntityAttributes.GENERIC_MOVEMENT_SPEED,
            Identifier.of(SweatyFeet.MOD_ID, "foot_fungus_slow"),
            -0.15D, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        // 每 tick 都跑调度（applyUpdateEffect 内部用 tickCount 分流），
        // 保证扣血在 duration % 3s == 0 的 tick 上必定执行（之前 % 20 唤醒会漏掉部分命中）
        return true;
    }

    @Override
    public boolean applyUpdateEffect(LivingEntity entity, int amplifier) {
        int t = entity.age;
        // 喷嚏粒子：每 60 tick 一次（表现）
        if (SfConfig.SNEEZE_PARTICLES
            && t % 60 == 0
            && entity.getWorld() instanceof ServerWorld serverLevel) {
            serverLevel.spawnParticles(
                ParticleTypes.SNEEZE,
                entity.getX(), entity.getY() + entity.getHeight() * 0.8, entity.getZ(),
                12, 0.3, 0.2, 0.3, 0.05);
        }
        // 缓慢扣血（无视护甲），按配置间隔，可致死。
        // DamageSources.source(key) 是 private，自己从注册表取 RegistryEntry 构造——
        // 必须是注册过的 RegistryEntry（damage_event 同步包按注册表 id 序列化，direct holder 会踢人）
        if (SfConfig.FUNGUS_DAMAGE_ENABLED
            && !entity.getWorld().isClient
            && t % (SfConfig.FUNGUS_DAMAGE_INTERVAL_SECONDS * 20) == 0) {
            net.minecraft.registry.Registry<net.minecraft.entity.damage.DamageType> reg =
                entity.getWorld().getRegistryManager().getOptional(net.minecraft.registry.RegistryKeys.DAMAGE_TYPE)
                    .orElseThrow();
            net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.damage.DamageType> type =
                reg.getEntry(ModEffects.FUNGUS_DAMAGE).orElseThrow();
            entity.damage(new net.minecraft.entity.damage.DamageSource(type), 1.0F); // 自定义死法：被脚气真菌侵蚀
        }
        return true;
    }
}
