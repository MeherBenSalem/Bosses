package com.nightbeam.remnants.client.realm;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.nightbeam.remnants.Constants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.attribute.EnvironmentAttributes;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Consumer;

/**
 * The Eclipsed Realm's sky: a slate-to-ash-gold dome, faint stars, the Gracetree on the
 * northern horizon and the black eclipse with its breathing gold corona high in the south.
 * Drawn in the vanilla sky pass ({@code SkyRendererMixin}) with vanilla pipelines, so it works on every graphics backend.
 */
public final class EclipseSkyRenderer {
	private static final Identifier ECLIPSE = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/environment/eclipse.png");
	private static final Identifier GRACETREE = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/environment/gracetree.png");
	private static final float RADIUS = 100f;
	private static final int SEGMENTS = 32;
	/** Ring elevations (degrees) and their colours; -1 is the gold horizon haze blended into the fog, -2 a darkened fog colour. */
	private static final float[] ELEVATIONS = {90, 58, 32, 14, 4, 0, -12, -90};
	private static final int[] COLORS = {0x0E111C, 0x161B2A, 0x262838, 0x4A4236, -1, -1, -2, 0x08080B};
	private static final int STAR_COUNT = 700;
	private static float @Nullable [] stars;
	private static final Mesh DOME = new Mesh("Eclipse dome", DefaultVertexFormat.POSITION_COLOR, (ELEVATIONS.length - 1) * SEGMENTS * 4);
	private static final Mesh STARS = new Mesh("Eclipse stars", DefaultVertexFormat.POSITION_COLOR, STAR_COUNT * 4);
	private static final Mesh GRACETREE_QUAD = new Mesh("Gracetree", DefaultVertexFormat.POSITION_TEX, 8);
	private static final Mesh ECLIPSE_QUAD = new Mesh("Eclipse disc", DefaultVertexFormat.POSITION_TEX_COLOR, 8);
	private static final Mesh CORONA_QUAD = new Mesh("Eclipse corona", DefaultVertexFormat.POSITION_TEX, 8);
	private static final Mesh CORONA_FLARE = new Mesh("Eclipse corona flare", DefaultVertexFormat.POSITION_TEX, 8);

	private EclipseSkyRenderer() {
	}

	/** Called from the sky pass instead of the vanilla sky; the model-view stack holds the camera rotation. */
	public static void render(ClientLevel level, float partialTick) {
		Minecraft minecraft = Minecraft.getInstance();
		int fogColor = minecraft.gameRenderer.mainCamera().attributeProbe().getValue(EnvironmentAttributes.FOG_COLOR, partialTick);
		float[] fog = {ARGB.redFloat(fogColor), ARGB.greenFloat(fogColor), ARGB.blueFloat(fogColor)};
		float time = level.getGameTime() + partialTick;
		RenderTarget target = minecraft.gameRenderer.mainRenderTarget();
		Matrix4f view = new Matrix4f(RenderSystem.getModelViewStack());

		DOME.draw(target, RenderPipelines.DEBUG_QUADS, view, null, white(1), buffer -> drawDome(buffer, fog));
		STARS.draw(target, RenderPipelines.DEBUG_QUADS, view, null, white(1), buffer -> drawStars(buffer, time));

		float shimmer = 0.78f + 0.08f * Mth.sin(time * 0.021f) + 0.04f * Mth.sin(time * 0.067f + 1.7f);
		AbstractTexture gracetree = minecraft.getTextureManager().getTexture(GRACETREE);
		GRACETREE_QUAD.draw(target, RenderPipelines.CELESTIAL, view, gracetree, new Vector4f(1.0f, 0.93f, 0.72f, shimmer),
				EclipseSkyRenderer::drawGracetree);

		// The black disc and corona over the sky, tilted toward the south.
		PoseStack pose = new PoseStack();
		pose.mulPose(Axis.XP.rotationDegrees(38));
		Matrix4f eclipseView = new Matrix4f(view).mul(pose.last().pose());
		AbstractTexture eclipse = minecraft.getTextureManager().getTexture(ECLIPSE);
		ECLIPSE_QUAD.draw(target, RenderPipelines.END_SKY, eclipseView, eclipse, white(1), buffer -> eclipseQuad(buffer, 26, true));
		// Two additive passes: the corona breathes, and a slightly larger copy lets it flare past its rim.
		float breath = 0.45f + 0.25f * Mth.sin(time * 0.035f);
		CORONA_QUAD.draw(target, RenderPipelines.CELESTIAL, eclipseView, eclipse, new Vector4f(1.0f, 0.9f, 0.7f, breath),
				buffer -> eclipseQuad(buffer, 26, false));
		CORONA_FLARE.draw(target, RenderPipelines.CELESTIAL, eclipseView, eclipse,
				new Vector4f(1.0f, 0.82f, 0.55f, 0.18f + 0.08f * Mth.sin(time * 0.05f + 2)), buffer -> eclipseQuad(buffer, 31, false));
	}

	private static Vector4f white(float alpha) {
		return new Vector4f(1, 1, 1, alpha);
	}

