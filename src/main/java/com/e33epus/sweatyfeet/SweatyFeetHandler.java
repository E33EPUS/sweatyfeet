package com.e33epus.sweatyfeet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 核心玩法：穿戴计时 → 汗化 → 等级推进 → 真菌感染；脱鞋清零；潜行右键倒汗。
 * 计时放服务端内存（脱鞋/下线即清），汗化状态放靴子的 DataComponent。
 */
@EventBusSubscriber(modid = SweatyFeet.MOD_ID)
public final class SweatyFeetHandler {
    public static final int REFRESH_INTERVAL = 20;         // 每 20 tick 刷新一次 debuff

    /** 玩家总穿戴 tick（服务端内存态，脱鞋/下线即清，不跨会话） */
    private static final Map<UUID, Integer> WEAR_TICKS = new HashMap<>();

    /** 脱鞋后降级倒计时（玩家 → 当前等级剩余 tick）；穿鞋即清，暂停降级 */
    private static final Map<UUID, Integer> DEGRADE_TICKS = new HashMap<>();

    /** 赤脚连续泡水 tick（玩家 → 连续 tick）；断水/穿鞋清零，满 wash_seconds 清汗脚 */
    private static final Map<UUID, Integer> WASH_TICKS = new HashMap<>();

    /** 上一 tick 手持三级汗靴的玩家（过渡检测用：放下瞬间清反胃，不伤其他来源） */
    private static final Set<UUID> HELD_L3 = new HashSet<>();

    /** 盆泡脚：累计计时（玩家 → 累计 tick）；离开暂停不清零，满 wash_seconds 洗完 + 盆变浑 */
    private static final Map<UUID, Integer> BASIN_TICKS = new HashMap<>();

    /** 盆泡脚：当前泡的目标盆（玩家 → 盆坐标）；空手右键盆（赤脚+有水+有汗脚）时记录 */
    private static final Map<UUID, BlockPos> BASIN_POS = new HashMap<>();

    private static int degradeTicks() {
        return SfConfig.DEGRADE_SECONDS.get() * 20;
    }

    private static int washTicks() {
        return SfConfig.WASH_SECONDS.get() * 20;
    }

    /** 洗脚所需 tick（随汗脚等级递增）：1 级 T_base，2 级 2×，3 级 3× */
    private static int washTicksFor(Player player) {
        MobEffectInstance sf = player.getEffect(ModEffects.SWEATY_FEET);
        int amp = sf == null ? 0 : sf.getAmplifier();
        return washTicks() * (amp + 1);
    }

    private static int lvl1() {
        return SfConfig.LEVEL_1_SECONDS.get() * 20;
    }

    private static int lvl2() {
        return SfConfig.LEVEL_2_SECONDS.get() * 20;
    }

    private static int lvl3() {
        return SfConfig.LEVEL_3_SECONDS.get() * 20;
    }

    private static int fungusDelay() {
        return SfConfig.FUNGUS_DELAY_SECONDS.get() * 20;
    }

    private static int effectTicks() {
        return SfConfig.EFFECT_SECONDS.get() * 20;
    }

    private SweatyFeetHandler() {
    }

