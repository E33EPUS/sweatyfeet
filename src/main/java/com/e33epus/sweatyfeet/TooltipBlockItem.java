package com.e33epus.sweatyfeet;

import java.util.List;
import net.minecraft.text.Text;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.block.Block;

/**
 * 带悬浮描述的方块物品：构造时传 tooltip 的 lang key 列表，悬停时逐条渲染。
 * （原版 BlockItem 不覆写 appendTooltip，方块描述只能靠这个子类挂）
 */
public class TooltipBlockItem extends BlockItem {
    private final String[] tooltipKeys;

    public TooltipBlockItem(Block block, Item.Settings properties, String... tooltipKeys) {
        super(block, properties);
        this.tooltipKeys = tooltipKeys;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip,
                                TooltipType flag) {
        for (String key : tooltipKeys) {
            SweatyTooltips.addIfPresent(tooltip, key);
        }
    }
}
