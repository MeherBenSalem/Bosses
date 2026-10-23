package com.nightbeam.remnants.entity;

import com.nightbeam.remnants.config.SovereignConfig;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.util.ARGB;
import com.nightbeam.remnants.entity.anim.RemnantAnims;
import com.nightbeam.remnants.entity.anim.RemnantClips;
import com.nightbeam.remnants.init.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * remnants:watcher_illusion: a memory of the Forgotten Watcher hovering over a lens.
 * It has 1 health and shatters on any damage, lasts its lifetime and then fades, and every 70-90 ticks casts
 * a weaker Ring Volley (8 bolts) with the Watcher's own telegraph. Never saved; discarded with its Watcher.
 */
public class WatcherIllusionEntity extends Mob implements GeoEntity {
	private static final EntityDataAccessor<Integer> CAST = SynchedEntityData.defineId(WatcherIllusionEntity.class, EntityDataSerializers.INT);
	private static final DustParticleOptions PALE = new DustParticleOptions(ARGB.colorFromFloat(1f, 0.82f, 0.78f, 1.0f), 1.0f);
	public static final int BOLTS = 8;
	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private UUID ownerId;
	private ForgottenWatcherEntity cachedOwner;
	private int life = 400, nextCast = 60, seenCast = -1;
	private float boltDamage = 3;

