package com.e33epus.sweatyfeet.mixin.client;

import com.e33epus.sweatyfeet.SoakSkinClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 坐凳上时把玩家皮肤整体替换为"下半身肤色"改图（脱裤观感，纯客户端）。
 * 必须注入 getSkinTextures() 而不是 getTexture：整合包的 skinlayers3d 渲染 3D 皮肤层
 * 时独立调 getSkinTextures().texture() 拿纹理（CustomLayerFeatureRenderer 反编译实锤），
 * 绕过了 PlayerRenderer.getTexture——只换 getTexture 对 3D 层无效
 * （"下半身毫无变化"根因）。getSkinTextures 一个入口覆盖：原版 2D 层 + skinlayers3d 3D 层 + 手部。
 * ClientPlayerEntity 不 override getSkinTextures（源码核对）→ 本地玩家自己也生效。
 * 防御：任何异常静默跳过——多个客户端共享 mods 目录时 jar 版本不一致会类缺失，不崩渲染线程。
 */
@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerMixin {
    // getSkinTextures() 无参实例方法 → 注入方法不能带宿主参数（宿主是隐式 this，带了 descriptor 不匹配会崩）；
    // 旧 PlayerRendererSoakSkinMixin 带 player 参数是因为 target 方法 getTexture(player) 本身带参
    @Inject(method = "getSkinTextures()Lnet/minecraft/client/util/SkinTextures;",
        at = @At("RETURN"), cancellable = true)
    private void sweatyfeet$soakUndressSkin(CallbackInfoReturnable<SkinTextures> cir) {
        try {
            AbstractClientPlayerEntity player = (AbstractClientPlayerEntity) (Object) this;
            SkinTextures skin = SoakSkinClient.resolve(player, cir.getReturnValue());
            if (skin != null) {
                cir.setReturnValue(skin);
            }
        } catch (Throwable t) {
            // 类缺失(NoClassDefFoundError)/静态初始化失败/任何异常：不脱衣不崩溃
        }
    }
}