	private static void drawDome(BufferBuilder buffer, float[] fog) {
		for (int ring = 0; ring < ELEVATIONS.length - 1; ring++) {
			float[] c0 = ringColor(ring, fog), c1 = ringColor(ring + 1, fog);
			float e0 = ELEVATIONS[ring] * Mth.DEG_TO_RAD, e1 = ELEVATIONS[ring + 1] * Mth.DEG_TO_RAD;
			for (int s = 0; s < SEGMENTS; s++) {
				float a0 = s * Mth.TWO_PI / SEGMENTS, a1 = (s + 1) * Mth.TWO_PI / SEGMENTS;
				vertex(buffer, e0, a0, c0);
				vertex(buffer, e0, a1, c0);
				vertex(buffer, e1, a1, c1);
				vertex(buffer, e1, a0, c1);
			}
		}
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

	private static void vertex(BufferBuilder buffer, float elevation, float azimuth, float[] c) {
		float cos = Mth.cos(elevation);
		buffer.addVertex(Mth.sin(azimuth) * cos * RADIUS, Mth.sin(elevation) * RADIUS, -Mth.cos(azimuth) * cos * RADIUS)
				.setColor(c[0], c[1], c[2], c[3]);
	}

	private static void drawStars(BufferBuilder buffer, float time) {
		if (stars == null) stars = buildStars();
		for (int i = 0; i < STAR_COUNT; i++) {
			int o = i * 13;
			float twinkle = 0.55f + 0.45f * Mth.sin(time * 0.05f + stars[o + 12] * 40f);
			float alpha = stars[o + 12] * twinkle * 0.55f;
			for (int v = 0; v < 4; v++) {
				buffer.addVertex(stars[o + v * 3], stars[o + v * 3 + 1], stars[o + v * 3 + 2]).setColor(0.95f, 0.9f, 0.8f, alpha);
			}
		}
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

	/** Both windings, so the quad shows whichever way the pipeline culls. */
	private static void drawGracetree(BufferBuilder buffer) {
		float half = 46, bottom = -14, top = 78, z = -RADIUS;
		float[][] corners = {{-half, bottom, 0, 1}, {half, bottom, 1, 1}, {half, top, 1, 0}, {-half, top, 0, 0}};
		for (int i = 0; i < 4; i++) buffer.addVertex(corners[i][0], corners[i][1], z).setUv(corners[i][2], corners[i][3]);
		for (int i = 3; i >= 0; i--) buffer.addVertex(corners[i][0], corners[i][1], z).setUv(corners[i][2], corners[i][3]);
	}

	private static void eclipseQuad(BufferBuilder buffer, float size, boolean colored) {
		float[][] corners = {{-size, -size, 0, 0}, {size, -size, 1, 0}, {size, size, 1, 1}, {-size, size, 0, 1}};
		for (int pass = 0; pass < 2; pass++) {
			for (int k = 0; k < 4; k++) {
				float[] c = corners[pass == 0 ? k : 3 - k];
				buffer.addVertex(c[0], RADIUS, c[1]).setUv(c[2], c[3]);
				if (colored) buffer.setColor(1f, 1f, 1f, 1f);
			}
		}
	}

	/** A per-frame vertex buffer: built on the CPU, copied into one reused GPU buffer, drawn as quads. */
	private static final class Mesh {
		private final String label;
		private final VertexFormat format;
		private final ByteBufferBuilder bytes;
		private @Nullable GpuBuffer gpu;

		Mesh(String label, VertexFormat format, int maxVertices) {
			this.label = label;
			this.format = format;
			this.bytes = new ByteBufferBuilder(maxVertices * format.getVertexSize());
		}

		void draw(RenderTarget target, RenderPipeline pipeline, Matrix4f modelView, @Nullable AbstractTexture texture, Vector4f color,
				Consumer<BufferBuilder> build) {
			BufferBuilder buffer = new BufferBuilder(bytes, PrimitiveTopology.QUADS, format);
			build.accept(buffer);
			try (MeshData mesh = buffer.build()) {
				if (mesh == null) return;
				int size = mesh.vertexBuffer().remaining();
				if (gpu == null || gpu.size() < size) {
					if (gpu != null) gpu.close();
					gpu = RenderSystem.getDevice().createBuffer(() -> label, GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST, size);
				}
				RenderSystem.getDevice().createCommandEncoder().writeToBuffer(gpu.slice(0, size), mesh.vertexBuffer());
				int indexCount = mesh.drawState().indexCount();
				RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
				GpuBuffer indexBuffer = indices.getBuffer(indexCount);
				GpuBufferSlice transforms = RenderSystem.getDynamicUniforms().writeTransform(new Matrix4f(modelView), color);
				try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
						.createRenderPass(() -> label, target.getColorTextureView(), Optional.empty(), target.getDepthTextureView(), OptionalDouble.empty())) {
					pass.setPipeline(pipeline);
					RenderSystem.bindDefaultUniforms(pass);
					pass.setUniform("DynamicTransforms", transforms);
					if (texture != null) pass.bindTexture("Sampler0", texture.getTextureView(), texture.getSampler());
					pass.setVertexBuffer(0, gpu.slice(0, size));
					pass.setIndexBuffer(indexBuffer, indices.type());
					pass.drawIndexed(indexCount, 1, 0, 0, 0);
				}
			}
		}
	}
}
