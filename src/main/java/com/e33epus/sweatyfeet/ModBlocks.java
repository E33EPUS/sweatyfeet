package com.e33epus.sweatyfeet;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
        DeferredRegister.createBlocks(SweatyFeet.MOD_ID);

    /** 洗脚盆：站上去泡脚清汗脚（半格高木盆） */
    public static final DeferredBlock<WashBasinBlock> WASH_BASIN =
        BLOCKS.register("wash_basin", () -> new WashBasinBlock(
            BlockBehaviour.Properties.of()
                .strength(0.6F)
                .sound(SoundType.WOOD)
                .noOcclusion()));

    /** 凳子：可放置可坐，坐凳上右键旁边洗脚盆洗脚 */
    public static final DeferredBlock<StoolBlock> STOOL =
        BLOCKS.register("stool", () -> new StoolBlock(
            BlockBehaviour.Properties.of()
                .strength(0.6F)
                .sound(SoundType.WOOD)
                .noOcclusion()));

    /** 洗脚盆的物品形态（进创造栏，带悬浮描述：简介+使用+条件 3 行） */
    public static final DeferredItem<TooltipBlockItem> WASH_BASIN_ITEM =
        ModItems.ITEMS.register("wash_basin",
            rl -> new TooltipBlockItem(WASH_BASIN.get(), new Item.Properties(),
                "item.sweatyfeet.wash_basin.tooltip1", "item.sweatyfeet.wash_basin.tooltip2",
                "item.sweatyfeet.wash_basin.tooltip3"));

    /** 凳子的物品形态（进创造栏，带悬浮描述：简介+使用 2 行） */
    public static final DeferredItem<TooltipBlockItem> STOOL_ITEM =
        ModItems.ITEMS.register("stool",
            rl -> new TooltipBlockItem(STOOL.get(), new Item.Properties(),
                "item.sweatyfeet.stool.tooltip1", "item.sweatyfeet.stool.tooltip2"));

    private ModBlocks() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }
}
