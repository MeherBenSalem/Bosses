package com.nightbeam.remnants.mixin;

import com.nightbeam.remnants.realm.RealmProtection;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Buckets neither pour nor scoop in the Realm. */
@Mixin(BucketItem.class)
public abstract class RealmBucketMixin {
	@Inject(method = "use", at = @At("HEAD"), cancellable = true)
	private void remnant$realmProtected(Level level, Player player, InteractionHand hand,
			CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
		if (RealmProtection.denies(player, level)) {
			RealmProtection.warn(player);
			cir.setReturnValue(InteractionResultHolder.fail(player.getItemInHand(hand)));
		}
	}
}
