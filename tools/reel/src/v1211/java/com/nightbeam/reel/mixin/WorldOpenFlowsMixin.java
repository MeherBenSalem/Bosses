package com.nightbeam.reel.mixin;

import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The reel world uses the mod's dimension (an "experimental" setting); reopen it without the backup prompt. */
@Mixin(WorldOpenFlows.class)
public abstract class WorldOpenFlowsMixin {
	@Inject(method = "askForBackup", at = @At("HEAD"), cancellable = true)
	private void reel$skipBackupPrompt(LevelStorageSource.LevelStorageAccess levelStorage, boolean customized, Runnable loadLevel,
			Runnable onCancel, CallbackInfo ci) {
		if (!"ReelWorld".equals(levelStorage.getLevelId())) return;
		loadLevel.run();
		ci.cancel();
	}
}
