package com.e33epus.sweatyfeet;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * 发酵靴（皮革靴+糖合成）：穿它汗脚发酵，汗脚 3 级时副手拿空玻璃瓶右键
 * 倒出汗液饮品（正面 buff）。独立成类是为了挂悬浮描述（原 ArmorItem 无 tooltip）。
 */
public class FermentedBootsItem extends ArmorItem {
    public FermentedBootsItem(Holder<ArmorMaterial> material, ArmorItem.Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        // 简介行 + 条件行（分行）
        tooltip.add(Component.translatable("item.sweatyfeet.fermented_boots.tooltip1"));
        tooltip.add(Component.translatable("item.sweatyfeet.fermented_boots.tooltip2"));
    }
}
