package tn.naizo.remnants.client.renderer;

import tn.naizo.remnants.entity.RemnantOssukageEntity;
import tn.naizo.remnants.client.model.animations.skeleton_ninjaAnimation;
import tn.naizo.remnants.client.model.Modelskeleton_ninja;

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

public class RemnantOssukageRenderer
		extends MobRenderer<RemnantOssukageEntity, Modelskeleton_ninja<RemnantOssukageEntity>> {
	public RemnantOssukageRenderer(EntityRendererProvider.Context context) {
		super(context, new AnimatedModel(context.bakeLayer(Modelskeleton_ninja.LAYER_LOCATION)), 0.5f);
		this.addLayer(new RenderLayer<RemnantOssukageEntity, Modelskeleton_ninja<RemnantOssukageEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = ResourceLocation
					.parse("remnant_bosses:textures/entities/ninja_dead.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light,
					RemnantOssukageEntity entity, float limbSwing, float limbSwingAmount, float partialTicks,
					float ageInTicks, float netHeadYaw, float headPitch) {
				VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
				this.getParentModel().renderToBuffer(poseStack, vertexConsumer, light,
						LivingEntityRenderer.getOverlayCoords(entity, 0), -1);
			}
		});
		this.addLayer(new RenderLayer<RemnantOssukageEntity, Modelskeleton_ninja<RemnantOssukageEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = ResourceLocation
					.parse("remnant_bosses:textures/entities/ninja_skeleton_orignal.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light,
					RemnantOssukageEntity entity, float limbSwing, float limbSwingAmount, float partialTicks,
					float ageInTicks, float netHeadYaw, float headPitch) {
				if ((entity.isTransformed() == true)) {
					VertexConsumer vertexConsumer = bufferSource
							.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, light,
							LivingEntityRenderer.getOverlayCoords(entity, 0), -1);
				}
			}
		});
	}

	@Override
	public ResourceLocation getTextureLocation(RemnantOssukageEntity entity) {
		return ResourceLocation.parse("remnant_bosses:textures/entities/ninja_dead.png");
	}

	private static final class AnimatedModel extends Modelskeleton_ninja<RemnantOssukageEntity> {
		private final ModelPart root;
		private final HierarchicalModel animator = new HierarchicalModel<RemnantOssukageEntity>() {
			@Override
			public ModelPart root() {
				return root;
			}

			@Override
			public void setupAnim(RemnantOssukageEntity entity, float limbSwing, float limbSwingAmount,
					float ageInTicks, float netHeadYaw, float headPitch) {
				this.root().getAllParts().forEach(ModelPart::resetPose);
				this.animate(entity.animationState0, skeleton_ninjaAnimation.idle, ageInTicks, 1f);
				this.animateWalk(skeleton_ninjaAnimation.walk, limbSwing, limbSwingAmount, 1f, 1f);
				this.animate(entity.animationState2, skeleton_ninjaAnimation.attack, ageInTicks, 1f);
				this.animate(entity.animationState3, skeleton_ninjaAnimation.death, ageInTicks, 1f);
				this.animate(entity.animationState4, skeleton_ninjaAnimation.leap_attack, ageInTicks, 1f);
				this.animate(entity.animationState5, skeleton_ninjaAnimation.spawn, ageInTicks, 1f);
			}
		};

		public AnimatedModel(ModelPart root) {
			super(root);
			this.root = root;
		}

		@Override
		public void setupAnim(RemnantOssukageEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
				float netHeadYaw, float headPitch) {
			animator.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
			super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
		}
	}
}