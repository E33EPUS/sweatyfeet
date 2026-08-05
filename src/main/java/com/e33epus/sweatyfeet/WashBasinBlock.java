package com.e33epus.sweatyfeet;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 洗脚盆：半格高的木盆（底 + 四壁，中空）。三态：空 / 有水 / 浑水。
 * - 水桶右键空盆 → 倒水（消耗水桶给空桶），盆变"有水"
 * - 空桶右键有水盆 → 舀水，盆变空
 * - 空桶右键浑水盆 → 收"xxx的洗脚水"（SweatRepellentItem 逻辑，见 3d）
 * 泡脚（赤脚右键 + 有水）→ 计时清汗脚，洗完水变浑（Handler 管）。
 * 与 models/block/wash_basin{,_water,_dirty}.json 视觉一致：底 1px + 四壁到 5px，壁厚 2px。
 */
public class WashBasinBlock extends Block {
    private static final VoxelShape SHAPE = box(0.0, 0.0, 0.0, 16.0, 5.0, 16.0);

    public enum Filled implements net.minecraft.util.StringRepresentable {
        EMPTY, WATER, DIRTY;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public static final EnumProperty<Filled> FILLED = EnumProperty.create("filled", Filled.class);

    public WashBasinBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FILLED, Filled.EMPTY));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FILLED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        Filled filled = state.getValue(FILLED);

        // 水桶倒水：只能倒进空盆
        if (stack.is(Items.WATER_BUCKET)) {
            if (filled == Filled.EMPTY) {
                if (!level.isClientSide) {
                    level.setBlockAndUpdate(pos, state.setValue(FILLED, Filled.WATER));
                    player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                    level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SPLASH,
                            pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5,
                            6, 0.3, 0.1, 0.3, 0.05);
                    }
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION; // 盆不是空的不给倒
        }

        // 空桶舀水：有水→空桶变水桶；浑水→空桶变洗脚水桶（3d 实现）
        if (stack.is(Items.BUCKET)) {
            if (filled == Filled.WATER) {
                if (!level.isClientSide) {
                    level.setBlockAndUpdate(pos, state.setValue(FILLED, Filled.EMPTY));
                    player.setItemInHand(hand, new ItemStack(Items.WATER_BUCKET));
                    level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
            if (filled == Filled.DIRTY) {
                // 3d：收集为"xxx的洗脚水"
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}
