package com.e33epus.sweatyfeet;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * 隐形座位实体：玩家骑上它 = 坐在凳子上。无渲染（ModRenderers 注册 no-op 渲染器防崩）、
 * 无重力、无碰撞。无乘客自动清掉；凳子被拆也清掉（防悬空坐在隐形座位上）。
 */
public class SeatEntity extends Entity {
    public SeatEntity(EntityType<? extends SeatEntity> type, World world) {
        super(type, world);
        this.noClip = true;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound tag) {
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound tag) {
    }

    /** 起身位置：朝向后 1.2 格；不安全就兜底凳面正上方 */
    @Override
    public Vec3d updatePassengerForDismount(LivingEntity passenger) {
        Direction facing = Direction.fromRotation(getYaw());
        BlockPos behind = BlockPos.ofFloored(
            getX() - facing.getOffsetX() * 1.2, getY(), getZ() - facing.getOffsetZ() * 1.2);
        Vec3d safe = net.minecraft.entity.Dismounting.findRespawnPos(
            passenger.getType(), getWorld(), behind, false);
        if (safe != null) {
            return safe;
        }
        return new Vec3d(getX(), getY() + 0.4, getZ());
    }

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClient) {
            return;
        }
        // 无乘客 = 没人坐 → 清掉（起身后残留座位在这里自我清理）
        if (getPassengerList().isEmpty()) {
            discard();
            return;
        }
        // 凳子被拆 → 座位强制清掉（乘客自动下来，防悬空坐在隐形座位上）
        if (!getWorld().getBlockState(getBlockPos()).isOf(ModBlocks.STOOL)) {
            discard();
        }
    }

    /** 坐凳旋转 = 照抄 vanilla 骑马（Boat.clampRotation，反编译实锤）：
     *  身体钉死凳朝向；视角相对凳朝向 clamp ±90°（头共 180°，用户预期）；头跟随视角。
     *  关键：每 tick 不动任何旋转，只在玩家转身（onPassengerLookAround）时 clamp，
     *  并修正 prevYaw 插值基准 → 视角被挡住但流畅无拉锯。
     *  之前每 tick 硬写 headYaw 是错的：第一人称相机读 headYaw（LivingEntity.getHeadYaw
     *  字节码实锤），每 tick 改 = 相机被拽 = 抢鼠标。 */
    @Override
    public void onPassengerLookAround(Entity passenger) {
        if (!(passenger instanceof LivingEntity living)) {
            return;
        }
        living.setBodyYaw(getYaw());
        float delta = MathHelper.wrapDegrees(living.getYaw() - getYaw());
        float clamped = MathHelper.clamp(delta, -90.0F, 90.0F); // ±90° = 头共 180°
        living.prevYaw += clamped - delta; // 修正插值基准：clamp 不产生视觉跳变
        living.setYaw(living.getYaw() + clamped - delta);
        living.setHeadYaw(living.getYaw());
    }

    @Override
    protected void updatePassengerPosition(Entity passenger, PositionUpdater positionUpdater) {
        super.updatePassengerPosition(passenger, positionUpdater);
    }

    /** 防止玩家对座位右键（避免 vanilla 二次骑乘提示/交互） */
    @Override
    public boolean canHit() {
        return false;
    }

    /** 座位与座位/其他实体不做碰撞推挤 */
    @Override
    public boolean collidesWith(Entity other) {
        return false;
    }
}
