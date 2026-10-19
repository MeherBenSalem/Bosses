package com.nightbeam.remnants.client.realm;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.nightbeam.remnants.Constants;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Matrix4f;

/**
 * The Eclipsed Realm's sky: a slate-to-ash-gold dome, faint stars, the Gracetree on the
 * northern horizon and the black eclipse with its breathing gold corona high in the south.
 */
public final class EclipseSkyRenderer {
	private static final ResourceLocation ECLIPSE = new ResourceLocation(Constants.MOD_ID, "textures/environment/eclipse.png");
	private static final ResourceLocation GRACETREE = new ResourceLocation(Constants.MOD_ID, "textures/environment/gracetree.png");
	private static final float RADIUS = 100f;
	private static final int SEGMENTS = 32;
	/** Ring elevations (degrees) and their colours; -1 is the gold horizon haze blended into the fog, -2 a darkened fog colour. */
	private static final float[] ELEVATIONS = {90, 58, 32, 14, 4, 0, -12, -90};
	private static final int[] COLORS = {0x0E111C, 0x161B2A, 0x262838, 0x4A4236, -1, -1, -2, 0x08080B};
	private static final int STAR_COUNT = 700;
	private static float[] stars;

	private EclipseSkyRenderer() {
	}

	public static void render(PoseStack poseStack, Matrix4f projection, float partialTick, Camera camera, ClientLevel level,
			Runnable fogSetup) {
		fogSetup.run();
		FogRenderer.levelFogColor();
		float[] fog = RenderSystem.getShaderFogColor();
		float time = level.getGameTime() + partialTick;

		RenderSystem.depthMask(false);
		RenderSystem.disableCull();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		Matrix4f matrix = poseStack.last().pose();

		drawDome(matrix, fog);
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
		drawStars(matrix, time);
		drawGracetree(matrix, time);
		RenderSystem.defaultBlendFunc();
		drawEclipse(poseStack, time);

		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableBlend();
		RenderSystem.enableCull();
		RenderSystem.depthMask(true);
	}

	private static void drawDome(Matrix4f matrix, float[] fog) {
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		for (int ring = 0; ring < ELEVATIONS.length - 1; ring++) {
			float[] c0 = ringColor(ring, fog), c1 = ringColor(ring + 1, fog);
			float e0 = ELEVATIONS[ring] * Mth.DEG_TO_RAD, e1 = ELEVATIONS[ring + 1] * Mth.DEG_TO_RAD;
			for (int s = 0; s < SEGMENTS; s++) {
				float a0 = s * Mth.TWO_PI / SEGMENTS, a1 = (s + 1) * Mth.TWO_PI / SEGMENTS;
				vertex(buffer, matrix, e0, a0, c0);
				vertex(buffer, matrix, e0, a1, c0);
				vertex(buffer, matrix, e1, a1, c1);
				vertex(buffer, matrix, e1, a0, c1);
			}
		}
		BufferUploader.drawWithShader(buffer.end());
	}

	private static float[] ringColor(int ring, float[] fog) {
		int rgb = COLORS[ring];
		if (rgb == -1) {
			// Gold haze at the horizon, blended into the fog so distant terrain meets the sky without a seam.
			float t = ring == 4 ? 0.65f : 1.0f;
			return new float[]{Mth.lerp(t, 0.42f, fog[0]), Mth.lerp(t, 0.36f, fog[1]), Mth.lerp(t, 0.27f, fog[2]), 1};
		}
		if (rgb == -2) return new float[]{fog[0] * 0.55f, fog[1] * 0.55f, fog[2] * 0.55f, 1};
		return new float[]{(rgb >> 16 & 255) / 255f, (rgb >> 8 & 255) / 255f, (rgb & 255) / 255f, 1};
	}

	private static void vertex(BufferBuilder buffer, Matrix4f matrix, float elevation, float azimuth, float[] c) {
		float cos = Mth.cos(elevation);
		buffer.vertex(matrix, Mth.sin(azimuth) * cos * RADIUS, Mth.sin(elevation) * RADIUS, -Mth.cos(azimuth) * cos * RADIUS)
				.color(c[0], c[1], c[2], c[3]).endVertex();
	}

