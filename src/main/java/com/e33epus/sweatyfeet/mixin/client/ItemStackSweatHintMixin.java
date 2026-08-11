package com.e33epus.sweatyfeet.mixin.client;

import java.util.List;
import com.e33epus.sweatyfeet.ModDataComponents;
import net.minecraft.text.Text;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 汗靴 tooltip 提示：靴子汗化（带 SWEAT 组件）后，在原版描述末尾追加一行
 * "副手持空玻璃瓶，潜行+右键倒出汗液瓶"，让新玩家知道汗靴能制汗液瓶。
 * 状态自动跟随：汗化→显示，洗掉/倒汗（组件移除）→消失，无需各处手动增删。
 * 防御：ItemStack.getTooltipLines 是渲染热路径，任何异常静默跳过（双开 jar 版本不一致防御）。
 * 注入实例方法 this = ItemStack，不能把 ItemStack 当参数（无参宿主规则，错签会崩启动）。
 */
@Mixin(ItemStack.class)
public abstract class ItemStackSweatHintMixin {
    // method 用名字不带描述符：ItemStack 只有一个 getTooltipLines 重载（javap 确认），
    // 按名唯一解析，绕开 MCP/部分工具对 Item$TooltipContext 嵌套类 $ 的匹配 bug
    @Inject(method = "getTooltip", at = @At("RETURN"))
    private void sweatyfeet$addSweatHint(Item.TooltipContext context, PlayerEntity player, TooltipType flag,
                                         CallbackInfoReturnable<List<Text>> cir) {
        try {
            ItemStack stack = (ItemStack) (Object) this;
            if (!stack.contains(ModDataComponents.SWEAT)) {
                return; // 非汗靴：快速短路，不碰热路径
            }
            List<Text> lines = cir.getReturnValue();
            if (lines == null) {
                return;
            }
            lines.add(Text.translatable("item.sweatyfeet.sweaty_hint"));
        } catch (Throwable t) {
            // 渲染热路径：类缺失/组件未注册等任何异常都不崩
        }
    }
}
