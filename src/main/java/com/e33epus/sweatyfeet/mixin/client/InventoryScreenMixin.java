package com.e33epus.sweatyfeet.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 背包界面右上角加配置按钮：NeoForge 侧 Mods 列表入口之外，背包里也能直接打开配置界面。
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractContainerScreen<InventoryMenu> {
    protected InventoryScreenMixin(InventoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void sfAddConfigButton(CallbackInfo ci) {
        Button button = Button.builder(Component.literal("⚙"),
            b -> Minecraft.getInstance().setScreen(new com.e33epus.sweatyfeet.SfConfigScreen(this)))
            .bounds(this.leftPos + this.imageWidth - 22, this.topPos - 22, 20, 20)
            .build();
        this.addRenderableWidget(button);
    }
}