	private static void drawStars(Matrix4f matrix, float time) {
		if (stars == null) stars = buildStars();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		for (int i = 0; i < STAR_COUNT; i++) {
			int o = i * 13;
			float twinkle = 0.55f + 0.45f * Mth.sin(time * 0.05f + stars[o + 12] * 40f);
			float alpha = stars[o + 12] * twinkle * 0.55f;
			for (int v = 0; v < 4; v++) {
				buffer.vertex(matrix, stars[o + v * 3], stars[o + v * 3 + 1], stars[o + v * 3 + 2]).color(0.95f, 0.9f, 0.8f, alpha).endVertex();
			}
		}
		BufferUploader.drawWithShader(buffer.end());
	}

	/** Star quads facing the origin, upper sky only; the 13th float is the star's base brightness. */
	private static float[] buildStars() {
		RandomSource random = RandomSource.create(0x7E11C1A5L);
		float[] out = new float[STAR_COUNT * 13];
		int i = 0;
		while (i < STAR_COUNT) {
			float x = random.nextFloat() * 2 - 1, y = random.nextFloat(), z = random.nextFloat() * 2 - 1;
			float len = Mth.sqrt(x * x + y * y + z * z);
			if (len < 0.05f || len > 1 || y / len < 0.12f) continue;
			x /= len;
			y /= len;
			z /= len;
			float size = 0.12f + random.nextFloat() * 0.18f;
			// Two tangents perpendicular to the star's direction.
			float tx = -z, tz = x;
			float tl = Mth.sqrt(tx * tx + tz * tz);
			tx /= tl;
			tz /= tl;
			float bx = y * tz, by = z * tx - x * tz, bz = -y * tx;
			int o = i * 13;
			float[][] corners = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};
			for (int v = 0; v < 4; v++) {
				float u = corners[v][0] * size, w = corners[v][1] * size;
				out[o + v * 3] = x * RADIUS + tx * u + bx * w;
				out[o + v * 3 + 1] = y * RADIUS + by * w;
				out[o + v * 3 + 2] = z * RADIUS + tz * u + bz * w;
			}
			out[o + 12] = 0.35f + random.nextFloat() * 0.65f;
			i++;
		}
		return out;
	}

	private static void drawGracetree(Matrix4f matrix, float time) {
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderTexture(0, GRACETREE);
		float shimmer = 0.78f + 0.08f * Mth.sin(time * 0.021f) + 0.04f * Mth.sin(time * 0.067f + 1.7f);
		RenderSystem.setShaderColor(1.0f, 0.93f, 0.72f, shimmer);
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		float half = 46, bottom = -14, top = 78, z = -RADIUS;
		buffer.vertex(matrix, -half, bottom, z).uv(0, 1).endVertex();
		buffer.vertex(matrix, half, bottom, z).uv(1, 1).endVertex();
		buffer.vertex(matrix, half, top, z).uv(1, 0).endVertex();
		buffer.vertex(matrix, -half, top, z).uv(0, 0).endVertex();
		BufferUploader.drawWithShader(buffer.end());
		RenderSystem.setShaderColor(1, 1, 1, 1);
	}

	private static void drawEclipse(PoseStack poseStack, float time) {
		poseStack.pushPose();
		poseStack.mulPose(Axis.XP.rotationDegrees(38));
		Matrix4f matrix = poseStack.last().pose();
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderTexture(0, ECLIPSE);
		// The black disc and corona over the sky.
		RenderSystem.setShaderColor(1, 1, 1, 1);
		eclipseQuad(matrix, 26);
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
		// Two additive passes: the corona breathes, and a slightly larger copy lets it flare past its rim.
		float breath = 0.45f + 0.25f * Mth.sin(time * 0.035f);
		RenderSystem.setShaderColor(1.0f, 0.9f, 0.7f, breath);
		eclipseQuad(matrix, 26);
		RenderSystem.setShaderColor(1.0f, 0.82f, 0.55f, 0.18f + 0.08f * Mth.sin(time * 0.05f + 2));
		eclipseQuad(matrix, 31);
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShaderColor(1, 1, 1, 1);
		poseStack.popPose();
	}

	private static void eclipseQuad(Matrix4f matrix, float size) {
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		buffer.vertex(matrix, -size, RADIUS, -size).uv(0, 0).endVertex();
		buffer.vertex(matrix, size, RADIUS, -size).uv(1, 0).endVertex();
		buffer.vertex(matrix, size, RADIUS, size).uv(1, 1).endVertex();
		buffer.vertex(matrix, -size, RADIUS, size).uv(0, 1).endVertex();
		BufferUploader.drawWithShader(buffer.end());
	}
}
