package com.e33epus.sweatyfeet;

import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * 投掷物客户端渲染器注册（不注册会 NPE 崩溃：EntityRenderDispatcher 找不到 renderer）。
 * 照抄原版雪球：ThrownItemRenderer 直接渲染物品本体（汗液瓶贴图）。
 */
@EventBusSubscriber(modid = SweatyFeet.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ModRenderers {
    private ModRenderers() {
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SWEAT_BOTTLE.get(), ThrownItemRenderer::new);
    }
}
