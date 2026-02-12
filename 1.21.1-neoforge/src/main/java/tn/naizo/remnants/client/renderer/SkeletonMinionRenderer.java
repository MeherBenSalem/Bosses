package tn.naizo.remnants.client.renderer;

import tn.naizo.remnants.entity.SkeletonMinionEntity;
import tn.naizo.remnants.client.model.animations.skeleton_minionAnimation;
import tn.naizo.remnants.client.model.Modelskeleton_minion;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.HierarchicalModel;

public class SkeletonMinionRenderer extends MobRenderer<SkeletonMinionEntity, Modelskeleton_minion<SkeletonMinionEntity>> {
	public SkeletonMinionRenderer(EntityRendererProvider.Context context) {
		super(context, new AnimatedModel(context.bakeLayer(Modelskeleton_minion.LAYER_LOCATION)), 0.5f);
	}

	@Override
	public ResourceLocation getTextureLocation(SkeletonMinionEntity entity) {
		return ResourceLocation.parse("remnant_bosses:textures/entities/java_skeleton.png");
	}

	private static final class AnimatedModel extends Modelskeleton_minion<SkeletonMinionEntity> {
		private final ModelPart root;
		private final HierarchicalModel animator = new HierarchicalModel<SkeletonMinionEntity>() {
			@Override
			public ModelPart root() {
				return root;
			}

			@Override
			public void setupAnim(SkeletonMinionEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
				this.root().getAllParts().forEach(ModelPart::resetPose);
				this.animate(entity.animationState0, skeleton_minionAnimation.idle, ageInTicks, 1f);
				this.animateWalk(skeleton_minionAnimation.walk, limbSwing, limbSwingAmount, 1f, 1f);
				this.animate(entity.animationState2, skeleton_minionAnimation.attack, ageInTicks, 1f);
				this.animate(entity.animationState3, skeleton_minionAnimation.spawn, ageInTicks, 1f);
			}
		};

		public AnimatedModel(ModelPart root) {
			super(root);
			this.root = root;
		}

		@Override
		public void setupAnim(SkeletonMinionEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
			animator.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
			super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
		}
	}
}