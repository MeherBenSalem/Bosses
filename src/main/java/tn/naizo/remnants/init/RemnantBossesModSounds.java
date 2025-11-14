/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package tn.naizo.remnants.init;

import tn.naizo.remnants.RemnantBossesMod;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;

public class RemnantBossesModSounds {
	public static final DeferredRegister<SoundEvent> REGISTRY = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, RemnantBossesMod.MODID);
	public static final RegistryObject<SoundEvent> SKELETONFIGHT_THEME = REGISTRY.register("skeletonfight_theme", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("remnant_bosses", "skeletonfight_theme")));
	public static final RegistryObject<SoundEvent> DASH_SFX = REGISTRY.register("dash_sfx", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("remnant_bosses", "dash_sfx")));
}