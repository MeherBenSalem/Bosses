package com.nightbeam.remnants.init;

import com.nightbeam.remnants.Constants;
import com.nightbeam.remnants.registry.RegistryHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

import java.util.List;

public final class ModSounds {
	public static final RegistryHolder<SoundEvent> SKELETONFIGHT_THEME = sound("skeletonfight_theme");
	public static final RegistryHolder<SoundEvent> DASH_SFX = sound("dash_sfx");
	// Hollow Sovereign and the Hollow Legion.
	public static final RegistryHolder<SoundEvent> SOVEREIGN_TOLL = sound("sovereign_toll");
	public static final RegistryHolder<SoundEvent> SOVEREIGN_GUARD = sound("sovereign_guard");
	public static final RegistryHolder<SoundEvent> SPECTRAL_SLASH = sound("spectral_slash");
	public static final RegistryHolder<SoundEvent> PAULDRON_HUM = sound("pauldron_hum");
	public static final RegistryHolder<SoundEvent> LEGION_HORN = sound("legion_horn");
	public static final RegistryHolder<SoundEvent> SOUL_RELEASE = sound("soul_release");

	// The Eclipsed Realm.
	public static final RegistryHolder<SoundEvent> WAYGATE_KINDLE = sound("waygate_kindle");
	public static final RegistryHolder<SoundEvent> WAYGATE_OPEN = sound("waygate_open");
	public static final RegistryHolder<SoundEvent> WAYGATE_HUM = sound("waygate_hum");
	public static final RegistryHolder<SoundEvent> WAYGATE_CROSS = sound("waygate_cross");
	public static final RegistryHolder<SoundEvent> GRACE_DISCOVER = sound("grace_discover");
	public static final RegistryHolder<SoundEvent> GRACE_REST = sound("grace_rest");
	public static final RegistryHolder<SoundEvent> REMNANT_FELLED = sound("remnant_felled");
	public static final RegistryHolder<SoundEvent> FOG_GATE_PASS = sound("fog_gate_pass");

	/** Every sound above, in registration order; loaders register this list. */
	public static final List<RegistryHolder<SoundEvent>> ALL = List.of(SKELETONFIGHT_THEME, DASH_SFX,
			SOVEREIGN_TOLL, SOVEREIGN_GUARD, SPECTRAL_SLASH,
			PAULDRON_HUM, LEGION_HORN, SOUL_RELEASE,
			WAYGATE_KINDLE, WAYGATE_OPEN, WAYGATE_HUM, WAYGATE_CROSS, GRACE_DISCOVER, GRACE_REST, REMNANT_FELLED, FOG_GATE_PASS);

	private ModSounds() {
	}

	private static RegistryHolder<SoundEvent> sound(String path) {
		return new RegistryHolder<>(path, () -> SoundEvent.createVariableRangeEvent(
				Identifier.fromNamespaceAndPath(Constants.MOD_ID, path)));
	}
}
