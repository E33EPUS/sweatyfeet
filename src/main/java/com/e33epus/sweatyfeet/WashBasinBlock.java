package com.e33epus.sweatyfeet;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 洗脚盆：半格高的木盆（底 + 四框，中空）。
 * 玩家站上去 = 脚在盆里 → Handler tick 检测到站盆上 → 泡脚计时清汗脚（复用泡水洗脚逻辑）。
 */
public class WashBasinBlock extends Block {
    /** 与 models/block/wash_basin.json 视觉一致：底 1px + 四壁到 5px，壁厚 2px */
    private static final VoxelShape SHAPE = box(0.0, 0.0, 0.0, 16.0, 5.0, 16.0);

    public WashBasinBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
