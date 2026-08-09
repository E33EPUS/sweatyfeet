package com.e33epus.sweatyfeet.mixin.client;

import com.e33epus.sweatyfeet.SeatEntity;
import com.e33epus.sweatyfeet.SoakPose;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;

/**
 * 坐凳泡脚姿势（去 PA 的 quark 式轻量实现）：
 * 注入 LivingEntityRenderer.render 里 model.renderToBuffer 调用之前——此时 setupAnim
 * 已完成、下一帧会重置，改 ModelPart 叠加到最终姿势，无状态无泄漏。
 * 只用旋转（xRot/yRot/zRot），第二层（pants/sleeve/jacket）在 setupAnim 的 copyFrom
 * 之后被覆盖，需手动再 copyFrom 同步（照抄 Quark ModelAccessor.messWithPlayerModel）。
 */
@Mixin(LivingEntityRenderer.class)
public abstract class PlayerSoakPoseMixin<T extends LivingEntity, M extends EntityModel<T>> {

    @Shadow
    @Final
    protected M model;

    @Inject(
        // method 只用方法名不用完整描述符：1.21.1 运行时混入 LivingEntityRenderer 带泛型签名
        // 的 render 时，全描述符匹配失效（InvalidInjectionException，启动崩溃）——与
        // ItemStackSweatHintMixin 的 getTooltipLines 同款教训。LivingEntityRenderer 只有一个 render。
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/model/Model;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"
        )
    )
    private void sf$applySoakPose(T entity, float entityYaw, float partialTick,
                                  PoseStack poseStack, MultiBufferSource bufferSource,
                                  int packedLight, CallbackInfo ci) {
        try {
            if (!(entity instanceof Player player)) {
                return;
            }
            if (!(player.getVehicle() instanceof SeatEntity)) {
                return;
            }
            if (!player.getItemBySlot(EquipmentSlot.FEET).isEmpty()) {
                return;
            }
            if (!(this.model instanceof PlayerModel<?> pm)) {
                return;
            }
            SoakPose.Parts pose = SoakPose.at(entity.tickCount, partialTick);
            pm.rightLeg.xRot = pose.rightLegXRot;
            pm.leftLeg.xRot = pose.leftLegXRot;
            pm.body.xRot = pose.bodyXRot;
            pm.rightArm.xRot = pose.rightArmXRot;
            pm.leftArm.xRot = pose.leftArmXRot;
            pm.rightArm.zRot = pose.rightArmZRot;
            pm.leftArm.zRot = pose.leftArmZRot;
            // 第二层跟随主部位（PlayerModel.setupAnim 的 copyFrom 在注入点之前，需重同步）
            pm.rightPants.copyFrom(pm.rightLeg);
            pm.leftPants.copyFrom(pm.leftLeg);
            pm.jacket.copyFrom(pm.body);
            pm.rightSleeve.copyFrom(pm.rightArm);
            pm.leftSleeve.copyFrom(pm.leftArm);
        } catch (Throwable ignored) {
            // 渲染热路径：失败静默跳过，不崩渲染
        }
    }
}
