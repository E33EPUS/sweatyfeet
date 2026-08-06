package com.e33epus.sweatyfeet;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
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

    /** 洗脚盆的物品形态（进创造栏） */
    public static final DeferredItem<BlockItem> WASH_BASIN_ITEM =
        ModItems.ITEMS.register("wash_basin",
            rl -> new BlockItem(WASH_BASIN.get(), new Item.Properties()));

    /** 凳子的物品形态（进创造栏） */
    public static final DeferredItem<BlockItem> STOOL_ITEM =
        ModItems.ITEMS.register("stool",
            rl -> new BlockItem(STOOL.get(), new Item.Properties()));

    private ModBlocks() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }
}
