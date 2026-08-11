package com.e33epus.sweatyfeet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.minecraft.block.BlockState;

/**
 * 核心玩法：穿戴计时 → 汗化 → 等级推进 → 真菌感染；脱鞋清零；潜行右键倒汗。
 * 计时放服务端内存（脱鞋/下线即清），汗化状态放靴子的 DataComponent。
 */
public final class SweatyFeetHandler {
    public static final int REFRESH_INTERVAL = 20;         // 每 20 tick 刷新一次 debuff

    /** debug 日志：state_log 打状态、flow_log 打判定（都走 [SF] 前缀，latest.log 可 grep） */
    static void debugLog(boolean flag, String tag, String msg) {
        if (flag) {
            com.mojang.logging.LogUtils.getLogger().info("[SF] [" + tag + "] " + msg);
        }
    }

    /** 状态日志（debug_state_log）：每秒一条，覆盖穿戴/降级/泡水/盆泡/真菌/手持全状态 */
    private static void logDebugState(PlayerEntity player, Integer wearTicks, int degradeS, int washS) {
        if (!SfConfig.DEBUG_STATE_LOG) {
            return;
        }
        int basinS = BASIN_TICKS.getOrDefault(player.getUuid(), 0) / 20;
        debugLog(true, "state", player.getGameProfile().getName()
            + " wear=" + (wearTicks == null ? "off" : wearTicks)
            + " lvl=" + (player.hasStatusEffect(ModEffects.SWEATY_FEET)
                ? player.getStatusEffect(ModEffects.SWEATY_FEET).getAmplifier() + 1 : 0)
            + " degrade_s=" + degradeS + " wash_s=" + washS + " basin_s=" + basinS
            + " sweat=" + (player.hasStatusEffect(ModEffects.SWEATY_FEET) ? "Y" : "N")
            + " fungus=" + (player.hasStatusEffect(ModEffects.FOOT_FUNGUS) ? "Y" : "N")
            + " heldL3=" + (HELD_L3.contains(player.getUuid()) ? "Y" : "N"));
    }

    /** 玩家总穿戴 tick（服务端内存态，脱鞋/下线即清，不跨会话） */
    private static final Map<UUID, Integer> WEAR_TICKS = new HashMap<>();

    /** 脱鞋后降级倒计时（玩家 → 当前等级剩余 tick）；穿鞋即清，暂停降级 */
    private static final Map<UUID, Integer> DEGRADE_TICKS = new HashMap<>();

    /** 赤脚连续泡水 tick（玩家 → 连续 tick）；断水/穿鞋清零，满 wash_seconds 清汗脚 */
    private static final Map<UUID, Integer> WASH_TICKS = new HashMap<>();

    /** 上一 tick 手持三级汗靴的玩家（过渡检测用：放下瞬间清反胃，不伤其他来源） */
    private static final Set<UUID> HELD_L3 = new HashSet<>();

    /** 已授予"生化武器"的汗靴实体（丢地污染臭到人只授予一次，防每 tick 重复） */
    private static final Set<UUID> BIO_WEAPON_AWARDED = new HashSet<>();

    /** 盆泡脚：累计计时（玩家 → 累计 tick）；离开暂停不清零，满 wash_seconds 洗完 + 盆变浑 */
    private static final Map<UUID, Integer> BASIN_TICKS = new HashMap<>();

    /** 盆泡脚：当前泡的目标盆（玩家 → 盆坐标）；空手右键盆（赤脚+有水+有汗脚）时记录 */
    private static final Map<UUID, BlockPos> BASIN_POS = new HashMap<>();

    private static int degradeTicks() {
        return SfConfig.DEGRADE_SECONDS * 20;
    }

    private static int washTicks() {
        return SfConfig.WASH_SECONDS * 20;
    }

    /** 授予自定义进度 criterion（impossible 触发器 + 代码手动 award；进度未加载/离线安全跳过） */
    private static void awardCriterion(ServerPlayerEntity sp, String advancementPath,
                                       String criterion) {
        net.minecraft.advancement.AdvancementEntry holder = sp.server.getAdvancementLoader()
            .get(Identifier.of(SweatyFeet.MOD_ID, advancementPath));
        if (holder != null) {
            sp.getAdvancementTracker().grantCriterion(holder, criterion);
            debugLog(SfConfig.DEBUG_FLOW_LOG, "flow",
                sp.getGameProfile().getName() + " advancement awarded: " + advancementPath + "/" + criterion);
        }
    }

    /** 洗脚所需 tick（随汗脚等级递增）：1 级 T_base，2 级 2×，3 级 3× */
    private static int washTicksFor(PlayerEntity player) {
        StatusEffectInstance sf = player.getStatusEffect(ModEffects.SWEATY_FEET);
        int amp = sf == null ? 0 : sf.getAmplifier();
        return washTicks() * (amp + 1);
    }

    private static int lvl1() {
        return SfConfig.LEVEL_1_SECONDS * 20;
    }

    private static int lvl2() {
        return SfConfig.LEVEL_2_SECONDS * 20;
    }

    private static int lvl3() {
        return SfConfig.LEVEL_3_SECONDS * 20;
    }

    private static int fungusDelay() {
        return SfConfig.FUNGUS_DELAY_SECONDS * 20;
    }

