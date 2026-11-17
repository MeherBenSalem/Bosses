package com.nightbeam.reel.mixin;

import com.nightbeam.reel.ReelClock;
import com.nightbeam.reel.ReelDirector;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.BooleanSupplier;

/** Lockstep: each server tick waits for the client's permit; chunk work may use the wait. */
@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
	@Inject(method = "tickServer", at = @At("HEAD"))
	private void reel$awaitPermit(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
		MinecraftServer server = (MinecraftServer) (Object) this;
		ReelClock.awaitPermit(server);
		ReelDirector.serverTick(server);
	}

	@Inject(method = "tickServer", at = @At("TAIL"))
	private void reel$tickDone(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
		ReelClock.tickDone();
	}

	@Inject(method = "haveTime", at = @At("HEAD"), cancellable = true)
	private void reel$haveTime(CallbackInfoReturnable<Boolean> cir) {
		if (ReelClock.serverWaiting) cir.setReturnValue(true);
	}
}
