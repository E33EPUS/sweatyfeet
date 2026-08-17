package com.e33epus.sweatyfeet;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public final class ModBlocks {
    /** 洗脚盆：站上去泡脚清汗脚（半格高木盆） */
    public static final Block WASH_BASIN = Registry.register(Registries.BLOCK,
        Identifier.of(SweatyFeet.MOD_ID, "wash_basin"),
        new WashBasinBlock(AbstractBlock.Settings.create()
            .strength(0.6F)
            .sounds(BlockSoundGroup.WOOD)
            .nonOpaque()));

    /** 凳子：可放置可坐，坐凳上右键旁边洗脚盆洗脚 */
    public static final Block STOOL = Registry.register(Registries.BLOCK,
        Identifier.of(SweatyFeet.MOD_ID, "stool"),
        new StoolBlock(AbstractBlock.Settings.create()
            .strength(0.6F)
            .sounds(BlockSoundGroup.WOOD)
            .nonOpaque()));

    /** 洗脚盆的物品形态（进创造栏，带悬浮描述：简介 1 行） */
    public static final Item WASH_BASIN_ITEM = Registry.register(Registries.ITEM,
        Identifier.of(SweatyFeet.MOD_ID, "wash_basin"),
        new TooltipBlockItem(WASH_BASIN, new Item.Settings(),
            "item.sweatyfeet.wash_basin.tooltip1"));

    /** 凳子的物品形态（进创造栏，带悬浮描述：简介 1 行） */
    public static final Item STOOL_ITEM = Registry.register(Registries.ITEM,
        Identifier.of(SweatyFeet.MOD_ID, "stool"),
        new TooltipBlockItem(STOOL, new Item.Settings(),
            "item.sweatyfeet.stool.tooltip1"));

    /** 触发类加载完成注册（fabric 静态注册模式） */
    public static void init() {
    }

    private ModBlocks() {
    }
}
