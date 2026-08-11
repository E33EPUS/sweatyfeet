package com.e33epus.sweatyfeet;

import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

public final class ModEffects {
    /** 汗脚：黄绿色 */
    public static final RegistryEntry<StatusEffect> SWEATY_FEET = Registry.registerReference(
        Registries.STATUS_EFFECT,
        Identifier.of(SweatyFeet.MOD_ID, "sweaty_feet"),
        new SweatyFeetEffect(StatusEffectCategory.HARMFUL, 0x9ACD32));

    /** 真菌感染：墨绿 */
    public static final RegistryEntry<StatusEffect> FOOT_FUNGUS = Registry.registerReference(
        Registries.STATUS_EFFECT,
        Identifier.of(SweatyFeet.MOD_ID, "foot_fungus"),
        new FootFungusEffect(StatusEffectCategory.HARMFUL, 0x228B22));

    /** 真菌扣血专用伤害类型：走数据包注册（data/sweatyfeet/damage_type/fungus.json）。
     *  之前用 RegistryEntry.direct 不走注册表 → vanilla 的 damage_event 同步包按注册表 id 序列化，
     *  找不到 id 直接编码失败把玩家踢下线（实测实锤）。死亡消息键 death.attack.sweatyfeet.fungus 不变 */
    public static final RegistryKey<DamageType> FUNGUS_DAMAGE =
        RegistryKey.of(RegistryKeys.DAMAGE_TYPE, Identifier.of(SweatyFeet.MOD_ID, "fungus"));

    /** 触发类加载完成注册（fabric 静态注册模式） */
    public static void init() {
    }

    private ModEffects() {
    }
}
