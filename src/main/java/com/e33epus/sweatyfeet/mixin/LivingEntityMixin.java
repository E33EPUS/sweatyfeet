package com.e33epus.sweatyfeet.mixin;

import com.e33epus.sweatyfeet.ModEffects;
import com.e33epus.sweatyfeet.SfConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 脚滑：Mixin 替换 LivingEntity.travel 里的方块摩擦（NeoForge patch 后是
 * BlockState.getFriction(LevelReader, BlockPos, Entity) 三参版本）。
 * 摩擦同时决定移动加速度和衰减系数（普通 0.6，冰面 0.989）——
 * 汗脚 2/3 级时改成配置的保留值（默认 0.98），玩家松键后衰减慢 = 真滑行，
 * 和原版冰面同源、双端一致（travel 双端都跑）。
 * 之前的 PlayerTickEvent.Post 乘动量方案无效：摩擦衰减在 travel 内先发生，Post 太晚且被下一 tick 输入重置。
 */
@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Redirect(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getFriction(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;)F"))
    private float sweatyfeet$slideFriction(BlockState state, LevelReader level, BlockPos pos, Entity entity) {
        if (entity instanceof Player player) {
            MobEffectInstance sf = player.getEffect(ModEffects.SWEATY_FEET);
            if (sf != null && sf.getAmplifier() >= 1 && SfConfig.SLIDE_ENABLED.get()) {
                return SfConfig.SLIDE_RETENTION_PERCENT.get() / 100.0F;
            }
        }
        return state.getFriction(level, pos, entity);
    }
}
