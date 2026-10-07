package com.squissy.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.squissy.SquissyMod;
import com.squissy.entity.SquissyEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Squissy: una esfera hecha de cubitos (se genera con tools/ en SquissyShape), cola de babosa,
 * carita con ojazos y dos tentáculos que se balancean.
 */
public class SquissyModel extends EntityModel<SquissyEntity> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation(SquissyMod.MOD_ID, "squissy"), "main");

    private static final float TENTACLE_TOP = -12.0F;

    private final ModelPart root;
    private final ModelPart blob;
    private final ModelPart tentL;
    private final ModelPart tentR;

    public SquissyModel(ModelPart root) {
        this.root = root;
        this.blob = root.getChild("blob");
        this.tentL = blob.getChild("tent_l");
        this.tentR = blob.getChild("tent_r");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        CubeListBuilder cubes = CubeListBuilder.create();
        for (int[] b : SquissyShape.BODY) {
            cubes.texOffs(b[6], b[7]).addBox(b[0], b[1], b[2], b[3], b[4], b[5]);
        }
        for (int[] b : SquissyShape.FACE) {
            cubes.texOffs(b[6], b[7]).addBox(b[0], b[1], b[2], b[3], b[4], b[5]);
        }
        PartDefinition blob = root.addOrReplaceChild("blob", cubes, PartPose.offset(0.0F, 24.0F, 0.0F));

        CubeListBuilder stalk = CubeListBuilder.create().texOffs(SquissyShape.STALK_UV[0], SquissyShape.STALK_UV[1])
                .addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F);
        CubeListBuilder knob = CubeListBuilder.create().texOffs(SquissyShape.KNOB_UV[0], SquissyShape.KNOB_UV[1])
                .addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F);
        PartDefinition left = blob.addOrReplaceChild("tent_l", stalk, PartPose.offset(2.5F, TENTACLE_TOP, -2.0F));
        left.addOrReplaceChild("knob", knob, PartPose.offset(0.0F, -3.0F, 0.0F));
        PartDefinition right = blob.addOrReplaceChild("tent_r", stalk, PartPose.offset(-2.5F, TENTACLE_TOP, -2.0F));
        right.addOrReplaceChild("knob", knob, PartPose.offset(0.0F, -3.0F, 0.0F));

        return LayerDefinition.create(mesh, SquissyShape.TEX_W, SquissyShape.TEX_H);
    }

    @Override
    public void setupAnim(SquissyEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        float t = ageInTicks * 0.15F;
        float amplitude = entity.isClimbing() ? 0.45F : 0.25F;
        tentL.zRot = Mth.sin(t) * amplitude - 0.15F;
        tentR.zRot = Mth.sin(t + 1.7F) * amplitude + 0.15F;
        tentL.xRot = Mth.cos(t * 0.8F) * 0.2F;
        tentR.xRot = Mth.cos(t * 0.8F + 1.0F) * 0.2F;
        // Se contonea al avanzar y se inclina un poco al mirar.
        blob.zRot = Mth.sin(limbSwing * 0.7F) * 0.12F * Math.min(1.0F, limbSwingAmount * 4.0F);
        blob.xRot = headPitch * Mth.DEG_TO_RAD * 0.25F + (entity.isClimbing() ? -0.35F : 0.0F);
        blob.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.3F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        root.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
