package com.e33epus.sweatyfeet;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;

/**
 * 客户端渲染器注册（不注册会 NPE 崩溃：EntityRenderDispatcher 找不到 renderer）。
 */
public final class ModRenderers {
    private ModRenderers() {
    }

    public static void init() {
        // 座位实体：隐形，no-op 渲染器（防 null 崩溃）
        EntityRendererRegistry.register(ModEntities.SEAT, NoopRenderer::new);
    }

    /** 什么都不画的渲染器（隐形座位用） */
    private static final class NoopRenderer<T extends net.minecraft.entity.Entity> extends EntityRenderer<T> {
        private static final Identifier BLANK =
            Identifier.of(SweatyFeet.MOD_ID, "textures/blank.png");

        NoopRenderer(EntityRendererFactory.Context context) {
            super(context);
        }

        @Override
        public void render(T entity, float yaw, float tickDelta, net.minecraft.client.util.math.MatrixStack poseStack,
                           net.minecraft.client.render.VertexConsumerProvider buffer, int packedLight) {
        }

        @Override
        public Identifier getTexture(T entity) {
            return BLANK;
        }
    }
}
