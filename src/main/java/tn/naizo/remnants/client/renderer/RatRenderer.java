package tn.naizo.remnants.client.renderer;

import tn.naizo.remnants.procedures.RatDisplayConditionProcedure;
import tn.naizo.remnants.procedures.RatDisplayCondition4Procedure;
import tn.naizo.remnants.procedures.RatDisplayCondition3Procedure;
import tn.naizo.remnants.procedures.RatDisplayCondition2Procedure;
import tn.naizo.remnants.entity.RatEntity;
import tn.naizo.remnants.client.model.animations.ratAnimation;
import tn.naizo.remnants.client.model.Modelrat;

import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.HierarchicalModel;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;

public class RatRenderer extends MobRenderer<RatEntity, Modelrat<RatEntity>> {
	public RatRenderer(EntityRendererProvider.Context context) {
		super(context, new AnimatedModel(context.bakeLayer(Modelrat.LAYER_LOCATION)), 0.5f);
		this.addLayer(new RenderLayer<RatEntity, Modelrat<RatEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = ResourceLocation.parse("remnant_bosses:textures/entities/rat.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, RatEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (RatDisplayConditionProcedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, light, LivingEntityRenderer.getOverlayCoords(entity, 0), 1, 1, 1, 1);
				}
			}
		});
		this.addLayer(new RenderLayer<RatEntity, Modelrat<RatEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = ResourceLocation.parse("remnant_bosses:textures/entities/rat_blue.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, RatEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (RatDisplayCondition2Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, light, LivingEntityRenderer.getOverlayCoords(entity, 0), 1, 1, 1, 1);
				}
			}
		});
		this.addLayer(new RenderLayer<RatEntity, Modelrat<RatEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = ResourceLocation.parse("remnant_bosses:textures/entities/rat_grey.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, RatEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (RatDisplayCondition3Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, light, LivingEntityRenderer.getOverlayCoords(entity, 0), 1, 1, 1, 1);
				}
			}
		});
		this.addLayer(new RenderLayer<RatEntity, Modelrat<RatEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = ResourceLocation.parse("remnant_bosses:textures/entities/rat_yellow.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, RatEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (RatDisplayCondition4Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, light, LivingEntityRenderer.getOverlayCoords(entity, 0), 1, 1, 1, 1);
				}
			}
		});
	}

	@Override
	public ResourceLocation getTextureLocation(RatEntity entity) {
		return ResourceLocation.parse("remnant_bosses:textures/entities/rat.png");
	}

	private static final class AnimatedModel extends Modelrat<RatEntity> {
		private final ModelPart root;
		private final HierarchicalModel animator = new HierarchicalModel<RatEntity>() {
			@Override
			public ModelPart root() {
				return root;
			}

			@Override
			public void setupAnim(RatEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
				this.root().getAllParts().forEach(ModelPart::resetPose);
				this.animate(entity.animationState0, ratAnimation.idle, ageInTicks, 1f);
				this.animateWalk(ratAnimation.walk, limbSwing, limbSwingAmount, 1f, 1f);
				this.animate(entity.animationState2, ratAnimation.attack, ageInTicks, 1f);
			}
		};

		public AnimatedModel(ModelPart root) {
			super(root);
			this.root = root;
		}

		@Override
		public void setupAnim(RatEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
			animator.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
			super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
		}
	}
}