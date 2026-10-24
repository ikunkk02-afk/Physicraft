package com.shoyun.physicraft.client;

import org.joml.Quaternionf;

import com.mojang.blaze3d.vertex.PoseStack;
import com.shoyun.physicraft.entity.PhysicsBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Draws the converted block as a cube spinning about its center, similar to a falling block with orientation.
 */
public class PhysicsBlockRenderer extends EntityRenderer<PhysicsBlockEntity> {
    public PhysicsBlockRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F;
    }

    @Override
    public ResourceLocation getTextureLocation(PhysicsBlockEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }

    @Override
    public void render(PhysicsBlockEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        BlockState state = entity.getBlockState();
        if (state.isAir() || state.getRenderShape() != RenderShape.MODEL) {
            super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
            return;
        }
        Quaternionf orientation = new Quaternionf(
                entity.prevRenderRotX(),
                entity.prevRenderRotY(),
                entity.prevRenderRotZ(),
                entity.prevRenderRotW());
        Quaternionf current = new Quaternionf(
                entity.renderRotX(),
                entity.renderRotY(),
                entity.renderRotZ(),
                entity.renderRotW());
        orientation.slerp(current, partialTick);

        poseStack.pushPose();
        poseStack.translate(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
        poseStack.mulPose(orientation);
        poseStack.translate(-0.5D, -0.5D, -0.5D);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                state, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }
}
