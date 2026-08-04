package com.e33epus.sweatyfeet;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

/**
 * Sweaty Feet 配置。Cloth Config AutoConfig 注解驱动（GUI 在 Mods 列表 → Config 按钮）。
 * 注册动作放在 SweatyFeet 构造的 Dist.CLIENT 分支里（官方要求 init 时注册、服务端禁用 AutoConfig），
 * 这里只存字段默认值；客户端启动时 INSTANCE 会被替换为 AutoConfig 管理的实例。
 *
 * 数值字段故意不加 @ConfigEntry.BoundedDiscrete：无注解的 int 字段在 Cloth 里生成输入框（startIntField），
 * 比滑块好输精确值。输入负数/超大值最多导致阈值永不触发，不会崩溃。
 */
@Config(name = SweatyFeet.MOD_ID)
public class SfConfig implements ConfigData {
    /** 配置实例：客户端启动时由 AutoConfig 填充；专用服务器进程保持默认值兜底 */
    public static SfConfig INSTANCE = new SfConfig();

    @ConfigEntry.Category("timing")
    public int level1_seconds = 120;
    @ConfigEntry.Category("timing")
    public int level2_seconds = 240;
    @ConfigEntry.Category("timing")
    public int level3_seconds = 360;

    @ConfigEntry.Category("fungus")
    public boolean enable_fungus = true;
    @ConfigEntry.Category("fungus")
    public int fungus_delay_seconds = 30;
    @ConfigEntry.Category("fungus")
    @ConfigEntry.Gui.Tooltip
    public boolean fungus_damage_enabled = true;
    @ConfigEntry.Category("fungus")
    @ConfigEntry.Gui.Tooltip
    public int fungus_damage_interval_seconds = 3;
    @ConfigEntry.Category("fungus")
    @ConfigEntry.Gui.Tooltip
    public boolean fungus_infection_enabled = true;
    @ConfigEntry.Category("fungus")
    public int fungus_infection_range = 3;
    @ConfigEntry.Category("fungus")
    public int fungus_infection_interval_seconds = 3;
    @ConfigEntry.Category("fungus")
    @ConfigEntry.Gui.Tooltip
    public boolean smell_enabled = true;
    @ConfigEntry.Category("fungus")
    @ConfigEntry.Gui.Tooltip
    public int smell_range = 5;

    @ConfigEntry.Category("durations")
    public int effect_seconds = 300;
    @ConfigEntry.Category("durations")
    public int throw_debuff_seconds = 5;
    @ConfigEntry.Category("durations")
    public int drink_poison_seconds = 3;
    @ConfigEntry.Category("durations")
    public int bottle_nausea_seconds = 10;
    @ConfigEntry.Category("durations")
    public int drink_buff_seconds = 30;
    @ConfigEntry.Category("durations")
    @ConfigEntry.Gui.Tooltip
    public int degrade_seconds = 60;
    @ConfigEntry.Category("durations")
    @ConfigEntry.Gui.Tooltip
    public int wash_seconds = 15;

    @ConfigEntry.Category("movement")
    @ConfigEntry.Gui.Tooltip
    public boolean slide_enabled = true;
    @ConfigEntry.Category("movement")
    @ConfigEntry.Gui.Tooltip
    public int slide_retention_percent = 98;

    @ConfigEntry.Category("visual")
    public boolean sweat_particles = true;
    @ConfigEntry.Category("visual")
    public int sweat_particle_scale = 1;
    @ConfigEntry.Category("visual")
    public boolean sneeze_particles = true;

    @ConfigEntry.Category("debug")
    public boolean debug_show_ticks = false;
    @ConfigEntry.Category("debug")
    @ConfigEntry.Gui.Tooltip
    public boolean debug_force_fungus = false;
}
