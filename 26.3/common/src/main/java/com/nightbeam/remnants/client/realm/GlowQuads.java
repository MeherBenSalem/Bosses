package com.nightbeam.remnants.client.realm;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Double-sided, full-bright quads for additive glow layers ({@code GraceEmberRenderer#glow}). The textures are
 * premultiplied (black is clear), so brightness is the vertex colour.
 */
final class GlowQuads {
	private GlowQuads() {
	}

	/** A horizontal square of half-size {@code half}, centred on the pose origin. */
	static void flat(VertexConsumer vc, PoseStack.Pose pose, float half, float brightness) {
		quad(vc, pose, -half, 0, -half, -half, 0, half, half, 0, half, half, 0, -half, 0, 1, 0, 1, brightness);
	}

	/** An upright quad in the pose's XY plane from y0 to y1, {@code half} to each side, with a UV window. */
	static void billboard(VertexConsumer vc, PoseStack.Pose pose, float half, float y0, float y1,
			float u0, float u1, float v0, float v1, float brightness) {
		quad(vc, pose, -half, y0, 0, -half, y1, 0, half, y1, 0, half, y0, 0, u0, u1, v1, v0, brightness);
	}

	private static void quad(VertexConsumer vc, PoseStack.Pose pose, float x0, float y0, float z0, float x1, float y1, float z1,
			float x2, float y2, float z2, float x3, float y3, float z3, float u0, float u1, float vBottom, float vTop, float c) {
		// corners: 0 = (u0, vBottom), 1 = (u0, vTop), 2 = (u1, vTop), 3 = (u1, vBottom); both windings
		vertex(vc, pose, x0, y0, z0, u0, vBottom, c);
		vertex(vc, pose, x1, y1, z1, u0, vTop, c);
		vertex(vc, pose, x2, y2, z2, u1, vTop, c);
		vertex(vc, pose, x3, y3, z3, u1, vBottom, c);
		vertex(vc, pose, x3, y3, z3, u1, vBottom, c);
		vertex(vc, pose, x2, y2, z2, u1, vTop, c);
		vertex(vc, pose, x1, y1, z1, u0, vTop, c);
		vertex(vc, pose, x0, y0, z0, u0, vBottom, c);
	}

	private static void vertex(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, float u, float v, float c) {
		vc.addVertex(pose, x, y, z).setColor(c, c, c, 1.0f).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(pose, 0.0f, 1.0f, 0.0f);
	}
}
