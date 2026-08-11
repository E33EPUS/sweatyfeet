package com.e33epus.sweatyfeet;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Sweaty Feet 配置（Fabric 版）：自写 JSON 配置文件，零前置依赖。
 * - 存 config/sweatyfeet-server.json（服务端玩法配置，客户端读同文件）
 * - 游戏内 GUI 为自绘 Screen（ModMenu → Config 按钮，见 SfConfigScreen）
 * - 逻辑代码读静态字段，GUI 改完调 save() 写回即时生效
 * 默认值 = 0.1.2 固化值（与 NeoForge 端 toml 一致）。
 */
public final class SfConfig {
    private static final String FILE_NAME = "sweatyfeet-server.json";

    // 穿戴计时（秒）
    public static int LEVEL_1_SECONDS = 120;
    public static int LEVEL_2_SECONDS = 240;
    public static int LEVEL_3_SECONDS = 360;
    // 真菌
    public static boolean ENABLE_FUNGUS = true;
    public static int FUNGUS_DELAY_SECONDS = 60;
    public static boolean FUNGUS_DAMAGE_ENABLED = true;
    public static int FUNGUS_DAMAGE_INTERVAL_SECONDS = 3;
    public static boolean FUNGUS_INFECTION_ENABLED = true;
    public static int FUNGUS_INFECTION_RANGE = 3;
    public static int FUNGUS_INFECTION_INTERVAL_SECONDS = 3;
    public static boolean SMELL_ENABLED = true;
    public static int SMELL_RANGE = 5;
    // 效果时长（秒）
    public static int EFFECT_SECONDS = 300;
    public static int THROW_DEBUFF_SECONDS = 5;
    public static int DRINK_POISON_SECONDS = 3;
    public static int BOTTLE_NAUSEA_SECONDS = 10;
    public static int DRINK_BUFF_SECONDS = 30;
    public static int DEGRADE_SECONDS = 60;
    public static int WASH_SECONDS = 15;
    public static boolean WASH_BOOTS_ENABLED = true;
    public static int WASH_BOOTS_SECONDS = 15;
    // 移动
    public static boolean SLIDE_ENABLED = true;
    public static int SLIDE_RETENTION_PERCENT = 85;
    // 表现
    public static boolean SWEAT_PARTICLES = true;
    public static int SWEAT_PARTICLE_SCALE = 1;
    public static boolean SNEEZE_PARTICLES = true;
    public static boolean SOAK_UNDRESS_ENABLED = true;
    public static String SOAK_UNDRESS_TINT = "";
    // 调试
    public static boolean DEBUG_SHOW_TICKS = false;
    public static boolean DEBUG_FORCE_FUNGUS = false;
    public static boolean DEBUG_FORCE_SWEAT = false;
    public static boolean DEBUG_FORCE_LEVEL3 = false;
    public static boolean DEBUG_STATE_LOG = false;
    public static boolean DEBUG_FLOW_LOG = false;
    public static boolean DEBUG_UNDRESS = false;

    private SfConfig() {
    }

    public static void init() {
        Path p = configPath();
        if (!Files.exists(p)) {
            save();
            return;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
            LEVEL_1_SECONDS = getInt(root, "level1_seconds", 120);
            LEVEL_2_SECONDS = getInt(root, "level2_seconds", 240);
            LEVEL_3_SECONDS = getInt(root, "level3_seconds", 360);
            ENABLE_FUNGUS = getBool(root, "enable_fungus", true);
            FUNGUS_DELAY_SECONDS = getInt(root, "fungus_delay_seconds", 60);
            FUNGUS_DAMAGE_ENABLED = getBool(root, "fungus_damage_enabled", true);
            FUNGUS_DAMAGE_INTERVAL_SECONDS = getInt(root, "fungus_damage_interval_seconds", 3);
            FUNGUS_INFECTION_ENABLED = getBool(root, "fungus_infection_enabled", true);
            FUNGUS_INFECTION_RANGE = getInt(root, "fungus_infection_range", 3);
            FUNGUS_INFECTION_INTERVAL_SECONDS = getInt(root, "fungus_infection_interval_seconds", 3);
            SMELL_ENABLED = getBool(root, "smell_enabled", true);
            SMELL_RANGE = getInt(root, "smell_range", 5);
            EFFECT_SECONDS = getInt(root, "effect_seconds", 300);
            THROW_DEBUFF_SECONDS = getInt(root, "throw_debuff_seconds", 5);
            DRINK_POISON_SECONDS = getInt(root, "drink_poison_seconds", 3);
            BOTTLE_NAUSEA_SECONDS = getInt(root, "bottle_nausea_seconds", 10);
            DRINK_BUFF_SECONDS = getInt(root, "drink_buff_seconds", 30);
            DEGRADE_SECONDS = getInt(root, "degrade_seconds", 60);
            WASH_SECONDS = getInt(root, "wash_seconds", 15);
            WASH_BOOTS_ENABLED = getBool(root, "wash_boots_enabled", true);
            WASH_BOOTS_SECONDS = getInt(root, "wash_boots_seconds", 15);
            SLIDE_ENABLED = getBool(root, "slide_enabled", true);
            SLIDE_RETENTION_PERCENT = getInt(root, "slide_retention_percent", 85);
            SWEAT_PARTICLES = getBool(root, "sweat_particles", true);
            SWEAT_PARTICLE_SCALE = getInt(root, "sweat_particle_scale", 1);
            SNEEZE_PARTICLES = getBool(root, "sneeze_particles", true);
            SOAK_UNDRESS_ENABLED = getBool(root, "soak_undress_enabled", true);
            SOAK_UNDRESS_TINT = getStr(root, "soak_undress_tint", "");
            DEBUG_SHOW_TICKS = getBool(root, "debug_show_ticks", false);
            DEBUG_FORCE_FUNGUS = getBool(root, "debug_force_fungus", false);
            DEBUG_FORCE_SWEAT = getBool(root, "debug_force_sweat", false);
            DEBUG_FORCE_LEVEL3 = getBool(root, "debug_force_level3", false);
            DEBUG_STATE_LOG = getBool(root, "debug_state_log", false);
            DEBUG_FLOW_LOG = getBool(root, "debug_flow_log", false);
            DEBUG_UNDRESS = getBool(root, "debug_undress", false);
        } catch (Exception e) {
            com.mojang.logging.LogUtils.getLogger().warn("[SF] config load failed, using defaults", e);
        }
    }

