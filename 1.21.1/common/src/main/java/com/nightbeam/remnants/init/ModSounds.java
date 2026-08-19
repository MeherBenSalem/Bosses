package com.nightbeam.remnants.init;

import com.nightbeam.remnants.Constants;
import com.nightbeam.remnants.registry.RegistryHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {
	public static final RegistryHolder<SoundEvent> SKELETONFIGHT_THEME = sound("skeletonfight_theme");
	public static final RegistryHolder<SoundEvent> DASH_SFX = sound("dash_sfx");
	public static final RegistryHolder<SoundEvent> ARMORED_GRUB_AMBIENT = sound("armored_grub_ambient");
	public static final RegistryHolder<SoundEvent> ARMORED_GRUB_DEATH = sound("armored_grub_death");

	private ModSounds() {
	}

	private static RegistryHolder<SoundEvent> sound(String path) {
		return new RegistryHolder<>(path, () -> SoundEvent.createVariableRangeEvent(
				ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, path)));
	}
}
