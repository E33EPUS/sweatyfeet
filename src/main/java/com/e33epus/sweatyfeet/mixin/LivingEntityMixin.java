package com.e33epus.sweatyfeet.mixin;

import com.e33epus.sweatyfeet.ModEffects;
import com.e33epus.sweatyfeet.SfConfig;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 脚滑：Mixin 替换 LivingEntity.travel 里的方块摩擦（vanilla 1.21.1 是
 * Block.getSlipperiness() 无参调用，NeoForge patch 后才是三参版）。
 * 摩擦同时决定移动加速度和衰减系数（普通 0.6，冰面 0.989）——
 * 汗脚 2/3 级时改成配置的保留值（默认 0.85），玩家松键后衰减慢 = 真滑行，
 * 和原版冰面同源、双端一致（travel 双端都跑）。
 * 之前的 PlayerTickEvent.Post 乘动量方案无效：摩擦衰减在 travel 内先发生，Post 太晚且被下一 tick 输入重置。
 * @Redirect handler 追加调用方参数（LivingEntity）拿宿主。
 */
@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Redirect(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;getSlipperiness()F"))
    private float sweatyfeet$slideFriction(Block block, LivingEntity entity) {
        if (entity instanceof PlayerEntity player) {
            StatusEffectInstance sf = player.getStatusEffect(ModEffects.SWEATY_FEET);
            if (sf != null && sf.getAmplifier() >= 1 && SfConfig.SLIDE_ENABLED) {
                return SfConfig.SLIDE_RETENTION_PERCENT / 100.0F;
            }
        }
        return block.getSlipperiness();
    }
}
