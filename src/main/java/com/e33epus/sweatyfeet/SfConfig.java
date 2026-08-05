package com.e33epus.sweatyfeet;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Sweaty Feet 配置。NeoForge 内置 ModConfigSpec（零前置）：
 * - 存 config/sweatyfeet-server.toml（服务端配置，玩法逻辑全在服务端）
 * - 游戏内 GUI 为自绘 Screen（Mods 列表 → Config 按钮，见 SfConfigScreen）
 * - 逻辑代码读静态 getter，GUI 改完 set() 即时生效
 */
public final class SfConfig {
    public static final ModConfigSpec SERVER_SPEC;

    // 穿戴计时（秒）
    public static final ModConfigSpec.IntValue LEVEL_1_SECONDS;
    public static final ModConfigSpec.IntValue LEVEL_2_SECONDS;
    public static final ModConfigSpec.IntValue LEVEL_3_SECONDS;
    // 真菌
    public static final ModConfigSpec.BooleanValue ENABLE_FUNGUS;
    public static final ModConfigSpec.IntValue FUNGUS_DELAY_SECONDS;
    public static final ModConfigSpec.BooleanValue FUNGUS_DAMAGE_ENABLED;
    public static final ModConfigSpec.IntValue FUNGUS_DAMAGE_INTERVAL_SECONDS;
    public static final ModConfigSpec.BooleanValue FUNGUS_INFECTION_ENABLED;
    public static final ModConfigSpec.IntValue FUNGUS_INFECTION_RANGE;
    public static final ModConfigSpec.IntValue FUNGUS_INFECTION_INTERVAL_SECONDS;
    public static final ModConfigSpec.BooleanValue SMELL_ENABLED;
    public static final ModConfigSpec.IntValue SMELL_RANGE;
    // 效果时长（秒）
    public static final ModConfigSpec.IntValue EFFECT_SECONDS;
    public static final ModConfigSpec.IntValue THROW_DEBUFF_SECONDS;
    public static final ModConfigSpec.IntValue DRINK_POISON_SECONDS;
    public static final ModConfigSpec.IntValue BOTTLE_NAUSEA_SECONDS;
    public static final ModConfigSpec.IntValue DRINK_BUFF_SECONDS;
    public static final ModConfigSpec.IntValue DEGRADE_SECONDS;
    public static final ModConfigSpec.IntValue WASH_SECONDS;
    // 移动
    public static final ModConfigSpec.BooleanValue SLIDE_ENABLED;
    public static final ModConfigSpec.IntValue SLIDE_RETENTION_PERCENT;
    // 表现
    public static final ModConfigSpec.BooleanValue SWEAT_PARTICLES;
    public static final ModConfigSpec.IntValue SWEAT_PARTICLE_SCALE;
    public static final ModConfigSpec.BooleanValue SNEEZE_PARTICLES;
    // 调试
    public static final ModConfigSpec.BooleanValue DEBUG_SHOW_TICKS;
    public static final ModConfigSpec.BooleanValue DEBUG_FORCE_FUNGUS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("汗脚 debuff 的穿戴时间阈值（秒）。等级按穿靴子总时长推进，脱鞋清零。").push("timing");
        LEVEL_1_SECONDS = builder.comment("汗脚 1 级所需穿戴秒数（触发汗化并改名「充满汗液的xxx」）")
            .defineInRange("level1_seconds", 120, 1, 86400);
        LEVEL_2_SECONDS = builder.comment("汗脚 2 级所需穿戴秒数")
            .defineInRange("level2_seconds", 240, 1, 86400);
        LEVEL_3_SECONDS = builder.comment("汗脚 3 级所需穿戴秒数")
            .defineInRange("level3_seconds", 360, 1, 86400);
        builder.pop();

