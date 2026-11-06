package com.nightbeam.reel.mixin;

import com.nightbeam.reel.ReelClock;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Under time control the frame advances by the director's tick step, not by wall-clock milliseconds. */
@Mixin(DeltaTracker.Timer.class)
public abstract class TimerMixin {
	@Shadow private float deltaTicks;
	@Shadow private float deltaTickResidual;
	@Shadow private float realtimeDeltaTicks;
	@Shadow private long lastMs;
	@Shadow private long lastUiMs;

	@Inject(method = "advanceTime", at = @At("HEAD"), cancellable = true)
	private void reel$advance(long time, boolean advanceGameTime, CallbackInfoReturnable<Integer> cir) {
		if (!ReelClock.timeControl) return;
		lastMs = time;
		lastUiMs = time;
		float step = (float) ReelClock.ticksThisFrame;
		realtimeDeltaTicks = step;
		if (!advanceGameTime) {
			cir.setReturnValue(0);
			return;
		}
		float[] residual = new float[1];
		int ticks = ReelClock.advance(residual);
		deltaTicks = step;
		deltaTickResidual = residual[0];
		cir.setReturnValue(ticks);
	}
}