    private static int effectTicks() {
        return SfConfig.EFFECT_SECONDS * 20;
    }

    private SweatyFeetHandler() {
    }

    /** Fabric 事件注册（双端都调）：tick/连接/骑乘/右键全部挂这里 */
    public static void init() {
        // 每秒世界 tick + 每玩家 tick（Fabric 无 per-player tick 事件 → END_SERVER_TICK 遍历）
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerWorld world : server.getWorlds()) {
                handleLevelTick(world);
            }
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                handlePlayerTick(p);
            }
        });

        // 登录发手册 / 下线清内存态（Map 泄漏防护）
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            PatchouliBookGiver.tryGiveBook(handler.player);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID id = handler.player.getUuid();
            WEAR_TICKS.remove(id);
            DEGRADE_TICKS.remove(id);
            WASH_TICKS.remove(id);
            BASIN_TICKS.remove(id);
            BASIN_POS.remove(id);
            HELD_L3.remove(id);
        });

        // 右键物品（空气/未命中方块）与右键方块（盆/凳等）双路径拦截倒汗
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            return tryPourSweat(player) ? TypedActionResult.fail(stack) : TypedActionResult.pass(stack);
        });
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            return tryPourSweat(player) ? ActionResult.FAIL : ActionResult.PASS;
        });
    }

    /**
     * 每 tick 级联扫描（每秒一次）：
     * 1) 汗靴扔水里泡洗（受 WASH_BOOTS_ENABLED 配置）
     * 2) 三级汗靴丢在地上散发绿臭味粒子 + 附近玩家（含本人）反胃（环境污染）
     */
    public static void handleLevelTick(ServerWorld level) {
        if (level.getTime() % 20 != 0) {
            return; // 每秒扫一次
        }
        // 单次全扫（泡洗 + 丢地污染合并，避免两次遍历全维度实体）：
        // 1) 汗靴扔水里泡洗（受 WASH_BOOTS_ENABLED 配置）
        // 2) 二级及以上汗靴丢地污染：绿粒子 + 附近玩家（含丢者本人）反胃 + 生化武器进度
        double smellRangeSq = (double) SfConfig.SMELL_RANGE * SfConfig.SMELL_RANGE;
        boolean washEnabled = SfConfig.WASH_BOOTS_ENABLED;
        int washBootsTicks = SfConfig.WASH_BOOTS_SECONDS * 20;
        // 单次全扫（泡洗 + 丢地污染合并）：用 EntityType.ITEM + Box.INFINITE 走
        // section 类型索引（只遍历物品实体），不再 getAll() 全维度扫所有实体类型
        net.minecraft.util.math.Box infinite = new net.minecraft.util.math.Box(
            Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,
            Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
        for (net.minecraft.entity.Entity e :
                level.getEntitiesByType(net.minecraft.entity.EntityType.ITEM, infinite, ignored -> true)) {
            if (!(e instanceof ItemEntity itemEntity)) {
                continue;
            }
            ItemStack stack = itemEntity.getStack();
            // 泡洗：带汗液组件的靴子泡水里累计计时还原
            if (washEnabled && itemEntity.isTouchingWater()
                && stack.isIn(ItemTags.FOOT_ARMOR) && stack.contains(ModDataComponents.SWEAT)) {
                SweatData data = stack.get(ModDataComponents.SWEAT);
                int need = washBootsTicks * (data.level() + 1);
                int washed = stack.getOrDefault(ModDataComponents.SWEAT_WASH_TICKS, 0) + 20;
                stack.set(ModDataComponents.SWEAT_WASH_TICKS, washed);
                level.spawnParticles(ParticleTypes.BUBBLE_COLUMN_UP,
                    itemEntity.getX(), itemEntity.getY() + 0.2, itemEntity.getZ(),
                    3, 0.15, 0.1, 0.15, 0.0);
                if (washed >= need) {
                    // 洗干净：去汗液 + 还原自定义名
                    stack.remove(ModDataComponents.SWEAT);
                    stack.remove(ModDataComponents.SWEAT_WASH_TICKS);
                    if (data != null && data.originalName() != null) {
                        stack.set(DataComponentTypes.CUSTOM_NAME, data.originalName());
                    } else {
                        stack.remove(DataComponentTypes.CUSTOM_NAME);
                    }
                }
            }
            // 丢地污染：二级及以上汗靴（用户定案二级触发）；臭气云 = 绿粒子团 + 菌丝孢子缓慢上飘
            SweatData data = stack.get(ModDataComponents.SWEAT);
            if (data == null || data.level() < 2) {
                continue;
            }
            level.spawnParticles(ParticleTypes.COMPOSTER,
                itemEntity.getX(), itemEntity.getY() + 0.2, itemEntity.getZ(),
                6, 0.6, 0.3, 0.6, 0.0);
            level.spawnParticles(ParticleTypes.MYCELIUM,
                itemEntity.getX(), itemEntity.getY() + 0.3, itemEntity.getZ(),
                2, 0.3, 0.2, 0.3, 0.02);
            giveNearbyNausea(level, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(),
                smellRangeSq, null, true);
            // 生化武器：这双汗靴第一次臭到"非丢者"的其他玩家 → 授予丢者（离线不补发）
            net.minecraft.entity.Entity ownerEntity = itemEntity.getOwner();
            UUID ownerId = ownerEntity != null ? ownerEntity.getUuid() : null;
            if (ownerId != null && !BIO_WEAPON_AWARDED.contains(itemEntity.getUuid())) {
                for (PlayerEntity other : level.getPlayers()) {
                    if (other.getUuid().equals(ownerId) || !other.hasStatusEffect(StatusEffects.NAUSEA)) {
                        continue;
                    }
                    if (other.squaredDistanceTo(itemEntity.getX(), itemEntity.getY(), itemEntity.getZ()) <= smellRangeSq) {
                        BIO_WEAPON_AWARDED.add(itemEntity.getUuid());
                        net.minecraft.server.network.ServerPlayerEntity owner =
                            level.getServer().getPlayerManager().getPlayer(ownerId);
                        if (owner != null) {
                            awardCriterion(owner, "bio_weapon", "bio_weapon");
                        }
                        break;
                    }
                }
            }
        }
    }

    /** 给范围内玩家反胃（3 秒）；self 为 null 时包含所有玩家，否则排除 self */
    private static void giveNearbyNausea(ServerWorld level, double x, double y, double z,
                                         double rangeSq, PlayerEntity self, boolean includeSelf) {
        for (PlayerEntity other : level.getPlayers()) {
            if (!includeSelf && other == self) {
                continue;
            }
            if (other.hasStatusEffect(StatusEffects.NAUSEA)) {
                continue;
            }
            if (other.squaredDistanceTo(x, y, z) <= rangeSq) {
                other.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 200, 0, false, true));
            }
        }
    }

    /** 洗脚 HUD 提示：穿鞋泡水提醒脱鞋；赤脚洗脚显示倒计时（action bar，物品栏正上方不重叠） */
    private static void showWashHud(PlayerEntity player) {
        if (player.age % 20 != 0) {
            return; // 每秒刷新
        }
        if (player.hasStatusEffect(ModEffects.SWEATY_FEET) && player.isTouchingWater()) {
            if (!player.getEquippedStack(EquipmentSlot.FEET).isIn(ItemTags.FOOT_ARMOR)) {
                // 赤脚洗脚中：显示剩余秒数（按当前等级需求）
                int consecutive = WASH_TICKS.getOrDefault(player.getUuid(), 0);
                int left = Math.max(0, (washTicksFor(player) - consecutive) / 20);
                player.sendMessage(
                    Text.translatable("sweatyfeet.msg.washing", left), true);
            } else {
                // 穿鞋泡水：提醒脱鞋
                player.sendMessage(
                    Text.translatable("sweatyfeet.msg.take_off"), true);
            }
        }
    }

    public static void handlePlayerTick(PlayerEntity player) {
        if (player.getWorld().isClient) {
            return;
        }

        // 手持二级及以上汗靴：持续反胃（文档：放下即消）。注意不能每 tick 无脑 remove——
        // 那会把二级汗液瓶等其他来源的反胃也秒删。用过渡检测：只在"刚放下"那一 tick 清一次
        ItemStack held = player.getMainHandStack();
        if (!held.isIn(ItemTags.FOOT_ARMOR)) {
            held = player.getOffHandStack();
        }
        SweatData heldData = held.get(ModDataComponents.SWEAT);
        UUID heldId = player.getUuid();
        if (heldData != null && heldData.level() >= 2) {
            // 首次手持二级+汗靴 → 授予"入味了"（HELD_L3.add 返回 true = 第一次进入）
            if (HELD_L3.add(heldId) && player instanceof net.minecraft.server.network.ServerPlayerEntity sp) {
                awardCriterion(sp, "marinated", "marinated");
            }
            // 60 tick、剩余低于 40 才补：每次补都比剩余长 → 触发客户端同步包，眩晕持续。
            // 之前每 tick 刷同值 40：服务端无变化不发同步，客户端 2 秒到期 → 看不到屏幕扭曲
            StatusEffectInstance cur = player.getStatusEffect(StatusEffects.NAUSEA);
            if (cur == null || cur.getDuration() < 100) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 120, 1, false, true));
            }
        } else if (HELD_L3.remove(heldId) && player.hasStatusEffect(StatusEffects.NAUSEA)) {
            player.removeStatusEffect(StatusEffects.NAUSEA);
        }

        // 真菌感染者：赤脚才散发绿粒子 + 附近玩家反胃（穿鞋防臭——真菌散臭不看穿鞋是"穿鞋还臭"根因）
        if (player.hasStatusEffect(ModEffects.FOOT_FUNGUS) && player.age % 20 == 0
            && !player.getEquippedStack(EquipmentSlot.FEET).isIn(ItemTags.FOOT_ARMOR)
            && player.getWorld() instanceof ServerWorld serverLevel) {
            // 臭气云：绿粒子团 + 菌丝孢子上飘（比旧版 2 个粒子更像一团气）
            serverLevel.spawnParticles(ParticleTypes.COMPOSTER,
                player.getX(), player.getY() + 0.1, player.getZ(),
                6, 0.6, 0.2, 0.6, 0.0);
            serverLevel.spawnParticles(ParticleTypes.MYCELIUM,
                player.getX(), player.getY() + 0.2, player.getZ(),
                2, 0.3, 0.2, 0.3, 0.02);
            double smellRangeSq = (double) SfConfig.SMELL_RANGE * SfConfig.SMELL_RANGE;
            giveNearbyNausea(serverLevel, player.getX(), player.getY(), player.getZ(),
                smellRangeSq, player, false);
        }

        // 持久化恢复：下线重进后按附件把汗脚/真菌重新挂回（每 20 tick 检查一次，避免每 tick 写）
        if (player.age % 20 == 0) {
            int savedAmp = ((SweatyDataHolder) player).sweatState();
            if (savedAmp >= 0 && !player.hasStatusEffect(ModEffects.SWEATY_FEET)) {
                player.removeStatusEffect(ModEffects.SWEATY_FEET);
                player.addStatusEffect(sweatyFeetEffect(effectTicks(), savedAmp));
            }
            if (((SweatyDataHolder) player).hasFungus() && !player.hasStatusEffect(ModEffects.FOOT_FUNGUS)) {
                player.addStatusEffect(new StatusEffectInstance(ModEffects.FOOT_FUNGUS, StatusEffectInstance.INFINITE, 0, false, true));
            }
        }

        // 真菌传染扩散：不依赖靴子，被传染者也能继续传染（站在感染者附近一段时间被传）
        if (SfConfig.FUNGUS_INFECTION_ENABLED
            && player.hasStatusEffect(ModEffects.FOOT_FUNGUS)
            && player.age % (SfConfig.FUNGUS_INFECTION_INTERVAL_SECONDS * 20) == 0) {
            double rangeSq = (double) SfConfig.FUNGUS_INFECTION_RANGE * SfConfig.FUNGUS_INFECTION_RANGE;
            for (PlayerEntity other : player.getWorld().getPlayers()) {
                if (other == player || other.hasStatusEffect(ModEffects.FOOT_FUNGUS)) {
                    continue;
                }
                if (other.squaredDistanceTo(player) <= rangeSq) {
                    // 传染也给无限时长：真菌只能被花露水/倒汗消除，不会自然消失
                    other.addStatusEffect(new StatusEffectInstance(ModEffects.FOOT_FUNGUS, StatusEffectInstance.INFINITE, 0, false, true));
                    ((SweatyDataHolder) other).setFungus(true); // 持久化传染
                    debugLog(SfConfig.DEBUG_FLOW_LOG, "flow",
                        "fungus spread: " + player.getGameProfile().getName() + " -> " + other.getGameProfile().getName());
                    // 瘟疫公司：把真菌传给别的玩家 → 授予传染源（award 幂等，传多人只授予一次）
                    if (player instanceof net.minecraft.server.network.ServerPlayerEntity sp) {
                        awardCriterion(sp, "plague_inc", "plague_inc");
                    }
                }
            }
        }

        // 盆泡脚会话（v2：坐凳+脱鞋+右键盆开始；这里推进累计计时）
        tickBasinSoak(player);

        ItemStack boots = player.getEquippedStack(EquipmentSlot.FEET);
        if (!boots.isIn(ItemTags.FOOT_ARMOR)) {
            // 脱鞋：清穿戴计时，汗脚走"按级降级"（每级 60 秒递减，3→2→1→消除）
            WEAR_TICKS.remove(player.getUuid());
            degradeSweatyFeet(player);
            // 洗脚：赤脚泡水满 wash_seconds 清汗脚（跳过降级）；真菌泡水洗不掉
            handleWashOff(player);
            // 洗脚 HUD：穿鞋泡水提醒脱鞋 / 赤脚泡水倒计时
            showWashHud(player);
            // 散臭：赤脚 + 有汗脚 → 附近玩家持续反胃（穿鞋防臭，洗脚/降级完不臭）
            spreadFootSmell(player);
            // Debug 状态日志（脱鞋分支：wear 已清，看降级/泡水进度）
            if (player.age % 20 == 0) {
                logDebugState(player, null,
                    DEGRADE_TICKS.getOrDefault(player.getUuid(), 0) / 20,
                    WASH_TICKS.getOrDefault(player.getUuid(), 0) / 20);
            }
            return;
        }

        // 穿鞋（含汗靴）：降级/洗脚暂停，回到冻结/重新计时
        DEGRADE_TICKS.remove(player.getUuid());
        WASH_TICKS.remove(player.getUuid());

        int totalTicks = WEAR_TICKS.merge(player.getUuid(),
            player.getWorld().getRegistryKey() == World.NETHER ? 2 : 1, Integer::sum); // 下界炎热，汗化速度 ×2（整蛊彩蛋）
        SweatData data = boots.get(ModDataComponents.SWEAT);

        // 淌汗脚印（表现）：汗脚 2/3 级走动时脚下滴汗珠——靴子里泡满了，一路走一路淌
        StatusEffectInstance sweatFx = player.getStatusEffect(ModEffects.SWEATY_FEET);
        if (sweatFx != null && sweatFx.getAmplifier() >= 1
            && player.age % 5 == 0 && player.isOnGround()
            && player.getVelocity().horizontalLengthSquared() > 1.0E-6
            && player.getWorld() instanceof ServerWorld sweatLevel) {
            int drops = sweatFx.getAmplifier() >= 2 ? 2 : 1;
            sweatLevel.spawnParticles(ParticleTypes.SPLASH,
                player.getX(), player.getY() + 0.05, player.getZ(),
                drops, 0.15, 0.0, 0.15, 0.0);
        }

        if (data == null) {
            // 未汗化：等汗化触发；同时冻结残余汗脚效果（脱鞋残余的效果，再穿时暂停倒计时，
            // 穿鞋期间效果不计时，只有脱鞋才走完——用户语义"穿鞋冻结/脱鞋倒计时"）
            if (totalTicks % REFRESH_INTERVAL == 0) {
                StatusEffectInstance leftover = player.getStatusEffect(ModEffects.SWEATY_FEET);
                if (leftover != null) {
                    player.addStatusEffect(sweatyFeetEffect(effectTicks(), leftover.getAmplifier()));
                }
            }
            if (totalTicks >= lvl1()) {
                sweatify(player, boots);
            }
            // Debug：强制 1 级汗化（跳过 30 秒计时）
            if (SfConfig.DEBUG_FORCE_SWEAT && totalTicks % REFRESH_INTERVAL == 0) {
                sweatify(player, boots);
            }
        } else {
            // 已汗化：汗靴等级固化在组件里，只升不降（脱鞋再穿汗靴等级保留）
            // 汗脚效果等级 = 组件等级与当前穿戴进度取最大：汗靴再穿立即恢复组件等级的效果
            int amplifier = Math.max(computeAmplifier(totalTicks, lvl2(), lvl3()), data.level());
            // Debug：强制 3 级（跳过 60/90 秒等待，测发酵靴倒汗/饮品）
            if (SfConfig.DEBUG_FORCE_LEVEL3) {
                amplifier = Math.max(2, data.level());
            }
            if (amplifier > data.level() && totalTicks % REFRESH_INTERVAL == 0) {
                boots.set(ModDataComponents.SWEAT, data.withLevel(amplifier));
                data = boots.get(ModDataComponents.SWEAT);
                renameSweatyBoots(player, boots, data); // 升级同步改名（等级II/III）
            }
            if (totalTicks % REFRESH_INTERVAL == 0) {
                player.addStatusEffect(sweatyFeetEffect(effectTicks(), amplifier));
                ((SweatyDataHolder) player).setSweatState(amplifier); // 持久化当前等级
                // 3 级：额外减速（汗脚3级 = 脚滑 + 减速）
                if (amplifier >= 2) {
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, effectTicks(), 0, false, true));
                }
            }
            // 3 级后继续穿 30 秒 → 真菌感染（无限时长：只能被花露水/倒汗消除，脱鞋不消失）
            if (SfConfig.ENABLE_FUNGUS
                && totalTicks >= lvl3() + fungusDelay()
                && !player.hasStatusEffect(ModEffects.FOOT_FUNGUS)
                && totalTicks % REFRESH_INTERVAL == 0) {
                player.addStatusEffect(new StatusEffectInstance(ModEffects.FOOT_FUNGUS, StatusEffectInstance.INFINITE, 0, false, true));
                ((SweatyDataHolder) player).setFungus(true); // 持久化真菌
                // 触发后穿戴计时归零：真菌治好后再穿需重新积累 3 级 + 延迟窗口
                // （totalTicks 是累计总时长，不归零则 totalTicks 早已超阈值 → 治好即秒感染）
                WEAR_TICKS.put(player.getUuid(), 0);
            }
        }

        // Debug：action bar 实时显示穿戴 tick 与汗脚等级（肉眼验证计时）
        if (SfConfig.DEBUG_SHOW_TICKS && totalTicks % 20 == 0) {
            int lvl = data == null ? -1 : computeAmplifier(totalTicks, lvl2(), lvl3()) + 1;
            player.sendMessage(Text.literal("SF tick=" + totalTicks + " lvl=" + lvl), true);
        }
        // Debug：强制真菌（方便测真菌表现，不看 3 级时长）
        if (SfConfig.DEBUG_FORCE_FUNGUS && totalTicks % 20 == 0 && !player.hasStatusEffect(ModEffects.FOOT_FUNGUS)) {
            player.addStatusEffect(new StatusEffectInstance(ModEffects.FOOT_FUNGUS, StatusEffectInstance.INFINITE, 0, false, true));
            debugLog(SfConfig.DEBUG_FLOW_LOG, "flow", player.getGameProfile().getName() + " forced fungus");
        }
        // Debug 状态日志（穿鞋分支：wear 推进中）
        if (totalTicks % 20 == 0) {
            logDebugState(player, totalTicks, 0, 0);
        }
    }

    /**
     * 散臭：赤脚 + 身上有汗脚效果（降级中）→ 附近玩家持续反胃。
     * 自己不受影响（自己闻不到自己脚臭）；汗脚等级越高反胃越强。
     * 穿鞋/泡水洗脚/降级完成后效果消失 → 停止散臭。
     */
    private static void spreadFootSmell(PlayerEntity player) {
        if (!SfConfig.SMELL_ENABLED) {
            return;
        }
        StatusEffectInstance sf = player.getStatusEffect(ModEffects.SWEATY_FEET);
        if (sf == null) {
            return;
        }
        if (player.age % 20 != 0) {
            return; // 每秒刷新一次
        }
        double rangeSq = (double) SfConfig.SMELL_RANGE * SfConfig.SMELL_RANGE;
        for (PlayerEntity other : player.getWorld().getPlayers()) {
            if (other == player || other.hasStatusEffect(StatusEffects.NAUSEA)) {
                continue;
            }
            if (other.squaredDistanceTo(player) <= rangeSq) {
                // 反胃 3 秒，amplifier = 汗脚等级（越臭反胃越强）
                other.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 200, sf.getAmplifier(), false, true));
                debugLog(SfConfig.DEBUG_FLOW_LOG, "flow",
                    "smell: " + player.getGameProfile().getName() + " -> " + other.getGameProfile().getName()
                    + " amp=" + sf.getAmplifier());
            }
        }
    }

    /**
     * 洗脚：赤脚泡水或站洗脚盆上，连续满 wash_seconds 清汗脚（跳过降级直接消除）。
     * 只清汗脚不清真菌——真菌只能用花露水治（泡水/泡盆洗不掉）。
     * 连续计时：断水/离开盆清零，防"沾一下水就算洗"。
     */
    private static void handleWashOff(PlayerEntity player) {
        boolean inWater = player.isTouchingWater();
        boolean onBasin = player.getSteppingBlockState().isOf(ModBlocks.WASH_BASIN);
        if (!inWater && !onBasin) {
            WASH_TICKS.remove(player.getUuid());
            return;
        }
        UUID id = player.getUuid();
        if (!player.hasStatusEffect(ModEffects.SWEATY_FEET)) {
            WASH_TICKS.remove(id);
            return; // 脚不臭泡水 = 游泳，不播洗脚声不累计（实测无汗脚水里一直响洗脚声）
        }
        int consecutive = WASH_TICKS.merge(id, 1, Integer::sum);
        // 泡脚表现：水花粒子 + 水声（每秒）
        if (consecutive % 20 == 0 && player.getWorld() instanceof ServerWorld serverLevel) {
            serverLevel.spawnParticles(ParticleTypes.SPLASH,
                player.getX(), player.getY() + 0.1, player.getZ(),
                3, 0.2, 0.0, 0.2, 0.05);
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_GENERIC_SPLASH, SoundCategory.PLAYERS, 0.4F, 1.0F);
        }
        if (consecutive >= washTicksFor(player) && player.hasStatusEffect(ModEffects.SWEATY_FEET)) {
            player.removeStatusEffect(ModEffects.SWEATY_FEET);
            player.removeStatusEffect(StatusEffects.SLOWNESS); // 洗清汗脚：二级汗脚刷的缓慢一并清（防 300s 残留）
            ((SweatyDataHolder) player).setSweatState(-1); // 洗清汗脚：持久化归零
            DEGRADE_TICKS.remove(id);
            WASH_TICKS.remove(id);
            // 健康生活：成功洗了一次汗脚（赤脚泡水路径）
            if (player instanceof net.minecraft.server.network.ServerPlayerEntity sp) {
                awardCriterion(sp, "healthy", "healthy");
            }
        }
    }

    /**
     * 泡脚倒计时 HUD（action bar）：清水="正在洗脚"，药水="正在清理真菌"。
     * 开始泡脚时立即调一次（首句不等 20 tick），之后 tickBasinSoak 每秒刷。
     */
    static void showBasinSoakHud(PlayerEntity player) {
        BlockPos pos = BASIN_POS.get(player.getUuid());
        if (pos == null) {
            return;
        }
        BlockState state = player.getWorld().getBlockState(pos);
        boolean medicinal = state.isOf(ModBlocks.WASH_BASIN)
            ? state.get(WashBasinBlock.FILLED) == WashBasinBlock.Filled.MEDICINAL
            : false;
        int t = BASIN_TICKS.getOrDefault(player.getUuid(), 0);
        int left = Math.max(0, (washTicksFor(player) - t) / 20);
        player.sendMessage(Text.translatable(
            medicinal ? "sweatyfeet.msg.cleaning_fungus" : "sweatyfeet.msg.washing", left), true);
    }

    /** 盆泡脚开始/继续：记录目标盆（累计计时不清零——离开暂停，回来右键接着泡） */
    static void startBasinSoak(PlayerEntity player, BlockPos pos) {
        BASIN_POS.put(player.getUuid(), pos.toImmutable());
    }

    /**
     * 盆泡脚推进：右键盆/坐泡脚椅开始的会话，站在盆边（半径 1.5 格）或坐在座位上时累计计时；
     * 离开暂停（不清零）。坐着泡额外要求赤脚（穿鞋只是干坐着，不累计）。
     * - 清水：满 washTicksFor 洗完 → 清汗脚 + 盆变浑水
     * - 药水洗脚水：满 washTicksFor 洗完 → 清汗脚 + 清真菌（药水洗脚水唯一治真菌），盆变浑水
     * 水被舀走/盆被拆 → 会话终止。
     */
    private static void tickBasinSoak(PlayerEntity player) {
        UUID id = player.getUuid();
        BlockPos pos = BASIN_POS.get(id);
        if (pos == null) {
            return;
        }
        World level = player.getWorld();
        BlockState basinState = level.getBlockState(pos);
        if (!basinState.isOf(ModBlocks.WASH_BASIN)) {
            BASIN_TICKS.remove(id);
            BASIN_POS.remove(id);
            return;
        }
        WashBasinBlock.Filled filled = basinState.get(WashBasinBlock.FILLED);
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
        boolean wearingBoots = player.getEquippedStack(EquipmentSlot.FEET).isIn(ItemTags.FOOT_ARMOR);
        if (!player.hasStatusEffect(ModEffects.SWEATY_FEET) && !player.hasStatusEffect(ModEffects.FOOT_FUNGUS)) {
            return; // 脚干净：不累计（不然白水站/坐一会就浑了）
        }
        if (wearingBoots) {
            return; // 穿鞋：暂停计时且不刷倒计时（WashBasinBlock 会提示脱鞋）
        }
        int t = BASIN_TICKS.merge(id, 1, Integer::sum);
        if (t % 20 == 0 && level instanceof ServerWorld serverLevel) {
            // 泡脚表现：水花粒子 + 水声（每秒）
            serverLevel.spawnParticles(ParticleTypes.SPLASH,
                pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5,
                3, 0.2, 0.0, 0.2, 0.05);
            serverLevel.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                SoundEvents.ENTITY_GENERIC_SPLASH, SoundCategory.PLAYERS, 0.4F, 1.0F);
            showBasinSoakHud(player);
        }
        if (t >= washTicksFor(player)) {
            // 洗完：清汗脚（药水还清真菌） + 盆变浑水
            player.removeStatusEffect(ModEffects.SWEATY_FEET);
            player.removeStatusEffect(StatusEffects.SLOWNESS); // 洗清汗脚：二级汗脚刷的缓慢一并清（防 300s 残留）
            ((SweatyDataHolder) player).setSweatState(-1); // 泡脚洗清：持久化归零
            // 健康生活：成功洗了一次汗脚（盆泡脚路径）
            if (player instanceof net.minecraft.server.network.ServerPlayerEntity sp) {
                awardCriterion(sp, "healthy", "healthy");
            }
            if (filled == WashBasinBlock.Filled.MEDICINAL) {
                player.removeStatusEffect(ModEffects.FOOT_FUNGUS);
                ((SweatyDataHolder) player).setFungus(false); // 药水洗脚水治好真菌：持久化
                // 洗心革面：成功去除一次真菌（药水泡脚是唯一治疗途径）
                if (player instanceof net.minecraft.server.network.ServerPlayerEntity sp2) {
                    awardCriterion(sp2, "reform", "reform");
                }
            }
            DEGRADE_TICKS.remove(id);
            WASH_TICKS.remove(id);
            level.setBlockState(pos, basinState.with(WashBasinBlock.FILLED, WashBasinBlock.Filled.DIRTY));
            BASIN_TICKS.remove(id);
            BASIN_POS.remove(id);
            player.sendMessage(Text.translatable("sweatyfeet.msg.soak_done"), true);
        }
    }

    /**
     * 脱鞋后的汗脚降级：每级倒计时 degrade_seconds，到头降一级重新计时，
     * 1 级到头则彻底消除。原版效果机制做不到（到头直接消失），这里手动管理。
     */
    private static void degradeSweatyFeet(PlayerEntity player) {
        StatusEffectInstance sf = player.getStatusEffect(ModEffects.SWEATY_FEET);
        if (sf == null) {
            DEGRADE_TICKS.remove(player.getUuid());
            return;
        }
        UUID id = player.getUuid();
        int left = DEGRADE_TICKS.getOrDefault(id, degradeTicks());
        DegradeResult next = nextDegradeState(sf.getAmplifier(), left, degradeTicks());
        // 降级重挂：必须 remove 再 add —— 原版 StatusEffectInstance.update 对"同 amp 更短时长/更低 amp"
        // 一律不覆盖，直接 addEffect(60s) 会被残留的 300s 汗脚挡住，降级永远不生效
        player.removeStatusEffect(ModEffects.SWEATY_FEET);
        player.addStatusEffect(sweatyFeetEffect(next.ticksLeft(), next.amplifier()));
        ((SweatyDataHolder) player).setSweatState(next.amplifier()); // 持久化降级后的等级
        DEGRADE_TICKS.put(id, next.ticksLeft());
    }

    /** 汗脚效果实例：无粒子但 HUD 图标要显示（5 参构造 showParticles=false 会把 showIcon 一起关掉，实测图标消失） */
    private static StatusEffectInstance sweatyFeetEffect(int ticks, int amplifier) {
        return new StatusEffectInstance(ModEffects.SWEATY_FEET, ticks, amplifier, false, false, true);
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

    /**
     * 主手汗靴 + 副手玻璃瓶 + 潜行右键 → 倒汗，靴子还原，产出汗液瓶。
     * 双路径共用（UseItemCallback / UseBlockCallback）：返回 true = 已拦截（客户端不再继续
     * use 物品 → 汗靴不会被 ArmorItem.use 预测装备；服务端不再真正装备）。
     */
    private static boolean tryPourSweat(PlayerEntity player) {
        if (!player.isSneaking()) {
            return false;
        }

        ItemStack main = player.getMainHandStack();
        if (!main.isIn(ItemTags.FOOT_ARMOR)) {
            return false;
        }
        SweatData data = main.get(ModDataComponents.SWEAT);
        if (data == null) {
            return false;
        }

        ItemStack offhand = player.getOffHandStack();
        if (!offhand.isOf(Items.GLASS_BOTTLE)) {
            debugLog(SfConfig.DEBUG_FLOW_LOG, "flow",
                player.getGameProfile().getName() + " pour blocked: offhand is not glass bottle");
            return false;
        }

        // 双端都取消：客户端若只 return，会继续 Item.use → ArmorItem.use → swapWithEquipmentSlot
        // 把主手汗靴【预测性】穿到脚上，产生"幽灵汗靴"，随后被服务端同步纠正消失。
        if (player.getWorld().isClient) {
            return true;
        }

        // 以下仅服务端执行
        // 瓶等级 = 汗靴的等级（存在 SweatData 里：汗化时靴子等级固化，倒汗时按它产瓶）
        // 之前用 WEAR_TICKS 内存态算等级：计时随脱鞋清零，导致二级/三级汗脚只能倒出一级瓶
        int lvl = data.level() + 1;
        if (!player.isCreative()) {
            offhand.decrement(1); // NeoForge 版 consume(1, player) 创造模式不消耗，语义对齐
        }
        if (lvl == 3 && main.isOf(ModItems.FERMENTED_BOOTS)) {
            // 发酵靴汗脚 3 级：汗液+糖发酵成熟 → 产"xxx的汗液饮品"（正面 buff）
            ItemStack drink = new ItemStack(ModItems.SWEAT_DRINK);
            drink.set(ModDataComponents.DRINK_TYPE, "fermented");
            drink.set(DataComponentTypes.CUSTOM_NAME, Text.translatable("item.sweatyfeet.sweat_drink.owned",
                player.getGameProfile().getName()));
            player.getInventory().offerOrDrop(drink);
        } else {
            // 汗液瓶：等级 + 风味（按汗靴材质）写入组件，名字带风味（lang key 显示）
            String flavor = flavorIdFor(main);
            ItemStack bottle = new ItemStack(ModItems.SWEAT_BOTTLE);
            bottle.set(ModDataComponents.SWEAT_LEVEL, lvl);
            if (!"plain".equals(flavor)) {
                bottle.set(ModDataComponents.SWEAT_FLAVOR, flavor);
            }
            bottle.set(DataComponentTypes.CUSTOM_NAME, Text.translatable("item.sweatyfeet.sweat_bottle.owned",
                player.getGameProfile().getName(),
                Text.translatable("item.sweatyfeet.flavor." + flavor),
                romanLevel(data.level())));
            player.getInventory().offerOrDrop(bottle);
        }

        // 靴子还原：删组件 + 还原自定义名（无原名则回到默认显示名）
        main.remove(ModDataComponents.SWEAT);
        if (data.originalName() != null) {
            main.set(DataComponentTypes.CUSTOM_NAME, data.originalName());
        } else {
            main.remove(DataComponentTypes.CUSTOM_NAME);
        }

        // 倒汗只产出瓶子 + 还原靴子；汗脚效果与穿戴计时完全保留（用户拍板：汗脚只能靠洗脚清，
        // 倒汗不清——真菌本来就不清，只能药水洗脚水泡脚治）
        debugLog(SfConfig.DEBUG_FLOW_LOG, "flow",
            player.getGameProfile().getName() + " poured: lvl=" + lvl
            + (lvl == 3 && main.isOf(ModItems.FERMENTED_BOOTS) ? " (drink)" : " (bottle)")
            + ", boots restored");
        return true;
    }

    /** 汗化：写组件（存原名）+ 改名「充满<玩家名>汗液的<原名>（等级X）」+ 挂汗脚 1 级 */
    private static void sweatify(PlayerEntity player, ItemStack boots) {
        Text customName = boots.get(DataComponentTypes.CUSTOM_NAME);
        SweatData data = new SweatData(0, customName);
        boots.set(ModDataComponents.SWEAT, data);
        renameSweatyBoots(player, boots, data);
        player.addStatusEffect(sweatyFeetEffect(effectTicks(), 0));
        ((SweatyDataHolder) player).setSweatState(0); // 持久化汗脚 1 级
    }

    /** 汗靴改名：带玩家名 + 等级。基底名取"原始自定义名或物品默认名"——
     *  绝不能用 getHoverName（含当前自定义名），升级重命名会把已套的名字再套一层（叠名 bug） */
    private static void renameSweatyBoots(PlayerEntity player, ItemStack boots, SweatData data) {
        Text baseName = data.originalName() != null
            ? data.originalName()
            : Text.translatable(boots.getTranslationKey());
        boots.set(DataComponentTypes.CUSTOM_NAME, Text.translatable("item.sweatyfeet.sweaty_boots",
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

    /** 汗液瓶风味 = 汗靴材质（发酵靴归皮革，与材质同味）；非原版五材质靴子 → plain（无风味组件/不显示） */
    static String flavorIdFor(ItemStack boots) {
        if (boots.isOf(Items.LEATHER_BOOTS) || boots.isOf(ModItems.FERMENTED_BOOTS)) {
            return "leather";
        }
        if (boots.isOf(Items.IRON_BOOTS)) {
            return "iron";
        }
        if (boots.isOf(Items.GOLDEN_BOOTS)) {
            return "gold";
        }
        if (boots.isOf(Items.DIAMOND_BOOTS)) {
            return "diamond";
        }
        if (boots.isOf(Items.NETHERITE_BOOTS)) {
            return "netherite";
        }
        return "plain";
    }

}
