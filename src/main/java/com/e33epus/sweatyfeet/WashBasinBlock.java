package com.e33epus.sweatyfeet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
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
 * - 空桶右键浑水盆 → 收"xxx的洗脚水"
 * - 空手右键（赤脚 + 盆有水 + 有汗脚）→ 开始/继续泡脚（累计计时，离开暂停，
 *   满 wash_seconds 洗完 → 清汗脚 + 盆变浑水）；穿鞋提示脱鞋、没汗脚提示。
 * 与 models/block/wash_basin{,_water,_dirty}.json 视觉一致：底 1px + 四壁到 5px，壁厚 2px。
 */
public class WashBasinBlock extends Block {
    private static final VoxelShape SHAPE = box(0.0, 0.0, 0.0, 16.0, 5.0, 16.0);

    public enum Filled implements net.minecraft.util.StringRepresentable {
        EMPTY, WATER, DIRTY, MEDICINAL;

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

    /** 手持物品交互：只管水桶/空桶，其余一律 FAIL（不让它落进 useWithoutItem 触发泡脚） */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        Filled filled = state.getValue(FILLED);

        // 花露水倒进含水盆：清水 → 药水洗脚水（真菌治疗两步走的第一步）
        if (stack.is(ModItems.FLORAL_WATER.get())) {
            if (filled == Filled.WATER) {
                if (!level.isClientSide) {
                    SweatyFeetHandler.debugLog(SfConfig.DEBUG_FLOW_LOG.get(), "flow",
                        player.getGameProfile().getName() + " floral water -> medicinal basin at " + pos);
                    level.setBlockAndUpdate(pos, state.setValue(FILLED, Filled.MEDICINAL));
                    stack.consume(1, player);
                    level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.CLOUD,
                            pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5,
                            8, 0.3, 0.1, 0.3, 0.05);
                    }
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
            return ItemInteractionResult.FAIL; // 盆里没清水不能倒花露水
        }

        // 水桶倒水：只能倒进空盆
        if (stack.is(Items.WATER_BUCKET)) {
            if (filled == Filled.EMPTY) {
                if (!level.isClientSide) {
                    level.setBlockAndUpdate(pos, state.setValue(FILLED, Filled.WATER));
                    player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                    level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.SPLASH,
                            pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5,
                            6, 0.3, 0.1, 0.3, 0.05);
                    }
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
            return ItemInteractionResult.FAIL; // 盆不是空的，不给倒
        }

