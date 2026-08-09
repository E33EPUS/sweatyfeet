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
            // 调用点 owner 必须是 EntityModel 不是 Model：javap 运行时字节码实锤
            // invokevirtual EntityModel.renderToBuffer(PoseStack;VertexConsumer;III)V——
            // LivingEntityRenderer 的 model 字段声明为 EntityModel<T>，常量池引用类型就是它，
            // target 写 Model 会 0/1 匹配失败（InjectionError，第二次启动崩溃）
            value = "INVOKE",
            target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"
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
            // 关键：必须连 yRot/zRot 一起清——vanilla 骑乘姿势（setupAnim 的 riding 分支）会给
            // 双腿设 ±18° yRot 外张 + zRot 微旋，只覆盖 xRot 会残留成"外八字伸腿"（用户截图实锤）
            pm.rightLeg.xRot = pose.rightLegXRot; pm.rightLeg.yRot = 0.0F; pm.rightLeg.zRot = 0.0F;
            pm.leftLeg.xRot = pose.leftLegXRot;   pm.leftLeg.yRot = 0.0F; pm.leftLeg.zRot = 0.0F;
            pm.body.xRot = pose.bodyXRot;         pm.body.yRot = 0.0F;    pm.body.zRot = 0.0F;
            pm.rightArm.xRot = pose.rightArmXRot; pm.rightArm.yRot = 0.0F; pm.rightArm.zRot = pose.rightArmZRot;
            pm.leftArm.xRot = pose.leftArmXRot;   pm.leftArm.yRot = 0.0F;  pm.leftArm.zRot = pose.leftArmZRot;
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
