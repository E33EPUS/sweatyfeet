package com.e33epus.sweatyfeet;

import net.minecraft.util.math.BlockPos;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ItemActionResult;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

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
    private static final VoxelShape SHAPE = VoxelShapes.cuboid(0.0, 0.0, 0.0, 16.0, 5.0, 16.0);

    public enum Filled implements net.minecraft.util.StringIdentifiable {
        EMPTY, WATER, DIRTY, MEDICINAL;

        @Override
        public String asString() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public static final EnumProperty<Filled> FILLED = EnumProperty.of("filled", Filled.class);

    public WashBasinBlock(AbstractBlock.Settings properties) {
        super(properties);
        this.setDefaultState(this.stateManager.getDefaultState().with(FILLED, Filled.EMPTY));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FILLED);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView level, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    /** 手持物品交互：只管水桶/空桶，其余一律 FAIL（不让它落进 useWithoutItem 触发泡脚） */
    @Override
    protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World level, BlockPos pos,
                                              PlayerEntity player, Hand hand, BlockHitResult hit) {
        Filled filled = state.get(FILLED);

        // 花露水倒进含水盆：清水 → 药水洗脚水（真菌治疗两步走的第一步）
        if (stack.isOf(ModItems.FLORAL_WATER)) {
            if (filled == Filled.WATER) {
                if (!level.isClient) {
                    SweatyFeetHandler.debugLog(SfConfig.DEBUG_FLOW_LOG, "flow",
                        player.getGameProfile().getName() + " floral water -> medicinal basin at " + pos);
                    level.setBlockState(pos, state.with(FILLED, Filled.MEDICINAL));
                    stack.decrement(1);
                    level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        SoundEvents.ITEM_BOTTLE_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);
                    if (level instanceof ServerWorld serverLevel) {
                        serverLevel.spawnParticles(ParticleTypes.CLOUD,
                            pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5,
                            8, 0.3, 0.1, 0.3, 0.05);
                    }
                }
                return ItemActionResult.success(level.isClient);
            }
            return ItemActionResult.FAIL; // 盆里没清水不能倒花露水
        }

        // 水桶倒水：只能倒进空盆
        if (stack.isOf(Items.WATER_BUCKET)) {
            if (filled == Filled.EMPTY) {
                if (!level.isClient) {
                    level.setBlockState(pos, state.with(FILLED, Filled.WATER));
                    player.setStackInHand(hand, new ItemStack(Items.BUCKET));
                    level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        SoundEvents.ITEM_BUCKET_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);
                    if (level instanceof ServerWorld serverLevel) {
                        serverLevel.spawnParticles(ParticleTypes.SPLASH,
                            pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5,
                            6, 0.3, 0.1, 0.3, 0.05);
                    }
                }
                return ItemActionResult.success(level.isClient);
            }
            return ItemActionResult.FAIL; // 盆不是空的，不给倒
        }

        // 空桶舀水：有水→空桶变水桶；浑水→收成"xxx的洗脚水"，盆变空；药水洗脚水→收"稀释的花露水"
        if (stack.isOf(Items.BUCKET)) {
            if (filled == Filled.WATER) {
                if (!level.isClient) {
                    level.setBlockState(pos, state.with(FILLED, Filled.EMPTY));
                    player.setStackInHand(hand, new ItemStack(Items.WATER_BUCKET));
                    level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        SoundEvents.ITEM_BUCKET_FILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
                }
                return ItemActionResult.success(level.isClient);
            }
            if (filled == Filled.DIRTY) {
                if (!level.isClient) {
                    level.setBlockState(pos, state.with(FILLED, Filled.EMPTY));
                    ItemStack dirtyWater = new ItemStack(ModItems.SWEAT_DRINK);
                    dirtyWater.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME,
                        Text.translatable("item.sweatyfeet.wash_water_bucket.owned", player.getName()));
                    player.setStackInHand(hand, dirtyWater);
                    level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        SoundEvents.ITEM_BUCKET_FILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
                }
                return ItemActionResult.success(level.isClient);
            }
            if (filled == Filled.MEDICINAL) {
                // 药水洗脚水也能用桶接走（之前落 FAIL = 接不起来的根因），收成"稀释的花露水"
                if (!level.isClient) {
                    level.setBlockState(pos, state.with(FILLED, Filled.EMPTY));
                    player.setStackInHand(hand, new ItemStack(ModItems.SWEAT_BOTTLE));
                    level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        SoundEvents.ITEM_BUCKET_FILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
                }
                return ItemActionResult.success(level.isClient);
            }
            return ItemActionResult.FAIL;
        }

        // 稀释的花露水倒进空盆：变回药水洗脚水（跟水桶倒水同逻辑）
        if (stack.isOf(ModItems.DILUTED_FLORAL_WATER)) {
            if (filled == Filled.EMPTY) {
                if (!level.isClient) {
                    level.setBlockState(pos, state.with(FILLED, Filled.MEDICINAL));
                    player.setStackInHand(hand, new ItemStack(Items.BUCKET));
                    level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        SoundEvents.ITEM_BUCKET_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);
                }
                return ItemActionResult.success(level.isClient);
            }
            return ItemActionResult.FAIL; // 盆不是空的，不给倒
        }

        // 空手/其他物品 → 交给 useWithoutItem（泡脚交互）。
        // 之前这里 return FAIL：vanilla 只在 PASS_TO_DEFAULT 时才调 useWithoutItem，
        // 导致空手右键盆永远没反应、泡脚/计时/浑水/收集整条链全死（实测实锤）
        return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** 空手右键：泡脚交互 */
    @Override
    protected ActionResult onUse(BlockState state, World level, BlockPos pos,
                                               PlayerEntity player, BlockHitResult hit) {
        // useItemOn 对非桶/非花露水一律 PASS 到这里（useWithoutItem 只在主手被调）。
        // 只拦主手非空（避免挥剑误泡脚）；副手有物品不拦——vanilla 右键永远先主手，
        // 主手空时 useWithoutItem 必被调用，拦副手会把整个泡脚交互 PASS 掉（实测失效根因）
        if (!player.getMainHandStack().isEmpty()) {
            return ActionResult.PASS;
        }
        Filled filled = state.get(FILLED);
        if (filled == Filled.DIRTY) {
            // 浑水是泡完的产物，不能泡——想泡先舀掉换新水
            if (!level.isClient) {
                SweatyFeetHandler.debugLog(SfConfig.DEBUG_FLOW_LOG, "flow",
                    player.getGameProfile().getName() + " basin blocked: dirty");
                player.sendMessage(Text.translatable("sweatyfeet.msg.basin_dirty"), true);
            }
            return ActionResult.CONSUME;
        }
        if (filled != Filled.WATER && filled != Filled.MEDICINAL) {
            if (!level.isClient) {
                SweatyFeetHandler.debugLog(SfConfig.DEBUG_FLOW_LOG, "flow",
                    player.getGameProfile().getName() + " basin blocked: empty");
                player.sendMessage(Text.translatable("sweatyfeet.msg.basin_empty"), true);
            }
            return ActionResult.CONSUME;
        }
        // 洗脚机制（v2）：必须坐在凳子上才能洗——站着右键盆只提示
        if (!(player.getVehicle() instanceof SeatEntity)) {
            if (!level.isClient) {
                SweatyFeetHandler.debugLog(SfConfig.DEBUG_FLOW_LOG, "flow",
                    player.getGameProfile().getName() + " basin blocked: not seated");
                player.sendMessage(Text.translatable("sweatyfeet.msg.sit_to_soak"), true);
            }
            return ActionResult.CONSUME;
        }
        if (player.getEquippedStack(EquipmentSlot.FEET).isIn(net.minecraft.registry.tag.ItemTags.FOOT_ARMOR)) {
            if (!level.isClient) {
                SweatyFeetHandler.debugLog(SfConfig.DEBUG_FLOW_LOG, "flow",
                    player.getGameProfile().getName() + " basin blocked: boots on");
                player.sendMessage(Text.translatable("sweatyfeet.msg.take_off"), true);
            }
            return ActionResult.CONSUME;
        }
        boolean hasSweat = player.hasStatusEffect(ModEffects.SWEATY_FEET);
        boolean hasFungus = player.hasStatusEffect(ModEffects.FOOT_FUNGUS);
        // 药水洗脚水：治真菌（没汗脚也能泡）；清水：洗汗脚。两者都没有 → 提示
        if (!hasSweat && !hasFungus) {
            if (!level.isClient) {
                SweatyFeetHandler.debugLog(SfConfig.DEBUG_FLOW_LOG, "flow",
                    player.getGameProfile().getName() + " basin blocked: no sweat/fungus");
                player.sendMessage(Text.translatable("sweatyfeet.msg.no_sweat"), true);
            }
            return ActionResult.CONSUME;
        }
        // 坐凳上 + 赤脚 + 盆有水/药水 + 有汗脚或真菌 → 开始/继续泡脚（累计计时不清零）
        if (!level.isClient) {
            SweatyFeetHandler.debugLog(SfConfig.DEBUG_FLOW_LOG, "flow",
                player.getGameProfile().getName() + " basin soak start: "
                + (filled == Filled.MEDICINAL ? "medicinal" : "water"));
            SweatyFeetHandler.startBasinSoak(player, pos);
            player.sendMessage(Text.translatable(
                filled == Filled.MEDICINAL ? "sweatyfeet.msg.soak_medicinal" : "sweatyfeet.msg.soak_start"), true);
            // 开泡的倒计时首句立刻刷（不等 20 tick），之后由 tickBasinSoak 每秒刷
            SweatyFeetHandler.showBasinSoakHud(player);
        }
        return ActionResult.CONSUME;
    }
}
