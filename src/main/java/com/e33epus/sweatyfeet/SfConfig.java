package com.e33epus.sweatyfeet;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;

/**
 * Sweaty Feet 配置。Cloth Config AutoConfig 注解驱动：
 * - 游戏内 GUI 在 Mods 列表 → Sweaty Feet → Config 按钮（IConfigScreenFactory 挂载）
 * - 磁盘持久化 config/sweatyfeet.json（Gson）
 * - 逻辑代码读 INSTANCE 字段，改完保存立即生效（字段实时读取）
 */
@Config(name = SweatyFeet.MOD_ID)
public class SfConfig implements ConfigData {
    /** 双端注册并加载（服务端也要读计时参数），GUI 生成在客户端入口做 */
    public static final SfConfig INSTANCE = AutoConfig.register(SfConfig.class, GsonConfigSerializer::new).getConfig();

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