        // 空桶舀水：有水→空桶变水桶；浑水→收成"xxx的洗脚水"，盆变空；药水洗脚水→收"稀释的花露水"
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
                if (!level.isClientSide) {
                    level.setBlockAndUpdate(pos, state.setValue(FILLED, Filled.EMPTY));
                    ItemStack dirtyWater = new ItemStack(ModItems.WASH_WATER_BUCKET.get());
                    dirtyWater.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                        Component.translatable("item.sweatyfeet.wash_water_bucket.owned", player.getName()));
                    player.setItemInHand(hand, dirtyWater);
                    level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
            if (filled == Filled.MEDICINAL) {
                // 药水洗脚水也能用桶接走（之前落 FAIL = 接不起来的根因），收成"稀释的花露水"
                if (!level.isClientSide) {
                    level.setBlockAndUpdate(pos, state.setValue(FILLED, Filled.EMPTY));
                    player.setItemInHand(hand, new ItemStack(ModItems.DILUTED_FLORAL_WATER.get()));
                    level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
            return ItemInteractionResult.FAIL;
        }

        // 稀释的花露水倒进空盆：变回药水洗脚水（跟水桶倒水同逻辑）
        if (stack.is(ModItems.DILUTED_FLORAL_WATER.get())) {
            if (filled == Filled.EMPTY) {
                if (!level.isClientSide) {
                    level.setBlockAndUpdate(pos, state.setValue(FILLED, Filled.MEDICINAL));
                    player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                    level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
            return ItemInteractionResult.FAIL; // 盆不是空的，不给倒
        }

        // 空手/其他物品 → 交给 useWithoutItem（泡脚交互）。
        // 之前这里 return FAIL：vanilla 只在 PASS_TO_DEFAULT 时才调 useWithoutItem，
        // 导致空手右键盆永远没反应、泡脚/计时/浑水/收集整条链全死（实测实锤）
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** 空手右键：泡脚交互 */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        // useItemOn 对非桶/非花露水一律 PASS 到这里（useWithoutItem 只在主手被调）。
        // 只拦主手非空（避免挥剑误泡脚）；副手有物品不拦——vanilla 右键永远先主手，
        // 主手空时 useWithoutItem 必被调用，拦副手会把整个泡脚交互 PASS 掉（实测失效根因）
        if (!player.getMainHandItem().isEmpty()) {
            return InteractionResult.PASS;
        }
        Filled filled = state.getValue(FILLED);
        if (filled == Filled.DIRTY) {
            // 浑水是泡完的产物，不能泡——想泡先舀掉换新水
            if (!level.isClientSide) {
                SweatyFeetHandler.debugLog(SfConfig.DEBUG_FLOW_LOG.get(), "flow",
                    player.getGameProfile().getName() + " basin blocked: dirty");
                player.displayClientMessage(Component.translatable("sweatyfeet.msg.basin_dirty"), true);
            }
            return InteractionResult.CONSUME;
        }
        if (filled != Filled.WATER && filled != Filled.MEDICINAL) {
            if (!level.isClientSide) {
                SweatyFeetHandler.debugLog(SfConfig.DEBUG_FLOW_LOG.get(), "flow",
                    player.getGameProfile().getName() + " basin blocked: empty");
                player.displayClientMessage(Component.translatable("sweatyfeet.msg.basin_empty"), true);
            }
            return InteractionResult.CONSUME;
        }
        // 洗脚机制（v2）：必须坐在凳子上才能洗——站着右键盆只提示
        if (!(player.getVehicle() instanceof SeatEntity)) {
            if (!level.isClientSide) {
                SweatyFeetHandler.debugLog(SfConfig.DEBUG_FLOW_LOG.get(), "flow",
                    player.getGameProfile().getName() + " basin blocked: not seated");
                player.displayClientMessage(Component.translatable("sweatyfeet.msg.sit_to_soak"), true);
            }
            return InteractionResult.CONSUME;
        }
        if (player.getItemBySlot(EquipmentSlot.FEET).is(net.minecraft.tags.ItemTags.FOOT_ARMOR)) {
            if (!level.isClientSide) {
                SweatyFeetHandler.debugLog(SfConfig.DEBUG_FLOW_LOG.get(), "flow",
                    player.getGameProfile().getName() + " basin blocked: boots on");
                player.displayClientMessage(Component.translatable("sweatyfeet.msg.take_off"), true);
            }
            return InteractionResult.CONSUME;
        }
        boolean hasSweat = player.hasEffect(ModEffects.SWEATY_FEET);
        boolean hasFungus = player.hasEffect(ModEffects.FOOT_FUNGUS);
        // 药水洗脚水：治真菌（没汗脚也能泡）；清水：洗汗脚。两者都没有 → 提示
        if (!hasSweat && !hasFungus) {
            if (!level.isClientSide) {
                SweatyFeetHandler.debugLog(SfConfig.DEBUG_FLOW_LOG.get(), "flow",
                    player.getGameProfile().getName() + " basin blocked: no sweat/fungus");
                player.displayClientMessage(Component.translatable("sweatyfeet.msg.no_sweat"), true);
            }
            return InteractionResult.CONSUME;
        }
        // 坐凳上 + 赤脚 + 盆有水/药水 + 有汗脚或真菌 → 开始/继续泡脚（累计计时不清零）
        if (!level.isClientSide) {
            SweatyFeetHandler.debugLog(SfConfig.DEBUG_FLOW_LOG.get(), "flow",
                player.getGameProfile().getName() + " basin soak start: "
                + (filled == Filled.MEDICINAL ? "medicinal" : "water"));
            SweatyFeetHandler.startBasinSoak(player, pos);
            player.displayClientMessage(Component.translatable(
                filled == Filled.MEDICINAL ? "sweatyfeet.msg.soak_medicinal" : "sweatyfeet.msg.soak_start"), true);
            // 开泡的倒计时首句立刻刷（不等 20 tick），之后由 tickBasinSoak 每秒刷
            SweatyFeetHandler.showBasinSoakHud(player);
        }
        return InteractionResult.CONSUME;
    }
}
