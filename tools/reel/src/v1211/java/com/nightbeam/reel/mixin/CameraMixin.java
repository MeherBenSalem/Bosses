package com.nightbeam.reel.mixin;

import com.nightbeam.reel.ReelCamera;
import com.nightbeam.reel.ReelDirector;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Places the render camera where the director says. Priority 500 runs this before the mod's own camera-shake
 * mixin, so boss shake still lands on top of a scripted camera. Roll is applied on every rotation update.
 */
@Mixin(value = Camera.class, priority = 500)
public abstract class CameraMixin {
	@Shadow private boolean detached;
	@Shadow @Final private Quaternionf rotation;
	@Shadow @Final private Vector3f forwards;
	@Shadow @Final private Vector3f up;
	@Shadow @Final private Vector3f left;

	@Shadow protected abstract void setRotation(float yRot, float xRot);

	@Shadow protected abstract void setPosition(double x, double y, double z);

	@Shadow public abstract net.minecraft.world.phys.Vec3 getPosition();

	@Inject(method = "setup", at = @At("HEAD"))
	private void reel$aim(BlockGetter level, Entity entity, boolean detachedIn, boolean mirrored, float partialTick, CallbackInfo ci) {
		ReelDirector.beforeCamera(partialTick);
	}

	@Inject(method = "setup", at = @At("TAIL"))
	private void reel$place(BlockGetter level, Entity entity, boolean detachedIn, boolean mirrored, float partialTick, CallbackInfo ci) {
		if (ReelCamera.active) {
			ReelDirector.placeCamera(partialTick);
			detached = ReelCamera.detached;
			setPosition(ReelCamera.x, ReelCamera.y, ReelCamera.z);
			setRotation(ReelCamera.yaw, ReelCamera.pitch);
		} else {
			ReelCamera.x = getPosition().x;
			ReelCamera.y = getPosition().y;
			ReelCamera.z = getPosition().z;
		}
	}

	@Inject(method = "setRotation", at = @At("TAIL"))
	private void reel$roll(float yRot, float xRot, CallbackInfo ci) {
		if (ReelCamera.roll == 0) return;
		rotation.rotateZ((float) Math.toRadians(ReelCamera.roll));
		new Vector3f(0, 0, -1).rotate(rotation, forwards);
		new Vector3f(0, 1, 0).rotate(rotation, up);
		new Vector3f(-1, 0, 0).rotate(rotation, left);
	}
}
