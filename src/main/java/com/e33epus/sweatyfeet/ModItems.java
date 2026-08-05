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

    /** 花露水：真菌唯一快捷解药，右键喷在身上清真菌（水瓶+铃兰+萤石粉合成） */
    public static final DeferredItem<SweatRepellentItem> FLORAL_WATER =
        ITEMS.register("floral_water", rl -> new SweatRepellentItem(new Item.Properties().stacksTo(16)));

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SweatyFeet.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB =
        CREATIVE_TABS.register("tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.sweatyfeet"))
            .icon(() -> new ItemStack(SWEAT_BOTTLE.get()))
            .displayItems((params, output) -> {
                // 汗液瓶 1/2/3 级（带等级组件）
                for (int lvl = 1; lvl <= 3; lvl++) {
                    ItemStack bottle = new ItemStack(SWEAT_BOTTLE.get());
                    bottle.set(ModDataComponents.SWEAT_LEVEL.get(), lvl);
                    output.accept(bottle);
                }
                // 饮品（速度/力量，带类型组件）
                ItemStack speed = new ItemStack(SWEAT_DRINK.get());
                speed.set(ModDataComponents.DRINK_TYPE.get(), "speed");
                output.accept(speed);
                ItemStack strength = new ItemStack(SWEAT_DRINK.get());
                strength.set(ModDataComponents.DRINK_TYPE.get(), "strength");
                output.accept(strength);
                // 花露水
                output.accept(new ItemStack(FLORAL_WATER.get()));
                // 洗脚盆
                output.accept(new ItemStack(ModBlocks.WASH_BASIN_ITEM.get()));
            })
            .build());

    private ModItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        CREATIVE_TABS.register(modBus);
    }
}
