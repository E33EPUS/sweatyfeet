package com.e33epus.sweatyfeet;

import java.util.List;
import java.util.Map;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class ModItems {
    /** 汗液瓶：穿汗靴倒汗产物，等级/风味放组件，喝 = 按等级+风味叠效果 */
    public static final Item SWEAT_BOTTLE = Registry.register(Registries.ITEM,
        Identifier.of(SweatyFeet.MOD_ID, "sweat_bottle"),
        new SweatBottleItem(new Item.Settings().maxCount(16)));

    /** 饮品：发酵靴 3 级倒汗产物，类型存 DRINK_TYPE 组件 */
    public static final Item SWEAT_DRINK = Registry.register(Registries.ITEM,
        Identifier.of(SweatyFeet.MOD_ID, "sweat_drink"),
        new SweatDrinkItem(new Item.Settings().maxCount(16)));

    /** 花露水：真菌治疗"两步走"第一步——倒进清水盆变药水洗脚水（水瓶 + 任意两种小花合成） */
    public static final Item FLORAL_WATER = Registry.register(Registries.ITEM,
        Identifier.of(SweatyFeet.MOD_ID, "floral_water"),
        new SweatRepellentItem(new Item.Settings().maxCount(16)));

    /** 洗脚水桶：空桶右键浑水盆收集，喝 = 只弹"醇香"提示（整蛊，喝完回空桶） */
    public static final Item WASH_WATER_BUCKET = Registry.register(Registries.ITEM,
        Identifier.of(SweatyFeet.MOD_ID, "wash_water_bucket"),
        new WashWaterBucketItem(new Item.Settings().maxCount(1)));

    /** 稀释的花露水：空桶右键药水洗脚水盆收集，可倒回盆变药水洗脚水，喝 = "有点苦..."（整蛊） */
    public static final Item DILUTED_FLORAL_WATER = Registry.register(Registries.ITEM,
        Identifier.of(SweatyFeet.MOD_ID, "diluted_floral_water"),
        new DilutedFloralWaterItem(new Item.Settings().maxCount(1)));

    /** 发酵靴专用盔甲材质：护甲 1（同皮革）、附魔 15（同皮革）、皮革音效/修复材料；
     *  纹理走 mod 自带 fermented_boots_layer_1.png（浅棕成品靴，不靠染色） */
    public static final RegistryEntry<ArmorMaterial> FERMENTED_MATERIAL = Registry.registerReference(
        Registries.ARMOR_MATERIAL,
        Identifier.of(SweatyFeet.MOD_ID, "fermented"),
        new ArmorMaterial(
            Map.of(ArmorItem.Type.BOOTS, 1),
            15,
            SoundEvents.ITEM_ARMOR_EQUIP_LEATHER,
            () -> Ingredient.ofItems(Items.LEATHER),
            List.of(new ArmorMaterial.Layer(Identifier.of(SweatyFeet.MOD_ID, "fermented_boots"))),
            0.0F, 0.0F));

    /** 发酵靴：皮革靴+糖合成，穿它汗脚发酵——3 级倒汗产出"汗液饮品"（正面 buff）。
     *  自定义 ArmorMaterial：原版 LEATHER 材质 + 不在 dyeable tag → 渲染时用白色上色
     *  灰白线稿 = 看起来像铁靴（用户实测）。自定义材质 dyeable=false + 自绘浅棕纹理。 */
    public static final Item FERMENTED_BOOTS = Registry.register(Registries.ITEM,
        Identifier.of(SweatyFeet.MOD_ID, "fermented_boots"),
        new FermentedBootsItem(FERMENTED_MATERIAL, ArmorItem.Type.BOOTS, new Item.Settings()));

    public static final ItemGroup TAB = Registry.register(Registries.ITEM_GROUP,
        Identifier.of(SweatyFeet.MOD_ID, "tab"),
        ItemGroup.create(ItemGroup.Row.TOP, 8)
            .displayName(Text.translatable("itemGroup.sweatyfeet"))
            .icon(() -> new ItemStack(SWEAT_BOTTLE))
            .entries((params, output) -> {
                // 汗液瓶 1/2/3 级（带等级组件）
                for (int lvl = 1; lvl <= 3; lvl++) {
                    ItemStack bottle = new ItemStack(SWEAT_BOTTLE);
                    bottle.set(ModDataComponents.SWEAT_LEVEL, lvl);
                    output.add(bottle);
                }
                // 风味汗液瓶：5 种靴子材质风味 × 3 级全组合（用户：等级与风味叠加，每种都要能拿到）
                for (String flavor : new String[]{"leather", "iron", "gold", "diamond", "netherite"}) {
                    for (int lvl = 1; lvl <= 3; lvl++) {
                        ItemStack flavored = new ItemStack(SWEAT_BOTTLE);
                        flavored.set(ModDataComponents.SWEAT_LEVEL, lvl);
                        flavored.set(ModDataComponents.SWEAT_FLAVOR, flavor);
                        output.add(flavored);
                    }
                }
                // 饮品（发酵靴 3 级倒汗产物：汗液饮品，正面 buff）
                ItemStack fermented = new ItemStack(SWEAT_DRINK);
                fermented.set(ModDataComponents.DRINK_TYPE, "fermented");
                output.add(fermented);
                // 发酵靴
                output.add(new ItemStack(FERMENTED_BOOTS));
                // 花露水
                output.add(new ItemStack(FLORAL_WATER));
                // 稀释的花露水
                output.add(new ItemStack(DILUTED_FLORAL_WATER));
                // 洗脚水桶（示例无名字）
                output.add(new ItemStack(WASH_WATER_BUCKET));
                // 洗脚盆
                output.add(new ItemStack(ModBlocks.WASH_BASIN_ITEM));
                // 凳子
                output.add(new ItemStack(ModBlocks.STOOL_ITEM));
            })
            .build());

    /** 触发类加载完成注册（fabric 静态注册模式） */
    public static void init() {
    }

    private ModItems() {
    }
}
