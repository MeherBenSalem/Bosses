package com.nightbeam.remnants.mixin;

import com.nightbeam.remnants.realm.RealmProtection;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * No placing or reshaping blocks in the Realm. Blocks are still used first (graces, Waygates, doors, chests):
 * this only stops the item's own use on a block, such as placing it or tilling, burning or stripping.
 */
@Mixin(ItemStack.class)
public abstract class RealmItemUseMixin {
	@Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
	private void remnant$realmProtected(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
		ItemStack stack = (ItemStack) (Object) this;
		if (RealmProtection.denies(context.getPlayer(), context.getLevel()) && !stack.is(RealmProtection.USABLE)) {
			RealmProtection.warn(context.getPlayer());
			cir.setReturnValue(InteractionResult.FAIL);
		}
	}
}
