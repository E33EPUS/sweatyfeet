package com.e33epus.sweatyfeet.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 背包界面右上角加配置按钮：不依赖 ModMenu 也能打开配置界面（J 键在整合包常被其他 mod 占用）。
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractInventoryScreen<PlayerScreenHandler> {
    protected InventoryScreenMixin(PlayerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void sfAddConfigButton(CallbackInfo ci) {
        // 背包 GUI 右上角外侧（配方书按钮上方区域）
        ButtonWidget button = ButtonWidget.builder(Text.literal("⚙"),
            btn -> MinecraftClient.getInstance().setScreen(new com.e33epus.sweatyfeet.SfConfigScreen(this)))
            .dimensions(this.x + this.backgroundWidth - 22, this.y - 22, 20, 20)
            .build();
        this.addDrawableChild(button);
    }
}
