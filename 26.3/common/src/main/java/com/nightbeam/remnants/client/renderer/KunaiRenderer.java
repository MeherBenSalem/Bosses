package com.nightbeam.remnants.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.nightbeam.remnants.client.model.Modelshuriken;
import com.nightbeam.remnants.entity.KunaiEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class KunaiRenderer extends EntityRenderer<KunaiEntity, KunaiRenderer.State> {
	private static final Identifier TEXTURE = Identifier.parse("remnant_bosses:textures/entities/kunai.png");
	private final Modelshuriken model;

	public KunaiRenderer(EntityRendererProvider.Context context) {
		super(context);
		model = new Modelshuriken(context.bakeLayer(Modelshuriken.LAYER_LOCATION));
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(KunaiEntity kunai, State state, float partialTicks) {
		super.extractRenderState(kunai, state, partialTicks);
		state.yRot = Mth.lerp(partialTicks, kunai.yRotO, kunai.getYRot());
		state.xRot = Mth.lerp(partialTicks, kunai.xRotO, kunai.getXRot());
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		poseStack.pushPose();
		poseStack.rotate(Axis.YP.rotationDegrees(state.yRot - 90));
		poseStack.rotate(Axis.ZP.rotationDegrees(90 + state.xRot));
		submitNodeCollector.submitModelPart(model.group, poseStack, RenderTypes.entityCutout(TEXTURE), state.lightCoords,
				OverlayTexture.NO_OVERLAY, null);
		poseStack.popPose();
		super.submit(state, poseStack, submitNodeCollector, camera);
	}

	public static class State extends EntityRenderState {
		float yRot, xRot;
	}
}
