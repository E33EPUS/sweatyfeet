package com.e33epus.sweatyfeet;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
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

    private ModEffects() {
    }

    public static void register(IEventBus modBus) {
        MOB_EFFECTS.register(modBus);
    }
}
