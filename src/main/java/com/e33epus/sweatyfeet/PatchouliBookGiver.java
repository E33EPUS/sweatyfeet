package com.e33epus.sweatyfeet;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

import java.util.List;

/**
 * 帕秋莉手册发放（optional 前置）：装了 patchouli 时，玩家每次进世界给一本 Sweaty Feet 指南。
 * 关键：服务端 PatchouliAPI.get() 返回的是 StubPatchouliAPI（书内容客户端加载），
 * getBookStack 直接返回 ItemStack.EMPTY（javap 实锤）→ 调 API 发书永远发不出来。
 * 改成手工构造 patchouli:guide_book + patchouli:book DataComponent（服务端可构造有效物品，
 * 客户端加载书内容时读 data/assets）。
 * 懒加载守卫：先 isLoaded("patchouli") 才引用 PatchouliDataComponents 类。
 */
public final class PatchouliBookGiver {
    private PatchouliBookGiver() {
    }

    /** 进世界调用：patchouli 装了才给书；背包/盔甲/副手已有同款手册则不再发（重复发书根因：无条件 add） */
    public static void tryGiveBook(ServerPlayer player) {
        try {
            if (!ModList.get().isLoaded("patchouli")) {
                return;
            }
            ItemStack book = new ItemStack(BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath("patchouli", "guide_book")));
            book.set(vazkii.patchouli.common.item.PatchouliDataComponents.BOOK,
                ResourceLocation.fromNamespaceAndPath(SweatyFeet.MOD_ID, "sweaty_guide"));
            if (!alreadyHasBook(player, book)) {
                player.getInventory().add(book);
            }
        } catch (Throwable t) {
            // 发书失败不崩（手册是锦上添花）
        }
    }

    /** ItemStack.matches 比较 item + 全部组件（含 patchouli:book），精确匹配已发的手册 */
    private static boolean alreadyHasBook(ServerPlayer player, ItemStack book) {
        Inventory inv = player.getInventory();
        for (List<ItemStack> part : List.of(inv.items, inv.armor, inv.offhand)) {
            for (ItemStack s : part) {
                if (ItemStack.matches(s, book)) {
                    return true;
                }
            }
        }
        return false;
    }
}
