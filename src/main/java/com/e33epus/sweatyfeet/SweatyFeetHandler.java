package com.e33epus.sweatyfeet;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
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

    private static int degradeTicks() {
        return SfConfig.INSTANCE.degrade_seconds * 20;
    }

    private static int washTicks() {
        return SfConfig.INSTANCE.wash_seconds * 20;
    }

    private static int lvl1() {
        return SfConfig.INSTANCE.level1_seconds * 20;
    }

    private static int lvl2() {
        return SfConfig.INSTANCE.level2_seconds * 20;
    }

    private static int lvl3() {
        return SfConfig.INSTANCE.level3_seconds * 20;
    }

    private static int fungusDelay() {
        return SfConfig.INSTANCE.fungus_delay_seconds * 20;
    }

    private static int fungusTicks() {
        return SfConfig.INSTANCE.fungus_seconds * 20;
    }

    private static int effectTicks() {
        return SfConfig.INSTANCE.effect_seconds * 20;
    }

    private SweatyFeetHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        // 脚滑：双端都要跑（客户端保手感，服务端保权威，衰减一致无纠偏）
        applySlide(player);

        if (player.level().isClientSide) {
            return;
        }

        // 真菌传染扩散：不依赖靴子，被传染者也能继续传染（站在感染者附近一段时间被传）
        if (SfConfig.INSTANCE.fungus_infection_enabled
            && player.hasEffect(ModEffects.FOOT_FUNGUS)
            && player.tickCount % (SfConfig.INSTANCE.fungus_infection_interval_seconds * 20) == 0) {
            double rangeSq = (double) SfConfig.INSTANCE.fungus_infection_range * SfConfig.INSTANCE.fungus_infection_range;
            for (Player other : player.level().players()) {
                if (other == player || other.hasEffect(ModEffects.FOOT_FUNGUS)) {
                    continue;
                }
                if (other.distanceToSqr(player) <= rangeSq) {
                    other.addEffect(new MobEffectInstance(ModEffects.FOOT_FUNGUS, fungusTicks(), 0, false, true));
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
            // 3 级后继续穿 30 秒 → 真菌感染；穿着期间持续刷新，不脱靴不清
            if (SfConfig.INSTANCE.enable_fungus
                && totalTicks >= lvl3() + fungusDelay()
                && totalTicks % REFRESH_INTERVAL == 0) {
                player.addEffect(new MobEffectInstance(ModEffects.FOOT_FUNGUS, fungusTicks(), 0, false, true));
            }
        }

        // Debug：action bar 实时显示穿戴 tick 与汗脚等级（肉眼验证计时）
        if (SfConfig.INSTANCE.debug_show_ticks && totalTicks % 20 == 0) {
            int lvl = data == null ? -1 : computeAmplifier(totalTicks, lvl2(), lvl3()) + 1;
            player.displayClientMessage(Component.literal("SF tick=" + totalTicks + " lvl=" + lvl), true);
        }
        // Debug：强制真菌（方便测真菌表现，不看 3 级时长）
        if (SfConfig.INSTANCE.debug_force_fungus && totalTicks % 20 == 0) {
            player.addEffect(new MobEffectInstance(ModEffects.FOOT_FUNGUS, fungusTicks(), 0, false, true));
        }
    }

    /**
     * 脚滑（汗脚 2 级起）：不依赖按键检测（服务端拿不到 input），
     * 直接对水平动量乘保留系数 → 松键后速度衰减变慢 = 滑行。
     * 双端对称执行，服务端权威 + 客户端预测一致。
     */
    private static void applySlide(Player player) {
        if (!SfConfig.INSTANCE.slide_enabled) {
            return;
        }
        MobEffectInstance sf = player.getEffect(ModEffects.SWEATY_FEET);
        if (sf == null || sf.getAmplifier() < 1) {
            return;
        }
        if (!player.onGround()) {
            return;
        }
        double retention = SfConfig.INSTANCE.slide_retention_percent / 100.0;
        Vec3 d = player.getDeltaMovement();
        player.setDeltaMovement(d.x * retention, d.y, d.z * retention);
    }

    /**
     * 洗脚：赤脚泡水连续满 wash_seconds 清汗脚（跳过降级直接消除）。
     * 只清汗脚不清真菌——真菌只能用花露水治（泡水洗不掉）。
     * 连续计时：断水清零，防"沾一下水就算洗"。
     */
    private static void handleWashOff(Player player) {
        if (!player.isInWater()) {
            WASH_TICKS.remove(player.getUUID());
            return;
        }
        UUID id = player.getUUID();
        int consecutive = WASH_TICKS.merge(id, 1, Integer::sum);
        if (consecutive >= washTicks() && player.hasEffect(ModEffects.SWEATY_FEET)) {
            player.removeEffect(ModEffects.SWEATY_FEET);
            DEGRADE_TICKS.remove(id);
            WASH_TICKS.remove(id);
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
            // 降级重挂：效果时长与倒计时同步（一个阶段），保证状态机与效果一致
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
