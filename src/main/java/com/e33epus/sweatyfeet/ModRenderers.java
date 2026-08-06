package com.e33epus.sweatyfeet;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * 客户端渲染器注册（不注册会 NPE 崩溃：EntityRenderDispatcher 找不到 renderer）。
 * 汗液瓶照抄原版雪球：ThrownItemRenderer 直接渲染物品本体（汗液瓶贴图）。
 */
@EventBusSubscriber(modid = SweatyFeet.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ModRenderers {
    private ModRenderers() {
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SWEAT_BOTTLE.get(), ThrownItemRenderer::new);
        // 座位实体：隐形，no-op 渲染器（防 null 崩溃）
        event.registerEntityRenderer(ModEntities.SEAT.get(), NoopRenderer::new);
    }

    /** 什么都不画的渲染器（隐形座位用） */
    private static final class NoopRenderer<T extends net.minecraft.world.entity.Entity> extends EntityRenderer<T> {
        private static final ResourceLocation BLANK =
            ResourceLocation.fromNamespaceAndPath(SweatyFeet.MOD_ID, "textures/blank.png");

        NoopRenderer(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public void render(T entity, float yaw, float partialTick, com.mojang.blaze3d.vertex.PoseStack poseStack,
                           net.minecraft.client.renderer.MultiBufferSource buffer, int packedLight) {
        }

        @Override
        public ResourceLocation getTextureLocation(T entity) {
            return BLANK;
        }
    }
}