	public WatcherIllusionEntity(EntityType<? extends WatcherIllusionEntity> type, Level level) {
		super(type, level);
		setNoGravity(true);
		this.xpReward = 0;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 1).add(Attributes.FOLLOW_RANGE, 48)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1);
	}

	/** Conjures an illusion of {@code owner} at {@code spot}. */
	public static WatcherIllusionEntity conjure(ServerLevel level, ForgottenWatcherEntity owner, Vec3 spot, int life, float boltDamage) {
		WatcherIllusionEntity illusion = ModEntities.WATCHER_ILLUSION.get().create(level, EntitySpawnReason.MOB_SUMMONED);
		if (illusion == null) return null;
		illusion.ownerId = owner.getUUID();
		illusion.cachedOwner = owner;
		illusion.life = life;
		illusion.boltDamage = boltDamage;
		illusion.nextCast = 40 + level.getRandom().nextInt(30);
		illusion.snapTo(spot.x, spot.y, spot.z, owner.getYRot(), 0);
		if (!level.addFreshEntity(illusion)) return null;
		Vec3 eye = spot.add(0, ForgottenWatcherEntity.EYE, 0);
		level.sendParticles(ParticleTypes.REVERSE_PORTAL, eye.x, eye.y, eye.z, 30, 0.6, 1.0, 0.6, 0.05);
		level.sendParticles(PALE, eye.x, eye.y, eye.z, 16, 0.7, 1.0, 0.7, 0);
		return illusion;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(CAST, -1);
	}

	public UUID ownerId() {
		return ownerId;
	}

	private ForgottenWatcherEntity owner() {
		if (cachedOwner != null && !cachedOwner.isRemoved()) return cachedOwner;
		if (ownerId != null && level() instanceof ServerLevel server && server.getEntity(ownerId) instanceof ForgottenWatcherEntity watcher) {
			cachedOwner = watcher;
		}
		return cachedOwner;
	}

	/** Ticks into the current cast, or -1. */
	public int castAge() {
		return entityData.get(CAST);
	}

	@Override
	public void tick() {
		super.tick();
		setDeltaMovement(Vec3.ZERO);
		if (level().isClientSide()) {
			clientVisuals();
			return;
		}
		ForgottenWatcherEntity owner = owner();
		if (owner == null || !owner.isAlive() || --life <= 0) {
			fade();
			return;
		}
		LivingEntity target = owner.getTarget();
		if (target != null) {
			float yaw = HollowLegion.yawToward(position(), target.position());
			float turned = getYRot() + Mth.clamp(Mth.wrapDegrees(yaw - getYRot()), -10, 10);
			setYRot(turned);
			setYBodyRot(turned);
			setYHeadRot(turned);
		}
		int cast = castAge();
		if (cast >= 0) {
			entityData.set(CAST, ++cast);
			if (cast == ForgottenWatcherEntity.VOLLEY_RELEASE && target != null && level() instanceof ServerLevel server) {
				Vec3 aim = target.position().add(0, target.getBbHeight() * 0.5, 0);
				ForgottenWatcherEntity.ringVolley(server, owner, position().add(0, ForgottenWatcherEntity.EYE, 0), aim, getYRot(), BOLTS, 0,
						boltDamage, 0.45, 28);
				playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 2.0f, 1.3f);
			}
			if (cast >= RemnantClips.FORGOTTEN_WATCHER.ticks("volley")) entityData.set(CAST, -1);
			return;
		}
		if (--nextCast <= 0 && target != null && hasLineOfSight(target)) {
			entityData.set(CAST, 0);
			nextCast = 70 + random.nextInt(21);
			playSound(SoundEvents.BEACON_ACTIVATE, 1.5f, 1.4f);
		}
	}

	private void clientVisuals() {
		double density = SovereignConfig.visualDensity();
		if (density <= 0) return;
		Vec3 eye = position().add(0, ForgottenWatcherEntity.EYE, 0);
		if (tickCount % 6 == 0 && random.nextDouble() < density) {
			level().addParticle(PALE, eye.x + random.nextGaussian() * 0.8, eye.y + random.nextGaussian() * 0.8, eye.z + random.nextGaussian() * 0.8, 0, 0.01, 0);
		}
		int cast = castAge();
		if (cast >= 0 && cast < ForgottenWatcherEntity.VOLLEY_RELEASE && tickCount % 2 == 0) {
			double radius = 0.4 + 2.2 * cast / ForgottenWatcherEntity.VOLLEY_RELEASE;
			for (int i = 0; i < 12; i++) {
				double a = i * Math.PI / 6 + cast * 0.1;
				level().addParticle(PALE, eye.x + Math.cos(a) * radius, eye.y, eye.z + Math.sin(a) * radius, 0, 0, 0);
			}
		}
	}

	/** Struck: the memory shatters (no loot, no experience). */
	public void shatter() {
		if (!(level() instanceof ServerLevel server) || isRemoved()) return;
		Vec3 eye = position().add(0, ForgottenWatcherEntity.EYE, 0);
		server.sendParticles(PALE, eye.x, eye.y, eye.z, 30, 0.7, 1.0, 0.7, 0);
		server.sendParticles(ParticleTypes.END_ROD, eye.x, eye.y, eye.z, 10, 0.5, 0.8, 0.5, 0.08);
		playSound(SoundEvents.GLASS_BREAK, 1.5f, 0.8f);
		playSound(SoundEvents.AMETHYST_CLUSTER_BREAK, 1.5f, 1.0f);
		discard();
	}

	private void fade() {
		if (level() instanceof ServerLevel server) {
			Vec3 eye = position().add(0, ForgottenWatcherEntity.EYE, 0);
			server.sendParticles(ParticleTypes.REVERSE_PORTAL, eye.x, eye.y, eye.z, 20, 0.5, 0.9, 0.5, 0.03);
		}
		discard();
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (level().isClientSide()) return false;
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurtServer(level, source, amount);
		Entity attacker = source.getEntity();
		if (attacker != null && ForgottenSouls.isWatcherSide(attacker)) return false;
		if (attacker == null && !source.is(DamageTypeTags.IS_EXPLOSION) && !source.is(DamageTypeTags.IS_PROJECTILE)) return false;
		shatter();
		return true;
	}

	@Override
	protected boolean considersEntityAsAlly(Entity other) {
		return super.considersEntityAsAlly(other) || (other != this && ForgottenSouls.isWatcherSide(other));
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void pushEntities() {
	}

	@Override
	public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
		return false;
	}

	@Override
	protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
		registrar.add(new AnimationController<>("illusion", 3, state -> {
			int cast = castAge();
			if (cast >= 0) {
				if (seenCast < 0) state.controller().reset();
				seenCast = cast;
				return state.setAndContinue(RemnantAnims.of(RemnantClips.FORGOTTEN_WATCHER, "volley"));
			}
			seenCast = -1;
			return state.setAndContinue(RemnantAnims.of(RemnantClips.FORGOTTEN_WATCHER, "idle"));
		}));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
