package com.e33epus.sweatyfeet;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.Map;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS =
        DeferredRegister.createItems(SweatyFeet.MOD_ID);

    public static final DeferredItem<SweatBottleItem> SWEAT_BOTTLE =
        ITEMS.register("sweat_bottle", rl -> new SweatBottleItem(new Item.Properties().stacksTo(16)));

    /** 饮品：发酵靴 3 级倒汗产物，类型存 DRINK_TYPE 组件 */
    public static final DeferredItem<SweatDrinkItem> SWEAT_DRINK =
        ITEMS.register("sweat_drink", rl -> new SweatDrinkItem(new Item.Properties().stacksTo(16)));

    /** 花露水：真菌治疗"两步走"第一步——倒进清水盆变药水洗脚水（水瓶 + 任意两种小花合成） */
    public static final DeferredItem<SweatRepellentItem> FLORAL_WATER =
        ITEMS.register("floral_water", rl -> new SweatRepellentItem(new Item.Properties().stacksTo(16)));

    /** 洗脚水桶：空桶右键浑水盆收集，喝 = 只弹"醇香"提示（整蛊，喝完回空桶） */
    public static final DeferredItem<WashWaterBucketItem> WASH_WATER_BUCKET =
        ITEMS.register("wash_water_bucket", rl -> new WashWaterBucketItem(new Item.Properties().stacksTo(1)));

    /** 稀释的花露水：空桶右键药水洗脚水盆收集，可倒回盆变药水洗脚水，喝 = "有点苦..."（整蛊） */
    public static final DeferredItem<DilutedFloralWaterItem> DILUTED_FLORAL_WATER =
        ITEMS.register("diluted_floral_water", rl -> new DilutedFloralWaterItem(new Item.Properties().stacksTo(1)));

    /** 发酵靴专用盔甲材质：护甲 1（同皮革）、附魔 15（同皮革）、皮革音效/修复材料；
     *  纹理走 mod 自带 fermented_boots_layer_1.png（浅棕成品靴，不靠染色） */
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
        DeferredRegister.create(Registries.ARMOR_MATERIAL, SweatyFeet.MOD_ID);

    public static final Holder<ArmorMaterial> FERMENTED_MATERIAL =
        ARMOR_MATERIALS.register("fermented", () -> new ArmorMaterial(
            Map.of(ArmorItem.Type.BOOTS, 1),
            15,
            net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_LEATHER,
            () -> net.minecraft.world.item.crafting.Ingredient.of(Items.LEATHER),
            List.of(new ArmorMaterial.Layer(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(SweatyFeet.MOD_ID, "fermented_boots"))),
            0.0F, 0.0F));

    /** 发酵靴：皮革靴+糖合成，穿它汗脚发酵——3 级倒汗产出"汗液饮品"（正面 buff）。
     *  自定义 ArmorMaterial：原版 LEATHER 材质 + 不在 dyeable tag → 渲染时用白色上色
     *  灰白线稿 = 看起来像铁靴（用户实测）。自定义材质 dyeable=false + 自绘浅棕纹理。 */
    public static final DeferredItem<FermentedBootsItem> FERMENTED_BOOTS =
        ITEMS.register("fermented_boots", rl -> new FermentedBootsItem(
            FERMENTED_MATERIAL,
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
                // 风味汗液瓶：5 种靴子材质风味 × 3 级全组合（用户：等级与风味叠加，每种都要能拿到）
                for (String flavor : new String[]{"leather", "iron", "gold", "diamond", "netherite"}) {
                    for (int lvl = 1; lvl <= 3; lvl++) {
                        ItemStack flavored = new ItemStack(SWEAT_BOTTLE.get());
                        flavored.set(ModDataComponents.SWEAT_LEVEL.get(), lvl);
                        flavored.set(ModDataComponents.SWEAT_FLAVOR.get(), flavor);
                        output.accept(flavored);
                    }
                }
                // 饮品（发酵靴 3 级倒汗产物：汗液饮品，正面 buff）
                ItemStack fermented = new ItemStack(SWEAT_DRINK.get());
                fermented.set(ModDataComponents.DRINK_TYPE.get(), "fermented");
                output.accept(fermented);
                // 发酵靴
                output.accept(new ItemStack(FERMENTED_BOOTS.get()));
                // 花露水
                output.accept(new ItemStack(FLORAL_WATER.get()));
                // 稀释的花露水
                output.accept(new ItemStack(DILUTED_FLORAL_WATER.get()));
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
        ARMOR_MATERIALS.register(modBus);
    }
}
