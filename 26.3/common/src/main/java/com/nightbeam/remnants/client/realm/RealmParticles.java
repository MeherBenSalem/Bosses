package com.nightbeam.remnants.client.realm;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/** Full-bright Realm particles: rising golden grace motes and quick cyan-violet eclipse sparks. */
public final class RealmParticles {
	private RealmParticles() {
	}

	private static class Glow extends SingleQuadParticle {
		private final SpriteSet sprites;
		private final float baseSize;
		private final float sway;

		Glow(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites,
				int life, float size, float sway) {
			super(level, x, y, z, sprites.first());
			this.xd = dx;
			this.yd = dy;
			this.zd = dz;
			this.sprites = sprites;
			this.lifetime = life;
			this.baseSize = size;
			this.quadSize = size;
			this.sway = sway;
			this.hasPhysics = false;
			this.friction = 0.96f;
			this.alpha = 0;
			setSpriteFromAge(sprites);
		}

		@Override
		public void tick() {
			super.tick();
			if (removed) return;
			setSpriteFromAge(sprites);
			float life = age / (float) lifetime;
			alpha = Math.min(1, age / 6f) * (1 - life * life);
			quadSize = baseSize * (1 - life * 0.45f);
			xd += Mth.sin(age * 0.23f + (float) x) * sway;
			zd += Mth.cos(age * 0.19f + (float) z) * sway;
		}

		@Override
		protected Layer getLayer() {
			return Layer.TRANSLUCENT;
		}

		@Override
		protected int getLightCoords(float partialTick) {
			return 0xF000F0;
		}
	}

	public static final class GraceMoteProvider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet sprites;

		public GraceMoteProvider(SpriteSet sprites) {
			this.sprites = sprites;
		}

		@Override
		public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
				double dx, double dy, double dz, RandomSource random) {
			Glow particle = new Glow(level, x, y, z, dx, dy, dz, sprites, 40 + random.nextInt(40),
					0.05f + random.nextFloat() * 0.06f, 0.0006f);
			float warm = random.nextFloat() * 0.12f;
			particle.setColor(1.0f, 0.82f + warm, 0.48f + warm);
			return particle;
		}
	}

	public static final class EclipseSparkProvider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet sprites;

		public EclipseSparkProvider(SpriteSet sprites) {
			this.sprites = sprites;
		}

		@Override
		public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
				double dx, double dy, double dz, RandomSource random) {
			Glow particle = new Glow(level, x, y, z, dx, dy, dz, sprites, 14 + random.nextInt(16),
					0.06f + random.nextFloat() * 0.05f, 0.0f);
			boolean violet = random.nextInt(3) == 0;
			particle.setColor(violet ? 0.78f : 0.55f, violet ? 0.55f : 0.95f, 1.0f);
			return particle;
		}
	}
}
