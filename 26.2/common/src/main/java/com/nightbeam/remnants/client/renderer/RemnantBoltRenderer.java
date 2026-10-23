package com.nightbeam.remnants.client.renderer;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.nightbeam.remnants.client.model.RemnantGeoModel;
import com.nightbeam.remnants.entity.RemnantBoltEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

/**
 * A family bolt: the Memory Bolt's shard model in the family's own texture ({@code ember_bolt},
 * {@code crystal_bolt}), turned along its flight like the Memory Bolt and always lit.
 */
public class RemnantBoltRenderer<T extends RemnantBoltEntity> extends RemnantGeoRenderer<T> {
	public RemnantBoltRenderer(EntityRendererProvider.Context context, String texture) {
		super(context, new RemnantGeoModel<>("memory_bolt", texture), 0);
	}

	@Override
	public @Nullable RenderType getRenderType(EntityRenderState renderState, Identifier texture) {
		return RenderTypes.entityTranslucent(texture);
	}

	@Override
	public void addRenderData(T animatable, @Nullable Void relatedObject, EntityRenderState renderState, float partialTick) {
		faceFlight(animatable, renderState, partialTick);
	}

	@Override
	protected void applyRotations(RenderPassInfo<EntityRenderState> renderPassInfo, PoseStack poseStack, float nativeScale) {
		super.applyRotations(renderPassInfo, poseStack, nativeScale);
		pitch(renderPassInfo, poseStack);
	}

	@Override
	protected int getBlockLightLevel(T entity, BlockPos pos) {
		return 15;
	}

	/** Shards have no body rotation: GeckoLib's yaw comes from the interpolated flight yaw. */
	static <E extends Entity & GeoAnimatable> void faceFlight(E entity, EntityRenderState renderState, float partialTick) {
		renderState.addGeckolibData(DataTickets.ENTITY_BODY_YAW, Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot()));
	}

	/** The model points along -Z; a positive pitch (falling bolt) tips its nose down. */
	static void pitch(RenderPassInfo<EntityRenderState> renderPassInfo, PoseStack poseStack) {
		float pitch = renderPassInfo.renderState().getOrDefaultGeckolibData(DataTickets.ENTITY_PITCH, 0f);
		poseStack.translate(0, 0.25, 0);
		poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
		poseStack.translate(0, -0.25, 0);
	}
}
