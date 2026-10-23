package com.nightbeam.remnants.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.nightbeam.remnants.entity.ChainAnchorEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * {@code remnants:chain_anchor} (Chain Wall), built from vanilla block models so it
 * needs no new assets: a polished blackstone post, a short chain and a soul lantern, plus the chain strung
 * to the next anchor at {@link ChainAnchorEntity#CHAIN_HEIGHT}. It flashes red when struck and sinks into
 * the floor when destroyed.
 */
public class ChainAnchorRenderer extends EntityRenderer<ChainAnchorEntity, ChainAnchorRenderer.State> {
	private static final BlockDisplayContext DISPLAY = BlockDisplayContext.create();
	private final BlockModelResolver blocks;

	public ChainAnchorRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.blocks = context.getBlockModelResolver();
		this.shadowRadius = 0.45f;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(ChainAnchorEntity anchor, State state, float partialTick) {
		super.extractRenderState(anchor, state, partialTick);
		state.dying = anchor.deathTime > 0 ? Math.min(1, (anchor.deathTime + partialTick) / 10f) : 0;
		state.overlay = OverlayTexture.pack(0, anchor.hurtTime > 0 || anchor.deathTime > 0);
		blocks.update(state.post, Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState(), DISPLAY);
		blocks.update(state.chain, Blocks.IRON_CHAIN.defaultBlockState(), DISPLAY);
		blocks.update(state.lantern, Blocks.SOUL_LANTERN.defaultBlockState(), DISPLAY);
		ChainAnchorEntity next = anchor.neighbour();
		state.span = next == null ? null : next.getPosition(partialTick).subtract(anchor.getPosition(partialTick));
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		int light = state.lightCoords;
		poseStack.pushPose();
		poseStack.translate(0, -state.dying * 2.0, 0);
		poseStack.translate(-0.5, 0, -0.5);
		state.post.submit(poseStack, collector, light, state.overlay, state.outlineColor);
		poseStack.pushPose();
		poseStack.translate(0, 1, 0);
		poseStack.scale(1, 0.45f, 1);
		state.chain.submit(poseStack, collector, light, state.overlay, state.outlineColor);
		poseStack.popPose();
		poseStack.translate(0, 1.45, 0);
		state.lantern.submit(poseStack, collector, light, state.overlay, state.outlineColor);
		poseStack.popPose();
		if (state.span != null && state.dying == 0) {
			Vec3 height = new Vec3(0, ChainAnchorEntity.CHAIN_HEIGHT, 0);
			ChainLinks.submit(poseStack, collector, height, state.span.add(height), light);
		}
		super.submit(state, poseStack, collector, camera);
	}

	@Override
	protected AABB getBoundingBoxForCulling(ChainAnchorEntity anchor, float partialTicks) {
		return anchor.cullingBox(super.getBoundingBoxForCulling(anchor, partialTicks));
	}

	public static class State extends EntityRenderState {
		final BlockModelRenderState post = new BlockModelRenderState();
		final BlockModelRenderState chain = new BlockModelRenderState();
		final BlockModelRenderState lantern = new BlockModelRenderState();
		float dying;
		int overlay;
		@Nullable Vec3 span;
	}
}
