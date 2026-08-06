package com.e33epus.sweatyfeet;

import javax.annotation.Nullable;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 凳子：可放置、空手右键坐上去（隐形 SeatEntity）。
 * 朝向锚定：放置时玩家面朝的正方向（N/E/S/W）即凳子固定朝向，
 * 坐下永远朝这个方向——不跟随玩家坐下时的脸（用户要求锚定正方向）。
 * 洗脚 v2：坐凳 + 脱鞋 + 右键旁边洗脚盆。
 */
public class StoolBlock extends HorizontalDirectionalBlock {
    /** 座面 8px 高，整格碰撞（坐上去脚踩凳面） */
    private static final VoxelShape SHAPE = box(0.0, 0.0, 0.0, 16.0, 8.0, 16.0);

    public static final MapCodec<StoolBlock> CODEC = simpleCodec(StoolBlock::new);

    @Override
    protected MapCodec<? extends StoolBlock> codec() {
        return CODEC;
    }

    public StoolBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** 放置朝向 = 玩家放置时面对的正方向（锚定，之后不变） */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (player.isPassenger() || player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.CONSUME;
        }
        // 座位朝向 = 方块锚定朝向，忽略玩家当前脸朝向
        if (!sitOn(level, pos, state.getValue(FACING).toYRot(), player)) {
            player.displayClientMessage(Component.translatable("sweatyfeet.msg.seat_blocked"), true);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.CONSUME;
    }

    /** 在凳面中心创建座位实体并让玩家骑上；朝向 = 方块锚定朝向（正方向） */
    public static boolean sitOn(Level level, BlockPos stoolPos, float seatYRot, Player player) {
        SeatEntity seat = ModEntities.SEAT.get().create(level);
        if (seat == null) {
            return false;
        }
        // 座位中心向凳子朝向偏 0.3 格：脚伸向盆（盆放凳子正前方 1 格）
        Direction facing = Direction.fromYRot(seatYRot);
        seat.moveTo(stoolPos.getX() + 0.5 + 0.3 * facing.getStepX(),
            stoolPos.getY() + 0.5,
            stoolPos.getZ() + 0.5 + 0.3 * facing.getStepZ(),
            seatYRot, 0.0F);
        if (!level.addFreshEntity(seat)) {
            return false;
        }
        if (!player.startRiding(seat)) {
            seat.discard();
            return false;
        }
        player.setYRot(seatYRot);
        player.yHeadRot = seatYRot;
        return true;
    }
}
