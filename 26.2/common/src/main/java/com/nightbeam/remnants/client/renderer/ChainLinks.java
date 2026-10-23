package com.nightbeam.remnants.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * Draws a vanilla-looking chain between two points: two crossed 3-pixel strips with the iron chain block's own
 * texture and UVs, one texture repeat per block of length. Used by the Warden's thrown chain and by the
 * Chain Wall instead of new model assets.
 */
public final class ChainLinks {
	public static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/block/iron_chain.png");
	private static final float HALF_WIDTH = 1.5f / 16f;

	private ChainLinks() {
	}

	/** {@code from} and {@code to} are relative to the pose's origin (the rendered entity). */
	public static void submit(PoseStack poseStack, SubmitNodeCollector collector, Vec3 from, Vec3 to, int light) {
		Vec3 span = to.subtract(from);
		double length = span.length();
		if (length < 1.0E-3) return;
		Vec3 dir = span.scale(1 / length);
		Vec3 side0 = dir.cross(new Vec3(0, 1, 0));
		if (side0.lengthSqr() < 1.0E-4) side0 = dir.cross(new Vec3(1, 0, 0));
		Vec3 side = side0.normalize();
		Vec3 other = dir.cross(side).normalize();
		collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TEXTURE), (pose, consumer) -> {
			// Whole blocks first, then the remainder with a cropped V range, so the links never stretch.
			for (double start = 0; start < length; start += 1) {
				double end = Math.min(length, start + 1);
				Vec3 a = from.add(dir.scale(start)), b = from.add(dir.scale(end));
				float v1 = (float) (end - start);
				strip(consumer, pose, a, b, side, 0f, 3f / 16f, v1, light);
				strip(consumer, pose, a, b, other, 3f / 16f, 6f / 16f, v1, light);
			}
		});
	}

	private static void strip(VertexConsumer consumer, PoseStack.Pose pose, Vec3 a, Vec3 b, Vec3 across, float u0, float u1,
			float v1, int light) {
		Vec3 w = across.scale(HALF_WIDTH);
		Vec3 normal = across.cross(b.subtract(a)).normalize();
		vertex(consumer, pose, a.subtract(w), u0, v1, normal, light);
		vertex(consumer, pose, a.add(w), u1, v1, normal, light);
		vertex(consumer, pose, b.add(w), u1, 0, normal, light);
		vertex(consumer, pose, b.subtract(w), u0, 0, normal, light);
	}

	private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 p, float u, float v, Vec3 n, int light) {
		consumer.addVertex(pose, (float) p.x, (float) p.y, (float) p.z)
				.setColor(255, 255, 255, 255)
				.setUv(u, v)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(light)
				.setNormal(pose, (float) n.x, (float) n.y, (float) n.z);
	}
}
