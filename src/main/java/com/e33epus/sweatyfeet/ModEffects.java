package com.e33epus.sweatyfeet;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageEffects;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageSource;
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

    /** 真菌扣血专用伤害源：自定义死法（Holder.direct 独立持有，不走注册表；
     *  死亡消息走 lang 键 death.attack.sweatyfeet.fungus） */
    public static final DamageSource FUNGUS_DAMAGE = new DamageSource(
        Holder.direct(new DamageType("sweatyfeet.fungus", DamageScaling.NEVER, 0.0F, DamageEffects.HURT)));

    private ModEffects() {
    }

    public static void register(IEventBus modBus) {
        MOB_EFFECTS.register(modBus);
    }
}