        builder.comment("真菌感染：3 级后继续穿靴子触发。").push("fungus");
        ENABLE_FUNGUS = builder.comment("是否启用真菌感染（false = 只到 3 级汗脚为止）")
            .define("enable_fungus", true);
        FUNGUS_DELAY_SECONDS = builder.comment("3 级后再继续穿多少秒触发真菌感染")
            .defineInRange("fungus_delay_seconds", 30, 1, 86400);
        FUNGUS_DAMAGE_ENABLED = builder.comment("真菌是否缓慢扣血（magic 伤害，无视护甲，可致死）")
            .define("fungus_damage_enabled", true);
        FUNGUS_DAMAGE_INTERVAL_SECONDS = builder.comment("真菌扣血间隔（秒）")
            .defineInRange("fungus_damage_interval_seconds", 3, 1, 86400);
        FUNGUS_INFECTION_ENABLED = builder.comment("真菌是否可传染（站在感染者附近会被传染）")
            .define("fungus_infection_enabled", true);
        FUNGUS_INFECTION_RANGE = builder.comment("真菌传染范围（格）")
            .defineInRange("fungus_infection_range", 3, 1, 64);
        FUNGUS_INFECTION_INTERVAL_SECONDS = builder.comment("真菌传染判定间隔（秒）")
            .defineInRange("fungus_infection_interval_seconds", 3, 1, 86400);
        SMELL_ENABLED = builder.comment("赤脚散臭：赤脚且还有汗脚时，附近玩家持续反胃（自己不受影响）")
            .define("smell_enabled", true);
        SMELL_RANGE = builder.comment("散臭范围（格）")
            .defineInRange("smell_range", 5, 1, 64);
        builder.pop();

        builder.comment("效果时长（秒）。").push("durations");
        EFFECT_SECONDS = builder.comment("汗脚 debuff 单次时长（秒），穿着期间持续刷新")
            .defineInRange("effect_seconds", 300, 1, 86400);
        THROW_DEBUFF_SECONDS = builder.comment("被汗液瓶砸中后挂汗脚 1 级的时长（秒）")
            .defineInRange("throw_debuff_seconds", 5, 1, 86400);
        DRINK_POISON_SECONDS = builder.comment("三级汗液瓶中毒时长（秒）")
            .defineInRange("drink_poison_seconds", 3, 1, 86400);
        BOTTLE_NAUSEA_SECONDS = builder.comment("二级汗液瓶反胃时长（秒）")
            .defineInRange("bottle_nausea_seconds", 10, 1, 86400);
        DRINK_BUFF_SECONDS = builder.comment("饮品 buff 时长（秒）")
            .defineInRange("drink_buff_seconds", 30, 1, 86400);
        DEGRADE_SECONDS = builder.comment("汗脚降级每级时长（秒）：脱鞋后 3级→2级→1级→消除")
            .defineInRange("degrade_seconds", 60, 1, 86400);
        WASH_SECONDS = builder.comment("赤脚泡水洗脚所需秒数（真菌泡水洗不掉，要用花露水）")
            .defineInRange("wash_seconds", 15, 1, 86400);
        builder.pop();

        builder.comment("移动效果（汗脚 2 级起）。").push("movement");
        SLIDE_ENABLED = builder.comment("汗脚 2 级起脚滑（摩擦改为冰面值，双端一致）")
            .define("slide_enabled", true);
        SLIDE_RETENTION_PERCENT = builder.comment("脚滑摩擦值（%）——100 同冰面，越低越黏")
            .defineInRange("slide_retention_percent", 98, 1, 100);
        builder.pop();

        builder.comment("表现选项。").push("visual");
        SWEAT_PARTICLES = builder.comment("汗脚效果是否冒汗粒子")
            .define("sweat_particles", true);
        SWEAT_PARTICLE_SCALE = builder.comment("汗粒子数量倍数（等级越高越多）")
            .defineInRange("sweat_particle_scale", 1, 1, 10);
        SNEEZE_PARTICLES = builder.comment("真菌感染是否打喷嚏粒子")
            .define("sneeze_particles", true);
        builder.pop();

        builder.comment("调试。").push("debug");
        DEBUG_SHOW_TICKS = builder.comment("action bar 实时显示穿戴 tick 与汗脚等级")
            .define("debug_show_ticks", false);
        DEBUG_FORCE_FUNGUS = builder.comment("穿靴即强制真菌（调试用，跳过 3 级等待）")
            .define("debug_force_fungus", false);
        builder.pop();

        SERVER_SPEC = builder.build();
    }

    private SfConfig() {
    }
}
