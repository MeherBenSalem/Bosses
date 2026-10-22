package com.nightbeam.remnants.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.nightbeam.remnants.entity.ChainAnchorEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * {@code remnants:chain_anchor} (Chain Wall), built from vanilla block models so it
 * needs no new assets: a polished blackstone post, a short chain and a soul lantern, plus the chain strung
 * to the next anchor at {@link ChainAnchorEntity#CHAIN_HEIGHT}. It flashes red when struck and sinks into
 * the floor when destroyed.
 */
public class ChainAnchorRenderer extends EntityRenderer<ChainAnchorEntity> {
	private final BlockRenderDispatcher blocks;

	public ChainAnchorRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.blocks = context.getBlockRenderDispatcher();
		this.shadowRadius = 0.45f;
	}

	@Override
	public void render(ChainAnchorEntity anchor, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
		float dying = anchor.deathTime > 0 ? Math.min(1, (anchor.deathTime + partialTick) / 10f) : 0;
		int overlay = OverlayTexture.pack(0, anchor.hurtTime > 0 || anchor.deathTime > 0);
		poseStack.pushPose();
		poseStack.translate(0, -dying * 2.0, 0);
		poseStack.translate(-0.5, 0, -0.5);
		blocks.renderSingleBlock(Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState(), poseStack, buffers, light, overlay);
		poseStack.pushPose();
		poseStack.translate(0, 1, 0);
		poseStack.scale(1, 0.45f, 1);
		blocks.renderSingleBlock(Blocks.CHAIN.defaultBlockState(), poseStack, buffers, light, overlay);
		poseStack.popPose();
		poseStack.translate(0, 1.45, 0);
		blocks.renderSingleBlock(Blocks.SOUL_LANTERN.defaultBlockState(), poseStack, buffers, light, overlay);
		poseStack.popPose();
		ChainAnchorEntity next = anchor.neighbour();
		if (next != null && dying == 0) {
			Vec3 offset = next.getPosition(partialTick).subtract(anchor.getPosition(partialTick));
			Vec3 height = new Vec3(0, ChainAnchorEntity.CHAIN_HEIGHT, 0);
			ChainLinks.render(poseStack, buffers, height, offset.add(height), light);
		}
		super.render(anchor, yaw, partialTick, poseStack, buffers, light);
	}

	@Override
	public ResourceLocation getTextureLocation(ChainAnchorEntity anchor) {
		return TextureAtlas.LOCATION_BLOCKS;
	}
}
