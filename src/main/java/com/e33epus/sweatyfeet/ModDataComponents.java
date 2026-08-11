package com.e33epus.sweatyfeet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.component.ComponentType;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Identifier;

public final class ModDataComponents {
    public static final Codec<SweatData> SWEAT_CODEC = RecordCodecBuilder.create(inst -> inst.group(
        Codec.INT.optionalFieldOf("level", 0).forGetter(SweatData::level),
        TextCodecs.CODEC.optionalFieldOf("original_name")
            .forGetter(d -> Optional.ofNullable(d.originalName()))
    ).apply(inst, (lvl, name) -> new SweatData(lvl, name.orElse(null))));

    public static final PacketCodec<net.minecraft.network.RegistryByteBuf, SweatData> SWEAT_STREAM_CODEC = PacketCodec.tuple(
        PacketCodecs.INTEGER,
        SweatData::level,
        PacketCodecs.optional(TextCodecs.PACKET_CODEC),
        d -> Optional.ofNullable(d.originalName()),
        (lvl, name) -> new SweatData(lvl, name.orElse(null))
    );

    public static final ComponentType<SweatData> SWEAT = Registry.register(
        Registries.DATA_COMPONENT_TYPE,
        Identifier.of(SweatyFeet.MOD_ID, "sweat"),
        ComponentType.<SweatData>builder()
            .codec(SWEAT_CODEC)
            .packetCodec(SWEAT_STREAM_CODEC)
            .build());

    /** 汗液瓶等级组件：1/2/3，喝的效果和名字按等级变（仿原版药水分级模式） */
    public static final ComponentType<Integer> SWEAT_LEVEL = Registry.register(
        Registries.DATA_COMPONENT_TYPE,
        Identifier.of(SweatyFeet.MOD_ID, "sweat_level"),
        ComponentType.<Integer>builder()
            .codec(Codec.INT)
            .packetCodec(PacketCodecs.INTEGER)
            .build());

    /** 汗液瓶风味组件：leather/iron/gold/diamond/netherite（倒汗时按汗靴材质写入，对应风味名与 lore） */
    public static final ComponentType<String> SWEAT_FLAVOR = Registry.register(
        Registries.DATA_COMPONENT_TYPE,
        Identifier.of(SweatyFeet.MOD_ID, "sweat_flavor"),
        ComponentType.<String>builder()
            .codec(Codec.STRING)
            .packetCodec(PacketCodecs.STRING)
            .build());

    /** 饮品类型组件：speed/strength/...（一个饮品物品，按类型变效果和名字） */
    public static final ComponentType<String> DRINK_TYPE = Registry.register(
        Registries.DATA_COMPONENT_TYPE,
        Identifier.of(SweatyFeet.MOD_ID, "drink_type"),
        ComponentType.<String>builder()
            .codec(Codec.STRING)
            .packetCodec(PacketCodecs.STRING)
            .build());

    /** 汗靴泡水清洗计时（瞬态：服务端内部用，客户端不需要 → 不 packetCodec，
     *  泡洗中每秒 +20 写组件不会触发物品同步包） */
    public static final ComponentType<Integer> SWEAT_WASH_TICKS = Registry.register(
        Registries.DATA_COMPONENT_TYPE,
        Identifier.of(SweatyFeet.MOD_ID, "sweat_wash_ticks"),
        ComponentType.<Integer>builder()
            .codec(Codec.INT)
            .build());

    /** 触发类加载完成注册（fabric 静态注册模式） */
    public static void init() {
    }

    private ModDataComponents() {
    }
}
