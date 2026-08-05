package com.e33epus.sweatyfeet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Supplier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * 玩家持久化附件（下线重进汗脚/真菌还在，文档九.1）：
 * - SWEAT_STATE：汗脚当前等级（amp，-1=无）——脱鞋降级到底/洗脚清除时归 -1
 * - FUNGUS：是否感染真菌
 * serialize(Codec) → 存玩家 NBT，跨会话保留。
 * 上线的恢复逻辑在 SweatyFeetHandler.onPlayerTick（有状态但没效果 → 重新挂）。
 */
public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
        DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, SweatyFeet.MOD_ID);

    public static final Codec<Integer> SWEAT_STATE_CODEC = Codec.INT;

    public static final Supplier<AttachmentType<Integer>> SWEAT_STATE =
        ATTACHMENT_TYPES.register("sweat_state",
            () -> AttachmentType.builder(() -> -1).serialize(SWEAT_STATE_CODEC).build());

    public static final Supplier<AttachmentType<Boolean>> FUNGUS =
        ATTACHMENT_TYPES.register("fungus",
            () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL).build());

    private ModAttachments() {
    }

    public static void register(IEventBus modBus) {
        ATTACHMENT_TYPES.register(modBus);
    }
}
