package com.e33epus.sweatyfeet;

import net.minecraft.core.Direction;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 隐形座位实体：玩家骑上它 = 坐在凳子上。无渲染（ModRenderers 注册 no-op 渲染器防崩）、
 * 无重力、无碰撞。无乘客自动清掉；凳子被拆也清掉（防悬空坐在隐形座位上）。
 */
public class SeatEntity extends Entity {
    public SeatEntity(EntityType<? extends SeatEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
    }

    /** 起身位置：朝向后 1.2 格；不安全就兜底凳面正上方 */
    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        Direction facing = Direction.fromYRot(getYRot());
        net.minecraft.core.BlockPos behind = net.minecraft.core.BlockPos.containing(
            getX() - facing.getStepX() * 1.2, getY(), getZ() - facing.getStepZ() * 1.2);
        Vec3 safe = net.minecraft.world.entity.vehicle.DismountHelper.findSafeDismountLocation(
            passenger.getType(), this.level(), behind, false);
        if (safe != null) {
            return safe;
        }
        return new Vec3(getX(), getY() + 0.4, getZ());
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        // 无乘客 = 没人坐 → 清掉（起身后残留座位在这里自我清理）
        if (this.getPassengers().isEmpty()) {
            this.discard();
            return;
        }
        // 凳子被拆 → 座位强制清掉（乘客自动下来，防悬空坐在隐形座位上）
        if (!level().getBlockState(blockPosition()).is(ModBlocks.STOOL.get())) {
            this.discard();
        }
    }

    /** 坐凳旋转 = 照抄 vanilla 骑马（Boat.clampRotation，反编译实锤）：
     *  身体钉死凳朝向；视角相对凳朝向 clamp ±90°（头共 180°，用户预期）；头跟随视角。
     *  关键：每 tick 不动任何旋转，只在玩家转身（Entity.turn → onPassengerTurned）时 clamp，
     *  并修正 yRotO 插值基准 → 视角被挡住但流畅无拉锯。
     *  之前每 tick 硬写 yHeadRot 是错的：第一人称相机读 yHeadRot（LivingEntity.getViewYRot
     *  字节码实锤），每 tick 改 = 相机被拽 = 抢鼠标。 */
    @Override
    public void onPassengerTurned(Entity passenger) {
        if (!(passenger instanceof LivingEntity living)) {
            return;
        }
        living.setYBodyRot(this.getYRot());
        float delta = net.minecraft.util.Mth.wrapDegrees(living.getYRot() - this.getYRot());
        float clamped = net.minecraft.util.Mth.clamp(delta, -90.0F, 90.0F); // ±90° = 头共 180°
        living.yRotO += clamped - delta; // 修正插值基准：clamp 不产生视觉跳变
        living.setYRot(living.getYRot() + clamped - delta);
        living.setYHeadRot(living.getYRot());
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction moveFunction) {
        super.positionRider(passenger, moveFunction);
    }

    /** 防止玩家对座位右键（避免 vanilla 二次骑乘提示/交互） */
    @Override
    public boolean isPickable() {
        return false;
    }

    /** 座位与座位/其他实体不做碰撞推挤 */
    @Override
    public boolean canCollideWith(Entity other) {
        return false;
    }
}
