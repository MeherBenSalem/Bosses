package com.nightbeam.reel.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** The attack and use actions a mouse click would trigger. */
@Mixin(Minecraft.class)
public interface MinecraftInvoker {
	@Invoker("startAttack")
	boolean reel$startAttack();

	@Invoker("startUseItem")
	void reel$startUseItem();
}
