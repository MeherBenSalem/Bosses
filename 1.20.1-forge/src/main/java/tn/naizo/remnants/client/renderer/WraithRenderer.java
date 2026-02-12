package tn.naizo.remnants.client.renderer;

import tn.naizo.remnants.entity.WraithEntity;
import tn.naizo.remnants.client.model.animations.wraithAnimation;
import tn.naizo.remnants.client.model.Modelwraith;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.HierarchicalModel;

public class WraithRenderer extends MobRenderer<WraithEntity, Modelwraith<WraithEntity>> {
	public WraithRenderer(EntityRendererProvider.Context context) {
		super(context, new AnimatedModel(context.bakeLayer(Modelwraith.LAYER_LOCATION)), 0.5f);
	}

	@Override
	public ResourceLocation getTextureLocation(WraithEntity entity) {
		return ResourceLocation.parse("remnant_bosses:textures/entities/wraith.png");
	}

	private static final class AnimatedModel extends Modelwraith<WraithEntity> {
		private final ModelPart root;
		private final HierarchicalModel animator = new HierarchicalModel<WraithEntity>() {
			@Override
			public ModelPart root() {
				return root;
			}

			@Override
			public void setupAnim(WraithEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
				this.root().getAllParts().forEach(ModelPart::resetPose);
				this.animate(entity.animationState0, wraithAnimation.idle, ageInTicks, 1f);
				this.animateWalk(wraithAnimation.walk, limbSwing, limbSwingAmount, 1f, 1f);
				this.animate(entity.animationState2, wraithAnimation.attack, ageInTicks, 1f);
				this.animate(entity.animationState3, wraithAnimation.death, ageInTicks, 1f);
			}
		};

		public AnimatedModel(ModelPart root) {
			super(root);
			this.root = root;
		}

		@Override
		public void setupAnim(WraithEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
			animator.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
			super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
		}
	}
}
