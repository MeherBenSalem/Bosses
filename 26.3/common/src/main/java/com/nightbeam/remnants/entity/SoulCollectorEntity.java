package com.nightbeam.remnants.entity;

import com.nightbeam.remnants.config.SovereignConfig;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import com.nightbeam.remnants.entity.anim.RemnantClips;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Soul Collector: an elite lantern-cage of harvested souls on four hooked arms. It
 * harvests in melee (healing per creature struck), marks the ground under its target for a delayed soul
 * burst, summons soul wisps, drinks the souls of creatures dying nearby and keeps the Forgotten Souls
 * around it hardened (Resistance I and Speed I).
 */
public class SoulCollectorEntity extends FloatingSoulEntity {
	public static final int MARK_PLACED = 16, HARVEST_HIT = 18, WISPS_RISE = 20, AURA_INTERVAL = 60;
	public static final int MAX_SOULS = 3;
	private static final DustParticleOptions SOUL_LIGHT = new DustParticleOptions(ARGB.colorFromFloat(1f, 0.31f, 0.79f, 0.76f), 1.1f);
	private final List<Mark> marks = new ArrayList<>();
	private int summonCooldown = 100, markCooldown, storedSouls;

	/** A soul mark on the ground and the game tick it bursts. */
	private record Mark(Vec3 at, long burst) {
	}

	public SoulCollectorEntity(EntityType<? extends SoulCollectorEntity> type, Level level) {
		super(type, level);
		this.xpReward = 22;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return floatingAttributes(70, 8, 6, 0.18).add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
	}

	@Override
	public String configId() {
		return "soul_collector";
	}

	@Override
	public RemnantClips.Sheet clips() {
		return RemnantClips.SOUL_COLLECTOR;
	}

	@Override
	protected double hoverHeight() {
		return 0.5;
	}

	/** The cage floats on its hooks: it has no walk clip, so drifting plays idle. */
	@Override
	protected String movementClip(boolean moving) {
		return "idle";
	}

	@Override
	protected double[] holdRange() {
		return new double[] {1.5, cfg("harvest_reach", 1, 8)};
	}

	@Override
	protected void applyConfig() {
		super.applyConfig();
		setBase(Attributes.KNOCKBACK_RESISTANCE, cfg("knockback_resistance", 0, 1));
	}

	@Override
	protected void serverTick() {
		super.serverTick();
		if (summonCooldown > 0) summonCooldown--;
		if (markCooldown > 0) markCooldown--;
		tickMarks();
		if (tickCount % AURA_INTERVAL == 0) spectreAura();
	}

	@Override
	protected void selectAction(LivingEntity target, double distance) {
		int maxWisps = (int) cfg("max_wisps", 0, 12);
		if (summonCooldown <= 0 && wisps().size() < maxWisps) {
			begin("summon", target.position());
			playSound(SoundEvents.EVOKER_PREPARE_SUMMON, 1.2f, 0.8f);
			return;
		}
		double reach = cfg("harvest_reach", 1, 8);
		if (distance - target.getBbWidth() / 2 <= reach) {
			begin("harvest", target.position());
			playSound(SoundEvents.CHAIN_PLACE, 1.2f, 0.6f);
			return;
		}
		if (markCooldown <= 0 && distance >= 4 && distance <= cfg("mark_range", 4, 32)) {
			begin("mark", target.position());
			playSound(SoundEvents.SOUL_ESCAPE.value(), 1.5f, 0.5f);
		}
	}

