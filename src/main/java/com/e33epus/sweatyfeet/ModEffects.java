package com.e33epus.sweatyfeet;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
        DeferredRegister.create(Registries.MOB_EFFECT, SweatyFeet.MOD_ID);

    /** 汗脚：黄绿色 */
    public static final Holder<MobEffect> SWEATY_FEET =
        MOB_EFFECTS.register("sweaty_feet", () -> new SweatyFeetEffect(MobEffectCategory.HARMFUL, 0x9ACD32));

    /** 真菌感染：墨绿 */
    public static final Holder<MobEffect> FOOT_FUNGUS =
        MOB_EFFECTS.register("foot_fungus", () -> new FootFungusEffect(MobEffectCategory.HARMFUL, 0x228B22));

    /** 真菌扣血专用伤害类型：走数据包注册（data/sweatyfeet/damage_type/fungus.json）。
     *  之前用 Holder.direct 不走注册表 → vanilla 的 damage_event 同步包按注册表 id 序列化，
     *  找不到 id 直接编码失败把玩家踢下线（实测实锤）。死亡消息键 death.attack.sweatyfeet.fungus 不变 */
    public static final ResourceKey<DamageType> FUNGUS_DAMAGE =
        ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(SweatyFeet.MOD_ID, "fungus"));

    private ModEffects() {
    }

    public static void register(IEventBus modBus) {
        MOB_EFFECTS.register(modBus);
    }
}
