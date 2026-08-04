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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
        if (player.level().isClientSide) {
            return;
        }

        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (!boots.is(ItemTags.FOOT_ARMOR)) {
            // 脱鞋：只清计时，不手动移除 debuff —— 让效果自然走完剩余时长
            // （真菌的减速属性修饰符随效果结束自动移除）
            WEAR_TICKS.remove(player.getUUID());
            return;
        }

        int totalTicks = WEAR_TICKS.merge(player.getUUID(), 1, Integer::sum);
        SweatData data = boots.get(ModDataComponents.SWEAT.get());

        if (data == null) {
            // 未汗化：穿满 1 级时长触发汗化（>= 而不是 ==：中途改小配置时累计时长可能已跳过阈值）
            if (totalTicks >= lvl1()) {
                sweatify(player, boots);
            }
        } else {
            // 已汗化：按总时长推进汗脚等级
            int amplifier = computeAmplifier(totalTicks, lvl2(), lvl3());
            if (totalTicks % REFRESH_INTERVAL == 0) {
                refreshEffect(player, ModEffects.SWEATY_FEET, amplifier);
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
        offhand.consume(1, player);
        ItemStack bottle = new ItemStack(ModItems.SWEAT_BOTTLE.get());
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
        boots.set(ModDataComponents.SWEAT.get(), new SweatData(customName));
        boots.set(DataComponents.CUSTOM_NAME, Component.translatable("item.sweatyfeet.sweaty_boots", displayName));
        player.addEffect(new MobEffectInstance(ModEffects.SWEATY_FEET, effectTicks(), 0, false, true));
    }

    private static void refreshEffect(Player player, Holder<MobEffect> effect, int amplifier) {
        player.addEffect(new MobEffectInstance(effect, effectTicks(), amplifier, false, true));
    }
}
