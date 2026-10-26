package com.nightbeam.reel.mixin;

import com.nightbeam.reel.ReelSounds;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Every sound the game starts, logged for the reel's audio mix. */
@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {
	@Shadow @Final private SoundManager soundManager;

	@Inject(method = "play", at = @At("HEAD"))
	private void reel$log(SoundInstance sound, CallbackInfo ci) {
		ReelSounds.played(sound, soundManager);
	}
}
