package com.e33epus.sweatyfeet;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS =
        DeferredRegister.createItems(SweatyFeet.MOD_ID);

    public static final DeferredItem<SweatBottleItem> SWEAT_BOTTLE =
        ITEMS.register("sweat_bottle", rl -> new SweatBottleItem(new Item.Properties().stacksTo(16)));

    /** 饮品：工作台合成汗液瓶+材料产出，类型存 DRINK_TYPE 组件 */
    public static final DeferredItem<SweatDrinkItem> SWEAT_DRINK =
        ITEMS.register("sweat_drink", rl -> new SweatDrinkItem(new Item.Properties().stacksTo(16)));

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SweatyFeet.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB =
        CREATIVE_TABS.register("tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.sweatyfeet"))
            .icon(() -> new ItemStack(SWEAT_BOTTLE.get()))
            .displayItems((params, output) -> output.accept(new ItemStack(SWEAT_BOTTLE.get())))
            .build());

    private ModItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        CREATIVE_TABS.register(modBus);
    }
}
