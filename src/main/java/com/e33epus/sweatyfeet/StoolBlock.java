package com.e33epus.sweatyfeet;

import org.jetbrains.annotations.Nullable;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateManager;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

/**
 * 凳子：可放置、空手右键坐上去（隐形 SeatEntity）。
 * 朝向锚定：放置时玩家面朝的正方向（N/E/S/W）即凳子固定朝向，
 * 坐下永远朝这个方向——不跟随玩家坐下时的脸（用户要求锚定正方向）。
 * 洗脚 v2：坐凳 + 脱鞋 + 右键旁边洗脚盆。
 */
public class StoolBlock extends HorizontalFacingBlock {
    /** 座面 8px 高，整格碰撞（坐上去脚踩凳面） */
    private static final VoxelShape SHAPE = VoxelShapes.cuboid(0.0, 0.0, 0.0, 16.0, 8.0, 16.0);

    public static final MapCodec<StoolBlock> CODEC = createCodec(StoolBlock::new);

    @Override
    protected MapCodec<? extends StoolBlock> getCodec() {
        return CODEC;
    }

    public StoolBlock(AbstractBlock.Settings properties) {
        super(properties);
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** 放置朝向 = 玩家放置时面对的正方向（锚定，之后不变） */
    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        return this.getDefaultState().with(FACING, context.getHorizontalPlayerFacing());
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView level, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    protected ActionResult onUse(BlockState state, World level, BlockPos pos,
                                               PlayerEntity player, BlockHitResult hit) {
        if (player.hasVehicle() || player.isSneaking()) {
            return ActionResult.PASS;
        }
        if (level.isClient) {
            return ActionResult.CONSUME;
        }
        // 座位朝向 = 方块锚定朝向，忽略玩家当前脸朝向
        if (!sitOn(level, pos, state.get(FACING).asRotation(), player)) {
            player.sendMessage(Text.translatable("sweatyfeet.msg.seat_blocked"), true);
            return ActionResult.CONSUME;
        }
        return ActionResult.CONSUME;
    }

    /** 在凳面中心创建座位实体并让玩家骑上；朝向 = 方块锚定朝向（正方向） */
    public static boolean sitOn(World level, BlockPos stoolPos, float seatYRot, PlayerEntity player) {
        SeatEntity seat = ModEntities.SEAT.create(level);
        if (seat == null) {
            return false;
        }
        // 座位中心向凳子朝向偏 0.3 格：脚伸向盆（盆放凳子正前方 1 格）
        Direction facing = Direction.fromRotation(seatYRot);
        seat.refreshPositionAndAngles(stoolPos.getX() + 0.5 + 0.3 * facing.getOffsetX(),
            stoolPos.getY() + 0.5,
            stoolPos.getZ() + 0.5 + 0.3 * facing.getOffsetZ(),
            seatYRot, 0.0F);
        if (!level.spawnEntity(seat)) {
            return false;
        }
        if (!player.startRiding(seat)) {
            seat.discard();
            return false;
        }
        player.setYaw(seatYRot);
        player.headYaw = seatYRot;
        return true;
    }
}
