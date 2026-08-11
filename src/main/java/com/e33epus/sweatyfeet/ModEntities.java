package com.e33epus.sweatyfeet;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModEntities {
    /** 隐形座位：maxTrackingRange 拉满保证远端玩家也能看到骑乘姿态 */
    public static final EntityType<SeatEntity> SEAT = Registry.register(
        Registries.ENTITY_TYPE,
        Identifier.of(SweatyFeet.MOD_ID, "seat"),
        EntityType.Builder.create(SeatEntity::new, SpawnGroup.MISC)
            .dimensions(0.0001F, 0.0001F)
            .maxTrackingRange(10)
            .build());

    /** 触发类加载完成注册（fabric 静态注册模式） */
    public static void init() {
    }

    private ModEntities() {
    }
}
