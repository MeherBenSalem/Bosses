package com.nightbeam.remnants.client.realm;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.nightbeam.remnants.Constants;
import com.nightbeam.remnants.block.realm.GraceEmberBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The grace's light (realm.md section 6): a pool of gold on the ground, a wispy flame floating over the cairn, a
 * bright core, motes circling up, and a beam that marks the grace from far away. All additive.
 */
public class GraceEmberRenderer implements BlockEntityRenderer<GraceEmberBlockEntity, GraceEmberRenderer.State> {
	private static final RenderType HALO = glow("grace_halo");
	private static final RenderType FLAME = glow("grace_flame");
	private static final RenderType CORE = glow("grace_core");
	private static final RenderType BEAM = glow("grace_beam");
	private static final int FLAME_FRAMES = 8;

	public GraceEmberRenderer(BlockEntityRendererProvider.Context context) {
	}

	/**
	 * Additive, like 1.21.1's {@code eyes} (which blends translucently since 26.1): the vanilla energy swirl type with no
	 * scroll. The textures' alpha is their brightness, so near-black texels are cut out instead of writing depth.
	 */
	private static RenderType glow(String name) {
		return RenderTypes.energySwirl(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/effect/" + name + ".png"), 0, 0);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(GraceEmberBlockEntity grace, State state, float partialTick, Vec3 cameraPosition,
			ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(grace, state, partialTick, cameraPosition, breakProgress);
		Level level = grace.getLevel();
		state.visible = level != null;
		if (level == null) return;
		state.time = (level.getGameTime() % 72000L) + partialTick + Math.floorMod(grace.getBlockPos().asLong(), 97L);
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		if (!state.visible) return;
		float time = state.time;
		float pulse = 0.84f + 0.10f * Mth.sin(time * 0.05f) + 0.06f * Mth.sin(time * 0.137f);
		float bob = 0.035f * Mth.sin(time * 0.07f);

		pose.pushPose();
		pose.translate(0.5, 0.02, 0.5);
		collector.submitCustomGeometry(pose, HALO, (p, vc) -> GlowQuads.flat(vc, p, 1.5f, 0.55f * pulse));
		pose.translate(0.0, 0.07, 0.0);
		collector.submitCustomGeometry(pose, HALO, (p, vc) -> GlowQuads.flat(vc, p, 0.6f, 0.55f * pulse));
		pose.popPose();

		pose.pushPose();
		pose.translate(0.5, 0.3, 0.5);
		pose.rotate(Axis.YP.rotationDegrees(-camera.yRot));
		collector.submitCustomGeometry(pose, BEAM, (p, vc) -> GlowQuads.billboard(vc, p, 0.3f, 0.0f, 9.0f, 0, 1, 0, 1, 0.45f * pulse));
		pose.popPose();

		int frame = (int) (time / 3.0f) % FLAME_FRAMES;
		float u0 = frame / (float) FLAME_FRAMES, u1 = (frame + 1) / (float) FLAME_FRAMES;
		pose.pushPose();
		pose.translate(0.5, 0.28 + bob, 0.5);
		pose.rotate(Axis.YP.rotationDegrees(-camera.yRot));
		collector.submitCustomGeometry(pose, FLAME, (p, vc) -> {
			GlowQuads.billboard(vc, p, 0.3f, 0.0f, 1.15f, u0, u1, 0, 1, 0.68f * pulse);
			GlowQuads.billboard(vc, p, 0.5f, -0.12f, 1.5f, u1, u0, 0, 1, 0.14f * pulse);
		});
		pose.popPose();

		pose.pushPose();
		pose.translate(0.5, 0.45 + bob, 0.5);
		pose.rotate(camera.orientation);
		collector.submitCustomGeometry(pose, CORE, (p, vc) -> GlowQuads.billboard(vc, p, 0.2f, -0.2f, 0.2f, 0, 1, 0, 1, 0.36f + 0.08f * pulse));
		pose.popPose();
		for (int i = 0; i < 4; i++) {
			float rise = ((time * 0.006f + i * 0.25f) % 1.0f);
			float angle = time * 0.03f + i * Mth.HALF_PI;
			float radius = 0.32f + 0.08f * Mth.sin(time * 0.05f + i);
			pose.pushPose();
			pose.translate(0.5 + Mth.cos(angle) * radius, 0.3 + rise * 1.4, 0.5 + Mth.sin(angle) * radius);
			pose.rotate(camera.orientation);
			float size = 0.07f * (1.0f - rise * 0.5f);
			collector.submitCustomGeometry(pose, CORE, (p, vc) -> GlowQuads.billboard(vc, p, size, -size, size, 0, 1, 0, 1, Mth.sin(rise * Mth.PI) * 0.9f));
			pose.popPose();
		}
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	public int getViewDistance() {
		return 128;
	}

	/** NeoForge asks the renderer for the culling box. */
	public AABB getRenderBoundingBox(GraceEmberBlockEntity grace) {
		return grace.getRenderBoundingBox();
	}

	public static class State extends BlockEntityRenderState {
		boolean visible;
		float time;
	}
}
