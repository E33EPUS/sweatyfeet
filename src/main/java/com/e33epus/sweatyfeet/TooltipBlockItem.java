package com.e33epus.sweatyfeet;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

/**
 * 带悬浮描述的方块物品：构造时传 tooltip 的 lang key 列表，悬停时逐条渲染。
 * （原版 BlockItem 不覆写 appendHoverText，方块描述只能靠这个子类挂）
 */
public class TooltipBlockItem extends BlockItem {
    private final String[] tooltipKeys;

    public TooltipBlockItem(Block block, Item.Properties properties, String... tooltipKeys) {
        super(block, properties);
        this.tooltipKeys = tooltipKeys;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        for (String key : tooltipKeys) {
            tooltip.add(Component.translatable(key));
        }
    }
}
