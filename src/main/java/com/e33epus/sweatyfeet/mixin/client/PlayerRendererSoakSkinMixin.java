package com.e33epus.sweatyfeet.mixin.client;

import com.e33epus.sweatyfeet.SoakSkinClient;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 坐凳上时把玩家皮肤换成"下半身肤色"改图（1.21.1 旧式 getTextureLocation，一个注入点全覆盖）。
 *  防御：SoakSkinClient 类加载/调用任何异常都不崩渲染线程——多个客户端共享 mods 目录时
 *  jar 版本不一致会导致类缺失（ClassNotFoundException 实测崩过），静默跳过即可。 */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererSoakSkinMixin {
    @Inject(method = "getTextureLocation(Lnet/minecraft/client/player/AbstractClientPlayer;)Lnet/minecraft/resources/ResourceLocation;",
        at = @At("RETURN"), cancellable = true)
    private void sweatyfeet$soakUndressSkin(AbstractClientPlayer player, CallbackInfoReturnable<ResourceLocation> cir) {
        try {
            ResourceLocation rl = SoakSkinClient.resolve(player, cir.getReturnValue());
            if (rl != null) {
                cir.setReturnValue(rl);
            }
        } catch (Throwable t) {
            // 类缺失(NoClassDefFoundError)/静态初始化失败/任何异常：不脱衣不崩溃
        }
    }
}