    public static void save() {
        JsonObject root = new JsonObject();
        root.addProperty("level1_seconds", LEVEL_1_SECONDS);
        root.addProperty("level2_seconds", LEVEL_2_SECONDS);
        root.addProperty("level3_seconds", LEVEL_3_SECONDS);
        root.addProperty("enable_fungus", ENABLE_FUNGUS);
        root.addProperty("fungus_delay_seconds", FUNGUS_DELAY_SECONDS);
        root.addProperty("fungus_damage_enabled", FUNGUS_DAMAGE_ENABLED);
        root.addProperty("fungus_damage_interval_seconds", FUNGUS_DAMAGE_INTERVAL_SECONDS);
        root.addProperty("fungus_infection_enabled", FUNGUS_INFECTION_ENABLED);
        root.addProperty("fungus_infection_range", FUNGUS_INFECTION_RANGE);
        root.addProperty("fungus_infection_interval_seconds", FUNGUS_INFECTION_INTERVAL_SECONDS);
        root.addProperty("smell_enabled", SMELL_ENABLED);
        root.addProperty("smell_range", SMELL_RANGE);
        root.addProperty("effect_seconds", EFFECT_SECONDS);
        root.addProperty("throw_debuff_seconds", THROW_DEBUFF_SECONDS);
        root.addProperty("drink_poison_seconds", DRINK_POISON_SECONDS);
        root.addProperty("bottle_nausea_seconds", BOTTLE_NAUSEA_SECONDS);
        root.addProperty("drink_buff_seconds", DRINK_BUFF_SECONDS);
        root.addProperty("degrade_seconds", DEGRADE_SECONDS);
        root.addProperty("wash_seconds", WASH_SECONDS);
        root.addProperty("wash_boots_enabled", WASH_BOOTS_ENABLED);
        root.addProperty("wash_boots_seconds", WASH_BOOTS_SECONDS);
        root.addProperty("slide_enabled", SLIDE_ENABLED);
        root.addProperty("slide_retention_percent", SLIDE_RETENTION_PERCENT);
        root.addProperty("sweat_particles", SWEAT_PARTICLES);
        root.addProperty("sweat_particle_scale", SWEAT_PARTICLE_SCALE);
        root.addProperty("sneeze_particles", SNEEZE_PARTICLES);
        root.addProperty("soak_undress_enabled", SOAK_UNDRESS_ENABLED);
        root.addProperty("soak_undress_tint", SOAK_UNDRESS_TINT);
        root.addProperty("debug_show_ticks", DEBUG_SHOW_TICKS);
        root.addProperty("debug_force_fungus", DEBUG_FORCE_FUNGUS);
        root.addProperty("debug_force_sweat", DEBUG_FORCE_SWEAT);
        root.addProperty("debug_force_level3", DEBUG_FORCE_LEVEL3);
        root.addProperty("debug_state_log", DEBUG_STATE_LOG);
        root.addProperty("debug_flow_log", DEBUG_FLOW_LOG);
        root.addProperty("debug_undress", DEBUG_UNDRESS);
        try {
            Files.writeString(configPath(),
                new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(root),
                StandardCharsets.UTF_8);
        } catch (IOException e) {
            com.mojang.logging.LogUtils.getLogger().warn("[SF] config save failed", e);
        }
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    private static int getInt(JsonObject root, String key, int def) {
        return root.has(key) ? root.get(key).getAsInt() : def;
    }

    private static boolean getBool(JsonObject root, String key, boolean def) {
        return root.has(key) ? root.get(key).getAsBoolean() : def;
    }

    private static String getStr(JsonObject root, String key, String def) {
        return root.has(key) ? root.get(key).getAsString() : def;
    }
}
