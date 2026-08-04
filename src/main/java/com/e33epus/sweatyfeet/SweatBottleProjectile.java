package com.e33epus.sweatyfeet;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * 汗液瓶投掷物：照抄雪球模式，命中目标挂 5 秒汗脚 1 级（整蛊害人）。
 */
public class SweatBottleProjectile extends ThrowableItemProjectile {
    public SweatBottleProjectile(EntityType<? extends SweatBottleProjectile> type, Level level) {
        super(type, level);
    }

    public SweatBottleProjectile(Level level, LivingEntity shooter) {
        super(ModEntities.SWEAT_BOTTLE.get(), shooter, level);
    }

    public SweatBottleProjectile(Level level, double x, double y, double z) {
        super(ModEntities.SWEAT_BOTTLE.get(), x, y, z, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.SWEAT_BOTTLE.get();
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (!this.level().isClientSide && hit.getEntity() instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(ModEffects.SWEATY_FEET, SfConfig.INSTANCE.throw_debuff_seconds * 20, 0));
        }
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (!this.level().isClientSide) {
            this.level().broadcastEntityEvent(this, (byte) 3);
            this.discard();
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 3) {
            for (int i = 0; i < 8; i++) {
                this.level().addParticle(ParticleTypes.SPLASH, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            }
        }
    }
}
