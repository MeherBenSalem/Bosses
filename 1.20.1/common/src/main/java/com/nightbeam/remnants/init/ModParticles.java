package com.nightbeam.remnants.init;

import com.nightbeam.remnants.registry.RegistryHolder;
import net.minecraft.core.particles.SimpleParticleType;

import java.util.List;

/** The Realm's particles: golden grace motes and the Waygate's cyan-violet eclipse sparks. */
public final class ModParticles {
	public static final RegistryHolder<SimpleParticleType> GRACE_MOTE = new RegistryHolder<>("grace_mote");
	public static final RegistryHolder<SimpleParticleType> ECLIPSE_SPARK = new RegistryHolder<>("eclipse_spark");
	public static final List<RegistryHolder<SimpleParticleType>> ALL = List.of(GRACE_MOTE, ECLIPSE_SPARK);

	private ModParticles() {
	}

	public static SimpleParticleType create() {
		return new RemnantParticleType();
	}

	/** SimpleParticleType's constructor is not public on every loader; a subclass can always reach it. */
	private static final class RemnantParticleType extends SimpleParticleType {
		private RemnantParticleType() {
			super(false);
		}
	}
}
