package com.nightbeam.remnants.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.nightbeam.remnants.entity.GraveboundWardenEntity;
import com.nightbeam.remnants.entity.WardenChainEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * {@code remnants:warden_chain}: a chain from the thrower's hand (the Warden's left shackle, or the
 * player's weapon hand for Warden's Grasp) to the hook, which rides on whatever it has latched onto.
 */
public class WardenChainRenderer extends EntityRenderer<WardenChainEntity> {
	public WardenChainRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.shadowRadius = 0;
	}

	@Override
	public void render(WardenChainEntity chain, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
		Entity owner = chain.getOwner();
		if (owner != null) {
			Vec3 origin = chain.getPosition(partialTick);
			Vec3 hook = new Vec3(0, chain.getBbHeight() / 2, 0);
			ChainLinks.render(poseStack, buffers, hand(owner, partialTick).subtract(origin), hook, light);
		}
		super.render(chain, yaw, partialTick, poseStack, buffers, light);
	}

	private static Vec3 hand(Entity owner, float partialTick) {
		if (owner instanceof GraveboundWardenEntity warden) {
			return warden.handPosition().add(warden.getPosition(partialTick).subtract(warden.position()));
		}
		Vec3 base = owner.getPosition(partialTick);
		float bodyYaw = owner instanceof LivingEntity living ? Mth.lerp(partialTick, living.yBodyRotO, living.yBodyRot) : owner.getYRot();
		boolean right = !(owner instanceof LivingEntity living) || living.getMainArm() == HumanoidArm.RIGHT;
		Vec3 side = Vec3.directionFromRotation(0, bodyYaw + (right ? 90 : -90));
		Vec3 forward = Vec3.directionFromRotation(0, bodyYaw);
		return base.add(side.scale(0.4)).add(forward.scale(0.35)).add(0, owner.getBbHeight() * 0.55, 0);
	}

	@Override
	public boolean shouldRender(WardenChainEntity chain, Frustum frustum, double x, double y, double z) {
		Entity owner = chain.getOwner();
		if (owner == null) return super.shouldRender(chain, frustum, x, y, z);
		return chain.shouldRenderAtSqrDistance(chain.distanceToSqr(x, y, z))
				&& frustum.isVisible(chain.getBoundingBox().minmax(owner.getBoundingBox()).inflate(0.5));
	}

	@Override
	public ResourceLocation getTextureLocation(WardenChainEntity chain) {
		return ChainLinks.TEXTURE;
	}
}