	@Override
	protected void actionTick(String action, int age) {
		LivingEntity target = getTarget();
		switch (action) {
			case "harvest" -> {
				if (target != null && age < HARVEST_HIT - 4) faceToward(target.position(), 15);
				if (age == HARVEST_HIT) {
					float heal = (float) cfg("harvest_heal", 0, 100);
					for (LivingEntity hit : HollowLegion.arc(this, cfg("harvest_reach", 1, 8), 120, this::canHit)) {
						if (strike(hit, (float) cfg("harvest_damage", 0, 1000), 0.3)) heal(heal);
					}
					playSound(SoundEvents.CHAIN_BREAK, 1.2f, 0.7f);
				}
			}
			case "mark" -> {
				if (target != null && age < MARK_PLACED) faceToward(target.position(), 20);
				if (age == MARK_PLACED && target != null) {
					marks.add(new Mark(target.position(), level().getGameTime() + (long) cfg("mark_delay_ticks", 5, 200)));
					markCooldown = (int) cfg("mark_cooldown_ticks", 0, 1200);
					playSound(SoundEvents.SCULK_CLICKING, 1.5f, 0.6f);
				}
			}
			case "summon" -> {
				if (age == WISPS_RISE) summonWisps(target);
			}
			default -> {
			}
		}
	}

