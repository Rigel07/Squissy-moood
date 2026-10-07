package com.squissy.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.squissy.SquissyMod;
import com.squissy.entity.SquissyEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class SquissyRenderer extends MobRenderer<SquissyEntity, SquissyModel> {
    private static final String[] COLORS = {"red", "green", "yellow", "pink", "blue"};
    private static final ResourceLocation[] TEXTURES = new ResourceLocation[COLORS.length];
    private static final ResourceLocation[] GLOW = new ResourceLocation[COLORS.length];

    static {
        for (int i = 0; i < COLORS.length; i++) {
            TEXTURES[i] = new ResourceLocation(SquissyMod.MOD_ID, "textures/entity/squissy_" + COLORS[i] + ".png");
            GLOW[i] = new ResourceLocation(SquissyMod.MOD_ID, "textures/entity/squissy_" + COLORS[i] + "_glow.png");
        }
    }

    public SquissyRenderer(EntityRendererProvider.Context context) {
        super(context, new SquissyModel(context.bakeLayer(SquissyModel.LAYER)), 0.6F);
        addLayer(new SquissyGlowLayer(this));
        addLayer(new SquissyItemLayer(this, context.getItemRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(SquissyEntity entity) {
        return TEXTURES[Math.floorMod(entity.getVariant(), TEXTURES.length)];
    }

    public static ResourceLocation glowTexture(int variant) {
        return GLOW[Math.floorMod(variant, GLOW.length)];
    }

    /** Se aplasta y estira como gelatina. */
    @Override
    protected void scale(SquissyEntity entity, PoseStack poseStack, float partialTick) {
        float t = (entity.tickCount + partialTick) * 0.18F;
        float wobble = 0.045F * Mth.sin(t);
        poseStack.scale(1.0F + wobble, 1.0F - wobble, 1.0F + wobble);
    }
}
