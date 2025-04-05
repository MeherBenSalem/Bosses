
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package com.naizo.ossukage.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;

import com.naizo.ossukage.RemnantOssukageMod;

public class RemnantOssukageModSounds {
	public static final DeferredRegister<SoundEvent> REGISTRY = DeferredRegister.create(Registries.SOUND_EVENT, RemnantOssukageMod.MODID);
	public static final DeferredHolder<SoundEvent, SoundEvent> SKELETONFIGHT_THEME = REGISTRY.register("skeletonfight_theme",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("remnant_ossukage", "skeletonfight_theme")));
	public static final DeferredHolder<SoundEvent, SoundEvent> DASH_SFX = REGISTRY.register("dash_sfx", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("remnant_ossukage", "dash_sfx")));
}
