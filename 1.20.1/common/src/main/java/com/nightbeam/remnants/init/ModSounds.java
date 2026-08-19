package com.nightbeam.remnants.init;

import com.nightbeam.remnants.Constants;
import com.nightbeam.remnants.registry.RegistryHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {
	public static final RegistryHolder<SoundEvent> SKELETONFIGHT_THEME = new RegistryHolder<>("skeletonfight_theme");
	public static final RegistryHolder<SoundEvent> DASH_SFX = new RegistryHolder<>("dash_sfx");
	public static final RegistryHolder<SoundEvent> ARMORED_GRUB_AMBIENT = new RegistryHolder<>("armored_grub_ambient");
	public static final RegistryHolder<SoundEvent> ARMORED_GRUB_DEATH = new RegistryHolder<>("armored_grub_death");

	private ModSounds() {
	}

	public static SoundEvent createSkeletonFightTheme() {
		return SoundEvent.createVariableRangeEvent(new ResourceLocation(Constants.MOD_ID, "skeletonfight_theme"));
	}

	public static SoundEvent createDashSfx() {
		return SoundEvent.createVariableRangeEvent(new ResourceLocation(Constants.MOD_ID, "dash_sfx"));
	}

	public static SoundEvent createArmoredGrubAmbient() {
		return SoundEvent.createVariableRangeEvent(new ResourceLocation(Constants.MOD_ID, "armored_grub_ambient"));
	}

	public static SoundEvent createArmoredGrubDeath() {
		return SoundEvent.createVariableRangeEvent(new ResourceLocation(Constants.MOD_ID, "armored_grub_death"));
	}
}
