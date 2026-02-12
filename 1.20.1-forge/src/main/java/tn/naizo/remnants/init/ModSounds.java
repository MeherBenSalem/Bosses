package tn.naizo.remnants.init;

import tn.naizo.remnants.RemnantBossesMod;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;

public class ModSounds {
	public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, RemnantBossesMod.MODID);

	public static final RegistryObject<SoundEvent> SKELETONFIGHT_THEME = SOUNDS.register("skeletonfight_theme",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("remnant_bosses", "skeletonfight_theme")));

	public static final RegistryObject<SoundEvent> DASH_SFX = SOUNDS.register("dash_sfx",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("remnant_bosses", "dash_sfx")));
}