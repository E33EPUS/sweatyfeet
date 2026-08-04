package com.e33epus.sweatyfeet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENT_TYPES =
        DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, SweatyFeet.MOD_ID);

    public static final Codec<SweatData> SWEAT_CODEC = RecordCodecBuilder.create(inst -> inst.group(
        ComponentSerialization.CODEC.optionalFieldOf("original_name")
            .forGetter(d -> Optional.ofNullable(d.originalName()))
    ).apply(inst, name -> new SweatData(name.orElse(null))));

    public static final StreamCodec<RegistryFriendlyByteBuf, SweatData> SWEAT_STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.optional(ComponentSerialization.STREAM_CODEC),
        d -> Optional.ofNullable(d.originalName()),
        name -> new SweatData(name.orElse(null))
    );

    public static final Supplier<DataComponentType<SweatData>> SWEAT =
        DATA_COMPONENT_TYPES.registerComponentType("sweat",
            builder -> builder
                .persistent(SWEAT_CODEC)
                .networkSynchronized(SWEAT_STREAM_CODEC));

    /** 汗液瓶等级组件：1/2/3，喝的效果和名字按等级变（仿原版药水分级模式） */
    public static final Supplier<DataComponentType<Integer>> SWEAT_LEVEL =
        DATA_COMPONENT_TYPES.registerComponentType("sweat_level",
            builder -> builder
                .persistent(Codec.INT)
                .networkSynchronized(ByteBufCodecs.INT));

    /** 饮品类型组件：speed/strength/...（一个饮品物品，按类型变效果和名字） */
    public static final Supplier<DataComponentType<String>> DRINK_TYPE =
        DATA_COMPONENT_TYPES.registerComponentType("drink_type",
            builder -> builder
                .persistent(Codec.STRING)
                .networkSynchronized(ByteBufCodecs.STRING_UTF8));

    private ModDataComponents() {
    }

    public static void register(IEventBus modBus) {
        DATA_COMPONENT_TYPES.register(modBus);
    }
}
