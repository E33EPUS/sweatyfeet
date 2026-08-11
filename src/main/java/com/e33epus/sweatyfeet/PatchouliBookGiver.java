package com.e33epus.sweatyfeet;

import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * 帕秋莉手册发放（optional 前置）：装了 patchouli 时，玩家每次进世界给一本 Sweaty Feet 指南。
 * 关键：服务端 PatchouliAPI.get() 返回的是 StubPatchouliAPI（书内容客户端加载），
 * getBookStack 直接返回 ItemStack.EMPTY（javap 实锤）→ 调 API 发书永远发不出来。
 * 改成手工构造 patchouli:guide_book + patchouli:book DataComponent（服务端可构造有效物品，
 * 客户端加载书内容时读 data/assets）。
 * 懒加载守卫：先 isModLoaded("patchouli") 才引用 PatchouliDataComponents 类。
 */
public final class PatchouliBookGiver {
    private PatchouliBookGiver() {
    }

    /** 进世界调用：patchouli 装了才给书；背包/盔甲/副手已有同款手册则不再发（重复发书根因：无条件 add） */
    public static void tryGiveBook(ServerPlayerEntity player) {
        try {
            if (!FabricLoader.getInstance().isModLoaded("patchouli")) {
                return;
            }
            ItemStack book = new ItemStack(Registries.ITEM.get(
                Identifier.of("patchouli", "guide_book")));
            book.set(vazkii.patchouli.common.item.PatchouliDataComponents.BOOK,
                Identifier.of(SweatyFeet.MOD_ID, "sweaty_guide"));
            if (!alreadyHasBook(player, book)) {
                player.getInventory().offerOrDrop(book);
            }
        } catch (Throwable t) {
            // 发书失败不崩（手册是锦上添花）
        }
    }

    /** ItemStack.matches 比较 item + 全部组件（含 patchouli:book），精确匹配已发的手册 */
    private static boolean alreadyHasBook(ServerPlayerEntity player, ItemStack book) {
        PlayerInventory inv = player.getInventory();
        for (List<ItemStack> part : List.of(inv.main, inv.armor, inv.offHand)) {
            for (ItemStack s : part) {
                if (ItemStack.areEqual(s, book)) {
                    return true;
                }
            }
        }
        return false;
    }
}
