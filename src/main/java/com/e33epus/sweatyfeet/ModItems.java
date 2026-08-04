package com.e33epus.sweatyfeet;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS =
        DeferredRegister.createItems(SweatyFeet.MOD_ID);

    public static final DeferredItem<SweatBottleItem> SWEAT_BOTTLE =
        ITEMS.register("sweat_bottle", rl -> new SweatBottleItem(new Item.Properties().stacksTo(16)));

    private ModItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
