package com.nightbeam.remnants.init;

import com.nightbeam.remnants.RemnantBossesMod;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;

public class ModSounds {
	public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT,
			RemnantBossesMod.MODID);

	public static final DeferredHolder<SoundEvent, SoundEvent> SKELETONFIGHT_THEME = SOUNDS.register(
			"skeletonfight_theme",
			() -> SoundEvent.createVariableRangeEvent(
					ResourceLocation.fromNamespaceAndPath("remnant_bosses", "skeletonfight_theme")));

	public static final DeferredHolder<SoundEvent, SoundEvent> DASH_SFX = SOUNDS.register("dash_sfx",
			() -> SoundEvent
					.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("remnant_bosses", "dash_sfx")));

	public static final DeferredHolder<SoundEvent, SoundEvent> ARMORED_GRUB_AMBIENT = SOUNDS.register("armored_grub_ambient",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("remnant_bosses", "armored_grub_ambient")));

	public static final DeferredHolder<SoundEvent, SoundEvent> ARMORED_GRUB_DEATH = SOUNDS.register("armored_grub_death",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("remnant_bosses", "armored_grub_death")));
}