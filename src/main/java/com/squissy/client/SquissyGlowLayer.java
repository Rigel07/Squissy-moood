package com.squissy.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.squissy.entity.SquissyEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

/** Capa que hace brillar a Squissy (con un latido suave) del color de su variante. */
public class SquissyGlowLayer extends RenderLayer<SquissyEntity, SquissyModel> {

    public SquissyGlowLayer(RenderLayerParent<SquissyEntity, SquissyModel> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, SquissyEntity entity,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (entity.isInvisible()) {
            return;
        }
        float pulse = 0.6F + 0.4F * (0.5F + 0.5F * Mth.sin(ageInTicks * 0.08F));
        VertexConsumer consumer = buffer.getBuffer(RenderType.eyes(SquissyRenderer.glowTexture(entity.getVariant())));
        getParentModel().renderToBuffer(poseStack, consumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                pulse, pulse, pulse, 1.0F);
    }
}