	private void tickMarks() {
		if (marks.isEmpty() || !(level() instanceof ServerLevel server)) return;
		long now = level().getGameTime();
		double radius = cfg("mark_radius", 0.5, 8);
		for (Iterator<Mark> it = marks.iterator(); it.hasNext(); ) {
			Mark mark = it.next();
			long left = mark.burst() - now;
			if (left > 0) {
				// The mark fills with a shrinking ring of soul light.
				if (left % 3 == 0 && SovereignConfig.visualDensity() > 0) {
					double r = radius * Math.max(0.15, left / cfg("mark_delay_ticks", 5, 200));
					for (int i = 0; i < 12; i++) {
						double a = i * Math.PI / 6;
						server.sendParticles(SOUL_LIGHT, mark.at().x + Math.cos(a) * r, mark.at().y + 0.1, mark.at().z + Math.sin(a) * r, 1, 0, 0, 0, 0);
					}
					server.sendParticles(ParticleTypes.SOUL, mark.at().x, mark.at().y + 0.1, mark.at().z, 1, radius * 0.4, 0, radius * 0.4, 0);
				}
				continue;
			}
			it.remove();
			AABB area = new AABB(mark.at().x - radius, mark.at().y - 1, mark.at().z - radius, mark.at().x + radius, mark.at().y + 2.5, mark.at().z + radius);
			int slow = (int) cfg("mark_slow_ticks", 0, 600);
			for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class, area, this::canHit)) {
				double dx = victim.getX() - mark.at().x, dz = victim.getZ() - mark.at().z;
				if (dx * dx + dz * dz > radius * radius) continue;
				if (Damage.hurt(victim, damageSources().mobAttack(this), (float) cfg("mark_damage", 0, 1000)) && slow > 0) {
					victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, slow, 0), this);
				}
			}
			server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, mark.at().x, mark.at().y + 0.3, mark.at().z, 24, radius * 0.4, 0.6, radius * 0.4, 0.05);
			server.sendParticles(ParticleTypes.SOUL, mark.at().x, mark.at().y + 0.6, mark.at().z, 10, radius * 0.3, 0.8, radius * 0.3, 0.06);
			server.playSound(null, mark.at().x, mark.at().y, mark.at().z, SoundEvents.SOUL_ESCAPE.value(), getSoundSource(), 1.6f, 0.7f);
		}
	}

	private void summonWisps(LivingEntity target) {
		if (!(level() instanceof ServerLevel server)) return;
		int room = Math.max(0, (int) cfg("max_wisps", 0, 12) - wisps().size());
		int count = Math.min(room, (int) cfg("summon_count", 0, 8) + storedSouls);
		Vec3 cage = position().add(0, 1.5, 0);
		for (int i = 0; i < count; i++) {
			double a = i * Math.PI * 2 / Math.max(1, count) + random.nextDouble();
			Vec3 spot = cage.add(Math.cos(a) * 1.2, 0.3 + random.nextDouble() * 0.5, Math.sin(a) * 1.2);
			SoulWispEntity.rise(server, this, spot, target);
		}
		storedSouls = 0;
		summonCooldown = (int) cfg("summon_cooldown_ticks", 0, 24000);
		playSound(SoundEvents.SOUL_ESCAPE.value(), 1.5f, 0.9f);
	}

	public List<SoulWispEntity> wisps() {
		return level().getEntitiesOfClass(SoulWispEntity.class, getBoundingBox().inflate(32), w -> w.isAlive() && getUUID().equals(w.ownerId()));
	}

	/**
	 * Called when any creature dies: every Soul Collector within 12 blocks draws its soul (players, wisps and
	 * the Collector's own kind excepted), heals 6 and stores it (at most 3).
	 */
	public static void onCreatureDied(LivingEntity dead) {
		if (!(dead.level() instanceof ServerLevel server) || dead instanceof Player || dead instanceof SoulWispEntity) return;
		for (SoulCollectorEntity collector : server.getEntitiesOfClass(SoulCollectorEntity.class, dead.getBoundingBox().inflate(12),
				c -> c != dead && c.isAlive())) {
			if (collector.distanceToSqr(dead) > 144) continue;
			collector.drinkSoul(server, dead);
		}
	}

	private void drinkSoul(ServerLevel server, LivingEntity dead) {
		heal((float) cfg("soul_heal", 0, 100));
		storedSouls = Math.min(MAX_SOULS, storedSouls + 1);
		Vec3 from = dead.position().add(0, dead.getBbHeight() / 2, 0), to = position().add(0, 1.5, 0);
		for (int i = 0; i <= 10; i++) {
			Vec3 p = from.lerp(to, i / 10.0);
			server.sendParticles(i % 2 == 0 ? ParticleTypes.SOUL : SOUL_LIGHT, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0);
		}
		playSound(SoundEvents.SOUL_ESCAPE.value(), 1.2f, 1.4f);
	}

	/** Every 60 ticks: Forgotten Souls within 10 blocks gain Resistance I and Speed I for 80 ticks. */
	private void spectreAura() {
		if (!(level() instanceof ServerLevel server)) return;
		double radius = cfg("aura_radius", 1, 32);
		int ticks = (int) cfg("aura_ticks", 1, 1200);
		Vec3 cage = position().add(0, 1.5, 0);
		for (LivingEntity ally : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(radius),
				e -> e != this && e.isAlive() && ForgottenSouls.isSoul(e) && e.distanceToSqr(this) <= radius * radius)) {
			ally.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, ticks, 0), this);
			ally.addEffect(new MobEffectInstance(MobEffects.SPEED, ticks, 0), this);
			if (SovereignConfig.visualDensity() > 0) {
				Vec3 to = ally.position().add(0, ally.getBbHeight() / 2, 0);
				for (int i = 1; i < 6; i++) {
					Vec3 p = cage.lerp(to, i / 6.0);
					server.sendParticles(SOUL_LIGHT, p.x, p.y, p.z, 1, 0, 0, 0, 0);
				}
			}
		}
	}

	@Override
	protected void clientTick() {
		if (tickCount % 6 != 0 || SovereignConfig.visualDensity() <= 0) return;
		// Harvested souls drift inside the cage.
		level().addParticle(ParticleTypes.SOUL, getX() + random.nextGaussian() * 0.15, getY() + 1.4 + random.nextDouble() * 0.4,
				getZ() + random.nextGaussian() * 0.15, 0, 0.01, 0);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.CHAIN_STEP;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.COPPER_HIT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.COPPER_BREAK;
	}

	@Override
	protected void playStepSound(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
	}

	@Override
	public float getVoicePitch() {
		return 0.6f;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 160;
	}

	@Override
	public void addAdditionalSaveData(ValueOutput tag) {
		super.addAdditionalSaveData(tag);
		tag.putInt("StoredSouls", storedSouls);
		tag.putInt("SummonCooldown", summonCooldown);
	}

	@Override
	public void readAdditionalSaveData(ValueInput tag) {
		super.readAdditionalSaveData(tag);
		storedSouls = Math.min(MAX_SOULS, tag.getIntOr("StoredSouls", 0));
		summonCooldown = tag.getIntOr("SummonCooldown", 0);
	}
}