    /**
     * 每 tick 级联扫描（每秒一次）：
     * 1) 汗靴扔水里泡洗（受 WASH_BOOTS_ENABLED 配置）
     * 2) 三级汗靴丢在地上散发绿臭味粒子 + 附近玩家（含本人）反胃（环境污染）
     */
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (level.getGameTime() % 20 != 0) {
            return; // 每秒扫一次
        }
        if (SfConfig.WASH_BOOTS_ENABLED.get()) {
            int washBootsTicks = SfConfig.WASH_BOOTS_SECONDS.get() * 20;
            for (net.minecraft.world.entity.Entity e : level.getEntities().getAll()) {
                if (!(e instanceof ItemEntity itemEntity) || !itemEntity.isInWater()) {
                    continue;
                }
                ItemStack stack = itemEntity.getItem();
                if (!stack.is(ItemTags.FOOT_ARMOR) || !stack.has(ModDataComponents.SWEAT.get())) {
                    continue;
                }
                // 泡洗计时随等级递增（1 级 T_base，3 级 3×）；组件累加，冒水花粒子
                SweatData data = stack.get(ModDataComponents.SWEAT.get());
                int need = washBootsTicks * (data.level() + 1);
                int washed = stack.getOrDefault(ModDataComponents.SWEAT_WASH_TICKS.get(), 0) + 20;
                stack.set(ModDataComponents.SWEAT_WASH_TICKS.get(), washed);
                level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP,
                    itemEntity.getX(), itemEntity.getY() + 0.2, itemEntity.getZ(),
                    3, 0.15, 0.1, 0.15, 0.0);
                if (washed >= need) {
                    // 洗干净：去汗液 + 还原自定义名
                    stack.remove(ModDataComponents.SWEAT.get());
                    stack.remove(ModDataComponents.SWEAT_WASH_TICKS.get());
                    if (data != null && data.originalName() != null) {
                        stack.set(DataComponents.CUSTOM_NAME, data.originalName());
                    } else {
                        stack.remove(DataComponents.CUSTOM_NAME);
                    }
                }
            }
        }
        // 三级汗靴丢地污染：绿粒子 + 附近玩家（含丢者本人）反胃
        double smellRangeSq = (double) SfConfig.SMELL_RANGE.get() * SfConfig.SMELL_RANGE.get();
        for (net.minecraft.world.entity.Entity e : level.getEntities().getAll()) {
            if (!(e instanceof ItemEntity itemEntity)) {
                continue;
            }
            ItemStack stack = itemEntity.getItem();
            SweatData data = stack.get(ModDataComponents.SWEAT.get());
            if (data == null || data.level() < 2) {
                continue; // 只有三级汗靴污染环境
            }
            level.sendParticles(ParticleTypes.COMPOSTER,
                itemEntity.getX(), itemEntity.getY() + 0.2, itemEntity.getZ(),
                2, 0.3, 0.2, 0.3, 0.0);
            giveNearbyNausea(level, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(),
                smellRangeSq, null, true);
        }
    }

    /** 给范围内玩家反胃（3 秒）；self 为 null 时包含所有玩家，否则排除 self */
    private static void giveNearbyNausea(ServerLevel level, double x, double y, double z,
                                         double rangeSq, Player self, boolean includeSelf) {
        for (Player other : level.players()) {
            if (!includeSelf && other == self) {
                continue;
            }
            if (other.hasEffect(MobEffects.CONFUSION)) {
                continue;
            }
            if (other.distanceToSqr(x, y, z) <= rangeSq) {
                other.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0, false, true));
            }
        }
    }

    /** 洗脚 HUD 提示：穿鞋泡水提醒脱鞋；赤脚洗脚显示倒计时（action bar，物品栏正上方不重叠） */
    private static void showWashHud(Player player) {
        if (player.tickCount % 20 != 0) {
            return; // 每秒刷新
        }
        if (player.hasEffect(ModEffects.SWEATY_FEET) && player.isInWater()) {
            if (!player.getItemBySlot(EquipmentSlot.FEET).is(ItemTags.FOOT_ARMOR)) {
                // 赤脚洗脚中：显示剩余秒数（按当前等级需求）
                int consecutive = WASH_TICKS.getOrDefault(player.getUUID(), 0);
                int left = Math.max(0, (washTicksFor(player) - consecutive) / 20);
                player.displayClientMessage(
                    Component.translatable("sweatyfeet.msg.washing", left), true);
            } else {
                // 穿鞋泡水：提醒脱鞋
                player.displayClientMessage(
                    Component.translatable("sweatyfeet.msg.take_off"), true);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }

        // 手持三级汗靴：持续反胃（文档：放下即消）。注意不能每 tick 无脑 remove——
        // 那会把二级汗液瓶等其他来源的反胃也秒删。用过渡检测：只在"刚放下"那一 tick 清一次
        ItemStack held = player.getMainHandItem();
        if (!held.is(ItemTags.FOOT_ARMOR)) {
            held = player.getOffhandItem();
        }
        SweatData heldData = held.get(ModDataComponents.SWEAT.get());
        UUID heldId = player.getUUID();
        if (heldData != null && heldData.level() >= 2) {
            HELD_L3.add(heldId);
            // 60 tick、剩余低于 40 才补：每次补都比剩余长 → 触发客户端同步包，眩晕持续。
            // 之前每 tick 刷同值 40：服务端无变化不发同步，客户端 2 秒到期 → 看不到屏幕扭曲
            MobEffectInstance cur = player.getEffect(MobEffects.CONFUSION);
            if (cur == null || cur.getDuration() < 100) {
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 1, false, true));
            }
        } else if (HELD_L3.remove(heldId) && player.hasEffect(MobEffects.CONFUSION)) {
            player.removeEffect(MobEffects.CONFUSION);
        }

        // 真菌感染者：无论是否穿鞋，散发绿粒子 + 附近玩家反胃（类似三级脱鞋）
        if (player.hasEffect(ModEffects.FOOT_FUNGUS) && player.tickCount % 20 == 0
            && player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.COMPOSTER,
                player.getX(), player.getY() + 0.1, player.getZ(),
                2, 0.3, 0.0, 0.3, 0.0);
            double smellRangeSq = (double) SfConfig.SMELL_RANGE.get() * SfConfig.SMELL_RANGE.get();
            giveNearbyNausea(serverLevel, player.getX(), player.getY(), player.getZ(),
                smellRangeSq, player, false);
        }

        // 持久化恢复：下线重进后按附件把汗脚/真菌重新挂回（每 20 tick 检查一次，避免每 tick 写）
        if (player.tickCount % 20 == 0) {
            int savedAmp = player.getData(ModAttachments.SWEAT_STATE);
            if (savedAmp >= 0 && !player.hasEffect(ModEffects.SWEATY_FEET)) {
                player.removeEffect(ModEffects.SWEATY_FEET);
                player.addEffect(sweatyFeetEffect(effectTicks(), savedAmp));
            }
            if (player.getData(ModAttachments.FUNGUS) && !player.hasEffect(ModEffects.FOOT_FUNGUS)) {
                player.addEffect(new MobEffectInstance(ModEffects.FOOT_FUNGUS, MobEffectInstance.INFINITE_DURATION, 0, false, true));
            }
        }

        // 真菌传染扩散：不依赖靴子，被传染者也能继续传染（站在感染者附近一段时间被传）
        if (SfConfig.FUNGUS_INFECTION_ENABLED.get()
            && player.hasEffect(ModEffects.FOOT_FUNGUS)
            && player.tickCount % (SfConfig.FUNGUS_INFECTION_INTERVAL_SECONDS.get() * 20) == 0) {
            double rangeSq = (double) SfConfig.FUNGUS_INFECTION_RANGE.get() * SfConfig.FUNGUS_INFECTION_RANGE.get();
            for (Player other : player.level().players()) {
                if (other == player || other.hasEffect(ModEffects.FOOT_FUNGUS)) {
                    continue;
                }
                if (other.distanceToSqr(player) <= rangeSq) {
                    // 传染也给无限时长：真菌只能被花露水/倒汗消除，不会自然消失
                    other.addEffect(new MobEffectInstance(ModEffects.FOOT_FUNGUS, MobEffectInstance.INFINITE_DURATION, 0, false, true));
                    other.setData(ModAttachments.FUNGUS, true); // 持久化传染
                }
            }
        }

        // 盆泡脚会话（v2：坐凳+脱鞋+右键盆开始；这里推进累计计时）
        tickBasinSoak(player);

        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (!boots.is(ItemTags.FOOT_ARMOR)) {
            // 脱鞋：清穿戴计时，汗脚走"按级降级"（每级 60 秒递减，3→2→1→消除）
            WEAR_TICKS.remove(player.getUUID());
            degradeSweatyFeet(player);
            // 洗脚：赤脚泡水满 wash_seconds 清汗脚（跳过降级）；真菌泡水洗不掉
            handleWashOff(player);
            // 洗脚 HUD：穿鞋泡水提醒脱鞋 / 赤脚泡水倒计时
            showWashHud(player);
            // 散臭：赤脚 + 有汗脚 → 附近玩家持续反胃（穿鞋防臭，洗脚/降级完不臭）
            spreadFootSmell(player);
            return;
        }

        // 穿鞋（含汗靴）：降级/洗脚暂停，回到冻结/重新计时
        DEGRADE_TICKS.remove(player.getUUID());
        WASH_TICKS.remove(player.getUUID());

        int totalTicks = WEAR_TICKS.merge(player.getUUID(), 1, Integer::sum);
        SweatData data = boots.get(ModDataComponents.SWEAT.get());

        if (data == null) {
            // 未汗化：等汗化触发；同时冻结残余汗脚效果（脱鞋残余的效果，再穿时暂停倒计时，
            // 穿鞋期间效果不计时，只有脱鞋才走完——用户语义"穿鞋冻结/脱鞋倒计时"）
            if (totalTicks % REFRESH_INTERVAL == 0) {
                MobEffectInstance leftover = player.getEffect(ModEffects.SWEATY_FEET);
                if (leftover != null) {
                    player.addEffect(sweatyFeetEffect(effectTicks(), leftover.getAmplifier()));
                }
            }
            if (totalTicks >= lvl1()) {
                sweatify(player, boots);
            }
        } else {
            // 已汗化：汗靴等级固化在组件里，只升不降（脱鞋再穿汗靴等级保留）
            // 汗脚效果等级 = 组件等级与当前穿戴进度取最大：汗靴再穿立即恢复组件等级的效果
            int amplifier = Math.max(computeAmplifier(totalTicks, lvl2(), lvl3()), data.level());
            if (amplifier > data.level() && totalTicks % REFRESH_INTERVAL == 0) {
                boots.set(ModDataComponents.SWEAT.get(), data.withLevel(amplifier));
                data = boots.get(ModDataComponents.SWEAT.get());
                renameSweatyBoots(player, boots, data); // 升级同步改名（等级II/III）
            }
            if (totalTicks % REFRESH_INTERVAL == 0) {
                player.addEffect(sweatyFeetEffect(effectTicks(), amplifier));
                player.setData(ModAttachments.SWEAT_STATE, amplifier); // 持久化当前等级
                // 3 级：额外减速（汗脚3级 = 脚滑 + 减速）
                if (amplifier >= 2) {
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, effectTicks(), 0, false, true));
                }
            }
            // 3 级后继续穿 30 秒 → 真菌感染（无限时长：只能被花露水/倒汗消除，脱鞋不消失）
            if (SfConfig.ENABLE_FUNGUS.get()
                && totalTicks >= lvl3() + fungusDelay()
                && !player.hasEffect(ModEffects.FOOT_FUNGUS)
                && totalTicks % REFRESH_INTERVAL == 0) {
                player.addEffect(new MobEffectInstance(ModEffects.FOOT_FUNGUS, MobEffectInstance.INFINITE_DURATION, 0, false, true));
                player.setData(ModAttachments.FUNGUS, true); // 持久化真菌
            }
        }

        // Debug：action bar 实时显示穿戴 tick 与汗脚等级（肉眼验证计时）
        if (SfConfig.DEBUG_SHOW_TICKS.get() && totalTicks % 20 == 0) {
            int lvl = data == null ? -1 : computeAmplifier(totalTicks, lvl2(), lvl3()) + 1;
            player.displayClientMessage(Component.literal("SF tick=" + totalTicks + " lvl=" + lvl), true);
        }
        // Debug：强制真菌（方便测真菌表现，不看 3 级时长）
        if (SfConfig.DEBUG_FORCE_FUNGUS.get() && totalTicks % 20 == 0 && !player.hasEffect(ModEffects.FOOT_FUNGUS)) {
            player.addEffect(new MobEffectInstance(ModEffects.FOOT_FUNGUS, MobEffectInstance.INFINITE_DURATION, 0, false, true));
        }
    }

    /**
     * 散臭：赤脚 + 身上有汗脚效果（降级中）→ 附近玩家持续反胃。
     * 自己不受影响（自己闻不到自己脚臭）；汗脚等级越高反胃越强。
     * 穿鞋/泡水洗脚/降级完成后效果消失 → 停止散臭。
     */
    private static void spreadFootSmell(Player player) {
        if (!SfConfig.SMELL_ENABLED.get()) {
            return;
        }
        MobEffectInstance sf = player.getEffect(ModEffects.SWEATY_FEET);
        if (sf == null) {
            return;
        }
        if (player.tickCount % 20 != 0) {
            return; // 每秒刷新一次
        }
        double rangeSq = (double) SfConfig.SMELL_RANGE.get() * SfConfig.SMELL_RANGE.get();
        for (Player other : player.level().players()) {
            if (other == player || other.hasEffect(MobEffects.CONFUSION)) {
                continue;
            }
            if (other.distanceToSqr(player) <= rangeSq) {
                // 反胃 3 秒，amplifier = 汗脚等级（越臭反胃越强）
                other.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, sf.getAmplifier(), false, true));
            }
        }
    }

    /**
     * 洗脚：赤脚泡水或站洗脚盆上，连续满 wash_seconds 清汗脚（跳过降级直接消除）。
     * 只清汗脚不清真菌——真菌只能用花露水治（泡水/泡盆洗不掉）。
     * 连续计时：断水/离开盆清零，防"沾一下水就算洗"。
     */
    private static void handleWashOff(Player player) {
        boolean inWater = player.isInWater();
        boolean onBasin = player.getBlockStateOn().is(ModBlocks.WASH_BASIN.get());
        if (!inWater && !onBasin) {
            WASH_TICKS.remove(player.getUUID());
            return;
        }
        UUID id = player.getUUID();
        if (!player.hasEffect(ModEffects.SWEATY_FEET)) {
            WASH_TICKS.remove(id);
            return; // 脚不臭泡水 = 游泳，不播洗脚声不累计（实测无汗脚水里一直响洗脚声）
        }
        int consecutive = WASH_TICKS.merge(id, 1, Integer::sum);
        // 泡脚表现：水花粒子 + 水声（每秒）
        if (consecutive % 20 == 0 && player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SPLASH,
                player.getX(), player.getY() + 0.1, player.getZ(),
                3, 0.2, 0.0, 0.2, 0.05);
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 0.4F, 1.0F);
        }
        if (consecutive >= washTicksFor(player) && player.hasEffect(ModEffects.SWEATY_FEET)) {
            player.removeEffect(ModEffects.SWEATY_FEET);
            player.setData(ModAttachments.SWEAT_STATE, -1); // 洗清汗脚：持久化归零
            DEGRADE_TICKS.remove(id);
            WASH_TICKS.remove(id);
        }
    }

    /**
     * 泡脚倒计时 HUD（action bar）：清水="正在洗脚"，药水="正在清理真菌"。
     * 开始泡脚时立即调一次（首句不等 20 tick），之后 tickBasinSoak 每秒刷。
     */
    static void showBasinSoakHud(Player player) {
        BlockPos pos = BASIN_POS.get(player.getUUID());
        if (pos == null) {
            return;
        }
        BlockState state = player.level().getBlockState(pos);
        boolean medicinal = state.is(ModBlocks.WASH_BASIN.get())
            ? state.getValue(WashBasinBlock.FILLED) == WashBasinBlock.Filled.MEDICINAL
            : false;
        int t = BASIN_TICKS.getOrDefault(player.getUUID(), 0);
        int left = Math.max(0, (washTicksFor(player) - t) / 20);
        player.displayClientMessage(Component.translatable(
            medicinal ? "sweatyfeet.msg.cleaning_fungus" : "sweatyfeet.msg.washing", left), true);
    }

    /** 盆泡脚开始/继续：记录目标盆（累计计时不清零——离开暂停，回来右键接着泡） */
    static void startBasinSoak(Player player, BlockPos pos) {
        BASIN_POS.put(player.getUUID(), pos.immutable());
    }

    /**
     * 盆泡脚推进：右键盆/坐泡脚椅开始的会话，站在盆边（半径 1.5 格）或坐在座位上时累计计时；
     * 离开暂停（不清零）。坐着泡额外要求赤脚（穿鞋只是干坐着，不累计）。
     * - 清水：满 washTicksFor 洗完 → 清汗脚 + 盆变浑水
     * - 药水洗脚水：满 washTicksFor 洗完 → 清汗脚 + 清真菌（药水洗脚水唯一治真菌），盆变浑水
     * 水被舀走/盆被拆 → 会话终止。
     */
    private static void tickBasinSoak(Player player) {
        UUID id = player.getUUID();
        BlockPos pos = BASIN_POS.get(id);
        if (pos == null) {
            return;
        }
        Level level = player.level();
        BlockState basinState = level.getBlockState(pos);
        if (!basinState.is(ModBlocks.WASH_BASIN.get())) {
            BASIN_TICKS.remove(id);
            BASIN_POS.remove(id);
            return;
        }
        WashBasinBlock.Filled filled = basinState.getValue(WashBasinBlock.FILLED);
        if (filled != WashBasinBlock.Filled.WATER && filled != WashBasinBlock.Filled.MEDICINAL) {
            // 水被舀走/变浑 → 会话终止（药水洗完变浑也一样）
            BASIN_TICKS.remove(id);
            BASIN_POS.remove(id);
            return;
        }
        // 站盆边（独立盆或组合盆半，半径 1.5 格）或坐椅子上（座位离盆心 1 格，同范围）都算泡脚位置
        double dx = player.getX() - (pos.getX() + 0.5);
        double dz = player.getZ() - (pos.getZ() + 0.5);
        if (dx * dx + dz * dz > 2.25) {
            return; // 离开盆边：暂停计时
        }
        boolean wearingBoots = player.getItemBySlot(EquipmentSlot.FEET).is(ItemTags.FOOT_ARMOR);
        if (!player.hasEffect(ModEffects.SWEATY_FEET) && !player.hasEffect(ModEffects.FOOT_FUNGUS)) {
            return; // 脚干净：不累计（不然白水站/坐一会就浑了）
        }
        if (wearingBoots) {
            return; // 穿鞋：暂停计时且不刷倒计时（WashBasinBlock 会提示脱鞋）
        }
        int t = BASIN_TICKS.merge(id, 1, Integer::sum);
        if (t % 20 == 0 && level instanceof ServerLevel serverLevel) {
            // 泡脚表现：水花粒子 + 水声（每秒）
            serverLevel.sendParticles(ParticleTypes.SPLASH,
                pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5,
                3, 0.2, 0.0, 0.2, 0.05);
            serverLevel.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 0.4F, 1.0F);
            showBasinSoakHud(player);
        }
        if (t >= washTicksFor(player)) {
            // 洗完：清汗脚（药水还清真菌） + 盆变浑水
            player.removeEffect(ModEffects.SWEATY_FEET);
            player.setData(ModAttachments.SWEAT_STATE, -1); // 泡脚洗清：持久化归零
            if (filled == WashBasinBlock.Filled.MEDICINAL) {
                player.removeEffect(ModEffects.FOOT_FUNGUS);
                player.setData(ModAttachments.FUNGUS, false); // 药水洗脚水治好真菌：持久化
            }
            DEGRADE_TICKS.remove(id);
            WASH_TICKS.remove(id);
            level.setBlockAndUpdate(pos, basinState.setValue(WashBasinBlock.FILLED, WashBasinBlock.Filled.DIRTY));
            BASIN_TICKS.remove(id);
            BASIN_POS.remove(id);
            player.displayClientMessage(Component.translatable("sweatyfeet.msg.soak_done"), true);
        }
    }

    /** 起身（或被拆椅子弹下来）：座位实体清掉（tick 里无乘客自清理兜底），会话保留暂停 */
    @SubscribeEvent
    public static void onMountChange(net.neoforged.neoforge.event.entity.EntityMountEvent event) {
        if (event.isMounting() || !event.getLevel().isClientSide) {
            return;
        }
        if (event.getEntityBeingMounted() instanceof SeatEntity seat && !seat.isRemoved()) {
            seat.discard();
        }
    }

    /**
     * 脱鞋后的汗脚降级：每级倒计时 degrade_seconds，到头降一级重新计时，
     * 1 级到头则彻底消除。原版效果机制做不到（到头直接消失），这里手动管理。
     */
    private static void degradeSweatyFeet(Player player) {
        MobEffectInstance sf = player.getEffect(ModEffects.SWEATY_FEET);
        if (sf == null) {
            DEGRADE_TICKS.remove(player.getUUID());
            return;
        }
        UUID id = player.getUUID();
        int left = DEGRADE_TICKS.getOrDefault(id, degradeTicks());
        DegradeResult next = nextDegradeState(sf.getAmplifier(), left, degradeTicks());
        // 降级重挂：必须 remove 再 add —— 原版 MobEffectInstance.update 对"同 amp 更短时长/更低 amp"
        // 一律不覆盖，直接 addEffect(60s) 会被残留的 300s 汗脚挡住，降级永远不生效
        player.removeEffect(ModEffects.SWEATY_FEET);
        player.addEffect(sweatyFeetEffect(next.ticksLeft(), next.amplifier()));
        player.setData(ModAttachments.SWEAT_STATE, next.amplifier()); // 持久化降级后的等级
        DEGRADE_TICKS.put(id, next.ticksLeft());
    }

    /** 汗脚效果实例：无粒子但 HUD 图标要显示（5 参构造 showParticles=false 会把 showIcon 一起关掉，实测图标消失） */
    private static MobEffectInstance sweatyFeetEffect(int ticks, int amplifier) {
        return new MobEffectInstance(ModEffects.SWEATY_FEET, ticks, amplifier, false, false, true);
    }

    /**
     * 纯逻辑：降级状态机的单步推进。降级到 1 级（amp 0）后保留不再消除——
     * 汗脚只能靠洗脚（泡水/泡盆）彻底清除，脱鞋永远清不干净（文档语义）。
     */
    static DegradeResult nextDegradeState(int amplifier, int ticksLeft, int degradeTicks) {
        if (ticksLeft > 1) {
            return new DegradeResult(amplifier, ticksLeft - 1);
        }
        // 倒计时到头：降一级；1 级到底后重置为 1 级满时长（循环）
        if (amplifier <= 0) {
            return new DegradeResult(0, degradeTicks);
        }
        return new DegradeResult(amplifier - 1, degradeTicks);
    }

    /** 降级状态（amplifier = 下一个效果等级，-1 = 移除） */
    record DegradeResult(int amplifier, int ticksLeft) {
    }

    /**
     * 纯逻辑：按累计穿戴 tick 计算汗脚效果等级（amplifier）。
     * 与物品/事件解耦，供单元测试锁定等级推进边界（防回归）。
     */
    static int computeAmplifier(int totalTicks, int lvl2Ticks, int lvl3Ticks) {
        if (totalTicks >= lvl3Ticks) {
            return 2;
        }
        if (totalTicks >= lvl2Ticks) {
            return 1;
        }
        return 0;
    }

    /** 主手汗靴 + 副手玻璃瓶 + 潜行右键 → 倒汗，靴子还原，产出汗液瓶 */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (!player.isCrouching()) {
            return;
        }

        ItemStack main = player.getMainHandItem();
        if (!main.is(ItemTags.FOOT_ARMOR)) {
            return;
        }
        SweatData data = main.get(ModDataComponents.SWEAT.get());
        if (data == null) {
            return;
        }

        ItemStack offhand = player.getOffhandItem();
        if (!offhand.is(Items.GLASS_BOTTLE)) {
            return;
        }

        // 双端都取消：客户端若只 return，会继续 Item.use → ArmorItem.use → swapWithEquipmentSlot
        // 把主手汗靴【预测性】穿到脚上，产生"幽灵汗靴"，随后被服务端同步纠正消失
        event.setCanceled(true);
        if (player.level().isClientSide) {
            return;
        }

        // 以下仅服务端执行
        // 瓶等级 = 汗靴的等级（存在 SweatData 里：汗化时靴子等级固化，倒汗时按它产瓶）
        // 之前用 WEAR_TICKS 内存态算等级：计时随脱鞋清零，导致二级/三级汗脚只能倒出一级瓶
        int lvl = data.level() + 1;
        offhand.consume(1, player);
        if (lvl == 3 && main.is(ModItems.FERMENTED_BOOTS.get())) {
            // 发酵靴汗脚 3 级：汗液+糖发酵成熟 → 产"xxx的汗液饮品"（正面 buff）
            ItemStack drink = new ItemStack(ModItems.SWEAT_DRINK.get());
            drink.set(ModDataComponents.DRINK_TYPE.get(), "fermented");
            drink.set(DataComponents.CUSTOM_NAME, Component.translatable("item.sweatyfeet.sweat_drink.owned",
                player.getGameProfile().getName()));
            if (!player.getInventory().add(drink)) {
                player.drop(drink, false);
            }
        } else {
            ItemStack bottle = new ItemStack(ModItems.SWEAT_BOTTLE.get());
            bottle.set(ModDataComponents.SWEAT_LEVEL.get(), lvl);
            bottle.set(DataComponents.CUSTOM_NAME, Component.translatable("item.sweatyfeet.sweat_bottle.owned",
                player.getGameProfile().getName(), romanLevel(data.level())));
            if (!player.getInventory().add(bottle)) {
                player.drop(bottle, false);
            }
        }

        // 靴子还原：删组件 + 还原自定义名（无原名则回到默认显示名）
        main.remove(ModDataComponents.SWEAT.get());
        if (data.originalName() != null) {
            main.set(DataComponents.CUSTOM_NAME, data.originalName());
        } else {
            main.remove(DataComponents.CUSTOM_NAME);
        }

        // 倒汗 = 主动缓解：清零计时 + 移除脚部 debuff（真菌不可逆：倒汗清不掉，只能花露水泡脚）
        WEAR_TICKS.remove(player.getUUID());
        player.removeEffect(ModEffects.SWEATY_FEET);
        player.setData(ModAttachments.SWEAT_STATE, -1); // 倒汗清汗脚：持久化归零
    }

    /** 汗化：写组件（存原名）+ 改名「充满<玩家名>汗液的<原名>（等级X）」+ 挂汗脚 1 级 */
    private static void sweatify(Player player, ItemStack boots) {
        Component customName = boots.get(DataComponents.CUSTOM_NAME);
        SweatData data = new SweatData(0, customName);
        boots.set(ModDataComponents.SWEAT.get(), data);
        renameSweatyBoots(player, boots, data);
        player.addEffect(sweatyFeetEffect(effectTicks(), 0));
        player.setData(ModAttachments.SWEAT_STATE, 0); // 持久化汗脚 1 级
    }

    /** 汗靴改名：带玩家名 + 等级。基底名取"原始自定义名或物品默认名"——
     *  绝不能用 getHoverName（含当前自定义名），升级重命名会把已套的名字再套一层（叠名 bug） */
    private static void renameSweatyBoots(Player player, ItemStack boots, SweatData data) {
        Component baseName = data.originalName() != null
            ? data.originalName()
            : Component.translatable(boots.getDescriptionId());
        boots.set(DataComponents.CUSTOM_NAME, Component.translatable("item.sweatyfeet.sweaty_boots",
            player.getGameProfile().getName(), baseName, romanLevel(data.level())));
    }

    /** 等级罗马数字（0=I 1=II 2=III，对应汗脚/瓶 1/2/3 级） */
    private static String romanLevel(int level) {
        return switch (level) {
            case 0 -> "I";
            case 1 -> "II";
            default -> "III";
        };
    }

}
