package com.e33epus.sweatyfeet.mixin;

import com.e33epus.sweatyfeet.SweatyDataHolder;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 玩家持久化汗脚数据（Fabric 无 attachment 系统 → 挂玩家 NBT）：
 * - sweatyfeet_sweatState：汗脚等级 amp（默认 -1，缺失 = 无汗脚）
 * - sweatyfeet_fungus：真菌标记（默认 false）
 * 与 NeoForge 版 AttachmentType 语义一致（默认值、序列化到玩家数据、跨会话保留）。
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin implements SweatyDataHolder {
    @Unique
    private int sweatyfeet_sweatState = -1;
    @Unique
    private boolean sweatyfeet_fungus = false;

    @Override
    public int sweatState() {
        return sweatyfeet_sweatState;
    }

    @Override
    public void setSweatState(int amp) {
        sweatyfeet_sweatState = amp;
    }

    @Override
    public boolean hasFungus() {
        return sweatyfeet_fungus;
    }

    @Override
    public void setFungus(boolean fungus) {
        sweatyfeet_fungus = fungus;
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void sweatyfeet$writeNbt(NbtCompound nbt, CallbackInfo ci) {
        nbt.putInt("sweatyfeet_sweat_state", sweatyfeet_sweatState);
        nbt.putBoolean("sweatyfeet_fungus", sweatyfeet_fungus);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void sweatyfeet$readNbt(NbtCompound nbt, CallbackInfo ci) {
        sweatyfeet_sweatState = nbt.contains("sweatyfeet_sweat_state")
            ? nbt.getInt("sweatyfeet_sweat_state") : -1;
        sweatyfeet_fungus = nbt.getBoolean("sweatyfeet_fungus");
    }
}
