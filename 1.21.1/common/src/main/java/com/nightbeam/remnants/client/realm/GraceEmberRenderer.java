package com.nightbeam.remnants.client.realm;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.nightbeam.remnants.Constants;
import com.nightbeam.remnants.block.realm.GraceEmberBlockEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * The grace's light (realm.md section 6): a pool of gold on the ground, a wispy flame floating over the cairn, a
 * bright core, motes circling up, and a beam that marks the grace from far away. All additive.
 */
public class GraceEmberRenderer implements BlockEntityRenderer<GraceEmberBlockEntity> {
	private static final ResourceLocation HALO = tex("grace_halo");
	private static final ResourceLocation FLAME = tex("grace_flame");
	private static final ResourceLocation CORE = tex("grace_core");
	private static final ResourceLocation BEAM = tex("grace_beam");
	private static final int FLAME_FRAMES = 8;

	public GraceEmberRenderer(BlockEntityRendererProvider.Context context) {
	}

	private static ResourceLocation tex(String name) {
		return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "textures/effect/" + name + ".png");
	}

	@Override
	public void render(GraceEmberBlockEntity grace, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
		Level level = grace.getLevel();
		if (level == null) return;
		float time = (level.getGameTime() % 72000L) + partialTick + Math.floorMod(grace.getBlockPos().asLong(), 97L);
		Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
		float pulse = 0.84f + 0.10f * Mth.sin(time * 0.05f) + 0.06f * Mth.sin(time * 0.137f);
		float bob = 0.035f * Mth.sin(time * 0.07f);

		VertexConsumer halo = buffers.getBuffer(RenderType.eyes(HALO));
		pose.pushPose();
		pose.translate(0.5, 0.02, 0.5);
		GlowQuads.flat(halo, pose.last(), 1.5f, 0.55f * pulse);
		pose.translate(0.0, 0.07, 0.0);
		GlowQuads.flat(halo, pose.last(), 0.6f, 0.55f * pulse);
		pose.popPose();

		VertexConsumer beam = buffers.getBuffer(RenderType.eyes(BEAM));
		pose.pushPose();
		pose.translate(0.5, 0.3, 0.5);
		pose.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()));
		GlowQuads.billboard(beam, pose.last(), 0.3f, 0.0f, 9.0f, 0, 1, 0, 1, 0.45f * pulse);
		pose.popPose();

		int frame = (int) (time / 3.0f) % FLAME_FRAMES;
		float u0 = frame / (float) FLAME_FRAMES, u1 = (frame + 1) / (float) FLAME_FRAMES;
		VertexConsumer flame = buffers.getBuffer(RenderType.eyes(FLAME));
		pose.pushPose();
		pose.translate(0.5, 0.28 + bob, 0.5);
		pose.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()));
		GlowQuads.billboard(flame, pose.last(), 0.3f, 0.0f, 1.15f, u0, u1, 0, 1, 0.68f * pulse);
		GlowQuads.billboard(flame, pose.last(), 0.5f, -0.12f, 1.5f, u1, u0, 0, 1, 0.14f * pulse);
		pose.popPose();

		VertexConsumer core = buffers.getBuffer(RenderType.eyes(CORE));
		pose.pushPose();
		pose.translate(0.5, 0.45 + bob, 0.5);
		pose.mulPose(camera.rotation());
		GlowQuads.billboard(core, pose.last(), 0.2f, -0.2f, 0.2f, 0, 1, 0, 1, 0.36f + 0.08f * pulse);
		pose.popPose();
		for (int i = 0; i < 4; i++) {
			float rise = ((time * 0.006f + i * 0.25f) % 1.0f);
			float angle = time * 0.03f + i * Mth.HALF_PI;
			float radius = 0.32f + 0.08f * Mth.sin(time * 0.05f + i);
			pose.pushPose();
			pose.translate(0.5 + Mth.cos(angle) * radius, 0.3 + rise * 1.4, 0.5 + Mth.sin(angle) * radius);
			pose.mulPose(camera.rotation());
			float size = 0.07f * (1.0f - rise * 0.5f);
			GlowQuads.billboard(core, pose.last(), size, -size, size, 0, 1, 0, 1, Mth.sin(rise * Mth.PI) * 0.9f);
			pose.popPose();
		}
	}

	@Override
	public boolean shouldRenderOffScreen(GraceEmberBlockEntity grace) {
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
}
