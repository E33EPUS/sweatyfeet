package com.e33epus.sweatyfeet;

import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, SweatyFeet.MOD_ID);

    public static final Supplier<EntityType<SweatBottleProjectile>> SWEAT_BOTTLE =
        ENTITY_TYPES.register("sweat_bottle", () -> EntityType.Builder
            .<SweatBottleProjectile>of(SweatBottleProjectile::new, MobCategory.MISC)
            .sized(0.25F, 0.25F)
            .clientTrackingRange(4)
            .updateInterval(10)
            .build("sweat_bottle"));

    /** 隐形座位：clientTrackingRange 拉满保证远端玩家也能看到骑乘姿态 */
    public static final Supplier<EntityType<SeatEntity>> SEAT =
        ENTITY_TYPES.register("seat", () -> EntityType.Builder
            .<SeatEntity>of(SeatEntity::new, MobCategory.MISC)
            .sized(0.0001F, 0.0001F)
            .clientTrackingRange(10)
            .build("seat"));

    private ModEntities() {
    }

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
    }
}
