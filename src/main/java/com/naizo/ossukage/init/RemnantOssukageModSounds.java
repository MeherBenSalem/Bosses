
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package com.naizo.ossukage.init;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;

import com.naizo.ossukage.RemnantOssukageMod;

public class RemnantOssukageModSounds {
	public static final DeferredRegister<SoundEvent> REGISTRY = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, RemnantOssukageMod.MODID);
	public static final RegistryObject<SoundEvent> SKELETONFIGHT_THEME = REGISTRY.register("skeletonfight_theme", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("remnant_ossukage", "skeletonfight_theme")));
	public static final RegistryObject<SoundEvent> DASH_SFX = REGISTRY.register("dash_sfx", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("remnant_ossukage", "dash_sfx")));
}
