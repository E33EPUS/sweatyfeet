package com.e33epus.sweatyfeet;

import java.util.List;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;

/**
 * 发酵靴（皮革靴+糖合成）：穿它汗脚发酵，汗脚 3 级时副手拿空玻璃瓶右键
 * 倒出汗液饮品（正面 buff）。独立成类是为了挂悬浮描述（原 ArmorItem 无 tooltip）。
 */
public class FermentedBootsItem extends ArmorItem {
    public FermentedBootsItem(RegistryEntry<ArmorMaterial> material, ArmorItem.Type type, Item.Settings properties) {
        super(material, type, properties);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip,
                                TooltipType flag) {
    }
}
