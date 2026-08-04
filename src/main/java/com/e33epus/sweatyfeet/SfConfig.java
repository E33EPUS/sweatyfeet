package com.e33epus.sweatyfeet;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

/**
 * Sweaty Feet 配置。Cloth Config AutoConfig 注解驱动（GUI 在 Mods 列表 → Config 按钮）。
 * 注意：注册动作放在 SweatyFeet 构造的 Dist.CLIENT 分支里（官方要求 init 时注册、服务端禁用 AutoConfig），
 * 这里只存字段默认值；客户端启动时 INSTANCE 会被替换为 AutoConfig 管理的实例。
 */
@Config(name = SweatyFeet.MOD_ID)
public class SfConfig implements ConfigData {
    /** 配置实例：客户端启动时由 AutoConfig 填充；专用服务器进程保持默认值兜底 */
    public static SfConfig INSTANCE = new SfConfig();

    @ConfigEntry.Category("timing")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 86400)
    public int level1_seconds = 120;
    @ConfigEntry.Category("timing")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 86400)
    public int level2_seconds = 240;
    @ConfigEntry.Category("timing")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 86400)
    public int level3_seconds = 360;

    @ConfigEntry.Category("fungus")
    public boolean enable_fungus = true;
    @ConfigEntry.Category("fungus")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 86400)
    public int fungus_delay_seconds = 30;
    @ConfigEntry.Category("fungus")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 86400)
    public int fungus_seconds = 60;

    @ConfigEntry.Category("durations")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 86400)
    public int effect_seconds = 300;
    @ConfigEntry.Category("durations")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 86400)
    public int throw_debuff_seconds = 5;
    @ConfigEntry.Category("durations")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 86400)
    public int drink_poison_seconds = 3;

    @ConfigEntry.Category("visual")
    public boolean sweat_particles = true;
    @ConfigEntry.Category("visual")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
    public int sweat_particle_scale = 1;
    @ConfigEntry.Category("visual")
    public boolean sneeze_particles = true;
    @ConfigEntry.Category("visual")
    public boolean sneeze_sound = true;
}
