package com.nightbeam.remnants.mixin;

import com.nightbeam.remnants.realm.RealmProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** No block breaking in the Realm: the same check adventure mode uses, so the client never starts mining. */
@Mixin(Player.class)
public abstract class RealmBuildMixin {
	@Inject(method = "blockActionRestricted", at = @At("HEAD"), cancellable = true)
	private void remnant$realmProtected(Level level, BlockPos pos, GameType gameMode, CallbackInfoReturnable<Boolean> cir) {
		Player player = (Player) (Object) this;
		if (RealmProtection.denies(player, level)) {
			RealmProtection.warn(player);
			cir.setReturnValue(true);
		}
	}
}
