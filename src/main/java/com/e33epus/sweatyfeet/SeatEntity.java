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

    /** 身体/腿锁死朝凳朝向；头可转但限 ±90°（共 180°）。
     *  不能钉 yRot：第一人称视角 = player.yRot，钉死 = 视角被吸住转不了（实测）。
     *  1.21.1 身体+腿根渲染用 yBodyRot（LivingEntityRenderer），头用 yHeadRot——
     *  所以 yRot 自由 + yBodyRot 钉 + yHeadRot clamp 三者组合正确。 */
    @Override
    protected void positionRider(Entity passenger, MoveFunction moveFunction) {
        super.positionRider(passenger, moveFunction);
        if (passenger instanceof LivingEntity living) {
            living.yBodyRot = this.getYRot();
            float target = this.getYRot();
            float delta = Math.floorMod((long) (living.yHeadRot - target), 360L);
            if (delta > 180) {
                delta -= 360;
            }
            delta = Math.max(-90, Math.min(90, delta)); // 头最左/最右各 90°
            living.yHeadRot = target + delta;
        }
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
