package com.e33epus.sweatyfeet;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
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

    /** 洗脚水桶：空桶右键浑水盆收集，喝 = 反胃+扣血+概率真菌（整蛊） */
    public static final DeferredItem<WashWaterBucketItem> WASH_WATER_BUCKET =
        ITEMS.register("wash_water_bucket", rl -> new WashWaterBucketItem(new Item.Properties().stacksTo(1)));

    /** 发酵靴：皮革靴+糖合成，穿它汗脚发酵——3 级倒汗产出"汗液饮品"（正面 buff） */
    public static final DeferredItem<ArmorItem> FERMENTED_BOOTS =
        ITEMS.register("fermented_boots", rl -> new ArmorItem(
            net.minecraft.world.item.ArmorMaterials.LEATHER,
            ArmorItem.Type.BOOTS,
            new Item.Properties()));

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
                // 饮品（发酵靴 3 级倒汗产物：汗液饮品，正面 buff）
                ItemStack fermented = new ItemStack(SWEAT_DRINK.get());
                fermented.set(ModDataComponents.DRINK_TYPE.get(), "fermented");
                output.accept(fermented);
                // 发酵靴
                output.accept(new ItemStack(FERMENTED_BOOTS.get()));
                // 花露水
                output.accept(new ItemStack(FLORAL_WATER.get()));
                // 洗脚水桶（示例无名字）
                output.accept(new ItemStack(WASH_WATER_BUCKET.get()));
                // 洗脚盆
                output.accept(new ItemStack(ModBlocks.WASH_BASIN_ITEM.get()));
                // 凳子
                output.accept(new ItemStack(ModBlocks.STOOL_ITEM.get()));
            })
            .build());

    private ModItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        CREATIVE_TABS.register(modBus);
    }
}
