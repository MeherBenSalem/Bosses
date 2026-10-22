package com.nightbeam.remnants.init;

import com.nightbeam.remnants.Constants;
import com.nightbeam.remnants.registry.RegistryHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import java.util.List;

public final class ModSounds {
	public static final RegistryHolder<SoundEvent> SKELETONFIGHT_THEME = new RegistryHolder<>("skeletonfight_theme");
	public static final RegistryHolder<SoundEvent> DASH_SFX = new RegistryHolder<>("dash_sfx");
	// Hollow Sovereign and the Hollow Legion.
	public static final RegistryHolder<SoundEvent> SOVEREIGN_TOLL = new RegistryHolder<>("sovereign_toll");
	public static final RegistryHolder<SoundEvent> SOVEREIGN_GUARD = new RegistryHolder<>("sovereign_guard");
	public static final RegistryHolder<SoundEvent> SPECTRAL_SLASH = new RegistryHolder<>("spectral_slash");
	public static final RegistryHolder<SoundEvent> PAULDRON_HUM = new RegistryHolder<>("pauldron_hum");
	public static final RegistryHolder<SoundEvent> LEGION_HORN = new RegistryHolder<>("legion_horn");
	public static final RegistryHolder<SoundEvent> SOUL_RELEASE = new RegistryHolder<>("soul_release");

	/** Hollow Sovereign sounds in registration order; loaders register each with {@link #create(RegistryHolder)}. */
	public static final List<RegistryHolder<SoundEvent>> SOVEREIGN_SOUNDS = List.of(SOVEREIGN_TOLL, SOVEREIGN_GUARD,
			SPECTRAL_SLASH, PAULDRON_HUM, LEGION_HORN, SOUL_RELEASE);

	// The Eclipsed Realm.
	public static final RegistryHolder<SoundEvent> WAYGATE_KINDLE = new RegistryHolder<>("waygate_kindle");
	public static final RegistryHolder<SoundEvent> WAYGATE_OPEN = new RegistryHolder<>("waygate_open");
	public static final RegistryHolder<SoundEvent> WAYGATE_HUM = new RegistryHolder<>("waygate_hum");
	public static final RegistryHolder<SoundEvent> WAYGATE_CROSS = new RegistryHolder<>("waygate_cross");
	public static final RegistryHolder<SoundEvent> GRACE_DISCOVER = new RegistryHolder<>("grace_discover");
	public static final RegistryHolder<SoundEvent> GRACE_REST = new RegistryHolder<>("grace_rest");
	public static final RegistryHolder<SoundEvent> REMNANT_FELLED = new RegistryHolder<>("remnant_felled");
	public static final RegistryHolder<SoundEvent> FOG_GATE_PASS = new RegistryHolder<>("fog_gate_pass");

	/** Realm sounds in registration order; loaders register each with {@link #create(RegistryHolder)}. */
	public static final List<RegistryHolder<SoundEvent>> REALM_SOUNDS = List.of(WAYGATE_KINDLE, WAYGATE_OPEN, WAYGATE_HUM,
			WAYGATE_CROSS, GRACE_DISCOVER, GRACE_REST, REMNANT_FELLED, FOG_GATE_PASS);

	private ModSounds() {
	}

	public static SoundEvent create(RegistryHolder<SoundEvent> holder) {
		return SoundEvent.createVariableRangeEvent(holder.id());
	}

	public static SoundEvent createSkeletonFightTheme() {
		return SoundEvent.createVariableRangeEvent(new ResourceLocation(Constants.MOD_ID, "skeletonfight_theme"));
	}

	public static SoundEvent createDashSfx() {
		return SoundEvent.createVariableRangeEvent(new ResourceLocation(Constants.MOD_ID, "dash_sfx"));
	}
}
