package com.e33epus.sweatyfeet;

import java.util.HashMap;
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
     * 汗靴扔进水里泡洗：LevelTick 扫描水中 ItemEntity（汗靴），泡满 wash_boots_seconds
     * 还原为正常靴子（去汗液 + 还原名），期间冒水花粒子。
     */
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!SfConfig.WASH_BOOTS_ENABLED.get() || level.getGameTime() % 20 != 0) {
            return; // 每秒扫一次
        }
        int washBootsTicks = SfConfig.WASH_BOOTS_SECONDS.get() * 20;
        for (net.minecraft.world.entity.Entity e : level.getEntities().getAll()) {
            if (!(e instanceof ItemEntity itemEntity) || !itemEntity.isInWater()) {
                continue;
            }
            ItemStack stack = itemEntity.getItem();
            if (!stack.is(ItemTags.FOOT_ARMOR) || !stack.has(ModDataComponents.SWEAT.get())) {
                continue;
            }
            // 泡洗计时（组件累加），冒水花粒子
            int washed = stack.getOrDefault(ModDataComponents.SWEAT_WASH_TICKS.get(), 0) + 20;
            stack.set(ModDataComponents.SWEAT_WASH_TICKS.get(), washed);
            level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP,
                itemEntity.getX(), itemEntity.getY() + 0.2, itemEntity.getZ(),
                3, 0.15, 0.1, 0.15, 0.0);
            if (washed >= washBootsTicks) {
                // 洗干净：去汗液 + 还原自定义名
                SweatData data = stack.get(ModDataComponents.SWEAT.get());
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

    /** 洗脚 HUD 提示：穿鞋泡水提醒脱鞋；赤脚洗脚显示倒计时（action bar，物品栏正上方不重叠） */
    private static void showWashHud(Player player) {
        if (player.tickCount % 20 != 0) {
            return; // 每秒刷新
        }
        if (player.hasEffect(ModEffects.SWEATY_FEET) && player.isInWater()) {
            if (!player.getItemBySlot(EquipmentSlot.FEET).is(ItemTags.FOOT_ARMOR)) {
                // 赤脚洗脚中：显示剩余秒数
                int consecutive = WASH_TICKS.getOrDefault(player.getUUID(), 0);
                int left = Math.max(0, (washTicks() - consecutive) / 20);
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
                }
            }
        }

        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (!boots.is(ItemTags.FOOT_ARMOR)) {
            // 脱鞋：清穿戴计时，汗脚走"按级降级"（每级 60 秒递减，3→2→1→消除）
            WEAR_TICKS.remove(player.getUUID());
            degradeSweatyFeet(player);
            // 洗脚：赤脚泡水满 wash_seconds 清汗脚（跳过降级）；真菌泡水洗不掉
            handleWashOff(player);
            // 盆泡脚：赤脚右键开始的会话，站盆边累计计时
            tickBasinSoak(player);
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
                    player.addEffect(new MobEffectInstance(ModEffects.SWEATY_FEET, effectTicks(), leftover.getAmplifier(), false, true));
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
            }
            if (totalTicks % REFRESH_INTERVAL == 0) {
                refreshEffect(player, ModEffects.SWEATY_FEET, amplifier);
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
                other.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, sf.getAmplifier(), false, true));
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
        int consecutive = WASH_TICKS.merge(id, 1, Integer::sum);
        // 泡脚表现：水花粒子 + 水声（每秒）
        if (consecutive % 20 == 0 && player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SPLASH,
                player.getX(), player.getY() + 0.1, player.getZ(),
                3, 0.2, 0.0, 0.2, 0.05);
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 0.4F, 1.0F);
        }
        if (consecutive >= washTicks() && player.hasEffect(ModEffects.SWEATY_FEET)) {
            player.removeEffect(ModEffects.SWEATY_FEET);
            DEGRADE_TICKS.remove(id);
            WASH_TICKS.remove(id);
        }
    }

    /** 盆泡脚开始/继续：记录目标盆（累计计时不清零——离开暂停，回来右键接着泡） */
    static void startBasinSoak(Player player, BlockPos pos) {
        BASIN_POS.put(player.getUUID(), pos.immutable());
    }

    /**
     * 盆泡脚推进：赤脚右键盆开始的会话，站在盆边（半径 1.5 格，兼容以后坐旁边椅子）
     * 且盆里仍是清水时累计计时；离开暂停（不清零）；满 wash_seconds →
     * 清汗脚（跳过降级）+ 盆变浑水。水被舀走/盆被拆 → 会话终止。
     */
    private static void tickBasinSoak(Player player) {
        UUID id = player.getUUID();
        BlockPos pos = BASIN_POS.get(id);
        if (pos == null) {
            return;
        }
        Level level = player.level();
        BlockState basinState = level.getBlockState(pos);
        if (!basinState.is(ModBlocks.WASH_BASIN.get())
            || basinState.getValue(WashBasinBlock.FILLED) != WashBasinBlock.Filled.WATER) {
            BASIN_TICKS.remove(id);
            BASIN_POS.remove(id);
            return;
        }
        double dx = player.getX() - (pos.getX() + 0.5);
        double dz = player.getZ() - (pos.getZ() + 0.5);
        if (dx * dx + dz * dz > 2.25) {
            return; // 离开盆边：暂停计时
        }
        int t = BASIN_TICKS.merge(id, 1, Integer::sum);
        if (t % 20 == 0 && level instanceof ServerLevel serverLevel) {
            // 泡脚表现：水花粒子 + 水声 + 倒计时 HUD（每秒）
            serverLevel.sendParticles(ParticleTypes.SPLASH,
                pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5,
                3, 0.2, 0.0, 0.2, 0.05);
            serverLevel.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 0.4F, 1.0F);
            int left = Math.max(0, (washTicks() - t) / 20);
            player.displayClientMessage(Component.translatable("sweatyfeet.msg.washing", left), true);
        }
        if (t >= washTicks()) {
            // 洗完：清汗脚 + 盆变浑水
            player.removeEffect(ModEffects.SWEATY_FEET);
            DEGRADE_TICKS.remove(id);
            WASH_TICKS.remove(id);
            level.setBlockAndUpdate(pos, basinState.setValue(WashBasinBlock.FILLED, WashBasinBlock.Filled.DIRTY));
            BASIN_TICKS.remove(id);
            BASIN_POS.remove(id);
            player.displayClientMessage(Component.translatable("sweatyfeet.msg.soak_done"), true);
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
        if (next.amplifier() < 0) {
            player.removeEffect(ModEffects.SWEATY_FEET);
            DEGRADE_TICKS.remove(id);
        } else {
            // 降级重挂：必须 remove 再 add —— 原版 MobEffectInstance.update 对"同 amp 更短时长/更低 amp"
            // 一律不覆盖，直接 addEffect(60s) 会被残留的 300s 汗脚挡住，降级永远不生效
            player.removeEffect(ModEffects.SWEATY_FEET);
            player.addEffect(new MobEffectInstance(ModEffects.SWEATY_FEET, next.ticksLeft(), next.amplifier(), false, true));
            DEGRADE_TICKS.put(id, next.ticksLeft());
        }
    }

    /**
     * 纯逻辑：降级状态机的单步推进。amplifier=-1 表示已到 1 级末尾，应移除效果。
     * 供单元测试锁定"3→2→1→消除"的降级链路。
     */
    static DegradeResult nextDegradeState(int amplifier, int ticksLeft, int degradeTicks) {
        if (ticksLeft > 1) {
            return new DegradeResult(amplifier, ticksLeft - 1);
        }
        int nextAmp = amplifier - 1;
        return nextAmp < 0 ? new DegradeResult(-1, 0) : new DegradeResult(nextAmp, degradeTicks);
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
        ItemStack bottle = new ItemStack(ModItems.SWEAT_BOTTLE.get());
        bottle.set(ModDataComponents.SWEAT_LEVEL.get(), lvl);
        if (!player.getInventory().add(bottle)) {
            player.drop(bottle, false);
        }

        // 靴子还原：删组件 + 还原自定义名（无原名则回到默认显示名）
        main.remove(ModDataComponents.SWEAT.get());
        if (data.originalName() != null) {
            main.set(DataComponents.CUSTOM_NAME, data.originalName());
        } else {
            main.remove(DataComponents.CUSTOM_NAME);
        }

        // 倒汗 = 主动缓解：清零计时 + 移除脚部 debuff
        WEAR_TICKS.remove(player.getUUID());
        player.removeEffect(ModEffects.SWEATY_FEET);
        player.removeEffect(ModEffects.FOOT_FUNGUS);
    }

    /** 汗化：写组件（存原名）+ 改名「充满汗液的xxx」+ 挂汗脚 1 级 */
    private static void sweatify(Player player, ItemStack boots) {
        Component customName = boots.get(DataComponents.CUSTOM_NAME);
        Component displayName = customName != null ? customName : boots.getHoverName();
        boots.set(ModDataComponents.SWEAT.get(), new SweatData(0, customName));
        boots.set(DataComponents.CUSTOM_NAME, Component.translatable("item.sweatyfeet.sweaty_boots", displayName));
        player.addEffect(new MobEffectInstance(ModEffects.SWEATY_FEET, effectTicks(), 0, false, true));
    }

    private static void refreshEffect(Player player, Holder<MobEffect> effect, int amplifier) {
        player.addEffect(new MobEffectInstance(effect, effectTicks(), amplifier, false, true));
    }
}
