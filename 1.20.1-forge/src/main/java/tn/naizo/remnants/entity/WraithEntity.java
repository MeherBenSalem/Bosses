package tn.naizo.remnants.entity;

import tn.naizo.remnants.init.ModEntities;
import tn.naizo.remnants.config.JaumlConfigLib;

import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.network.PlayMessages;
import net.minecraftforge.network.NetworkHooks;

import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;

public class WraithEntity extends Monster {
	public static final EntityDataAccessor<Boolean> DATA_isAttacking = SynchedEntityData.defineId(WraithEntity.class, EntityDataSerializers.BOOLEAN);

	public final AnimationState animationState0 = new AnimationState(); // Idle
	public final AnimationState animationState1 = new AnimationState(); // Walk
	public final AnimationState animationState2 = new AnimationState(); // Attack
	public final AnimationState animationState3 = new AnimationState(); // Death

	private double spawnY;

	public WraithEntity(PlayMessages.SpawnEntity packet, Level world) {
		this(ModEntities.WRAITH.get(), world);
	}

	public WraithEntity(EntityType<WraithEntity> type, Level world) {
		super(type, world);
		setMaxUpStep(0.6f);
		xpReward = 0;
		setNoAi(false);
	}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(DATA_isAttacking, false);
	}

	@Override
	public void tick() {
		super.tick();
		if (!this.level().isClientSide) {
			// Sync attacking state to client
			this.entityData.set(DATA_isAttacking, this.getTarget() != null && this.isWithinReachOfTarget());
		}
		if (this.level().isClientSide()) {
			// Animation state updates moved to event handler
		}
	}

	@Override
	public void aiStep() {
		super.aiStep();

		// Flying behavior - hover up to 5 blocks above ground when target is above
		LivingEntity target = this.getTarget();
		if (target != null && !this.level().isClientSide) {
			double targetY = target.getY();
			double currentY = this.getY();

			// If target is above, apply upward velocity (max 5 blocks above spawn point)
			if (targetY > currentY + 2 && currentY < spawnY + 5) {
				var motion = this.getDeltaMovement();
				this.setDeltaMovement(motion.x(), motion.y() + 0.1, motion.z());
			}
		}
	}


	public boolean isWithinReachOfTarget() {
		if (this.getTarget() == null)
			return false;
		return this.distanceTo(this.getTarget()) < 3.0;
	}

	// Public accessor for client animation logic
	public boolean isAttacking() {
		return this.entityData.get(DATA_isAttacking);
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, false) {
			@Override
			protected double getAttackReachSqr(LivingEntity entity) {
				return this.mob.getBbWidth() * this.mob.getBbWidth() + entity.getBbWidth();
			}
		});
		this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
		this.goalSelector.addGoal(3, new RandomStrollGoal(this, 0.8));
		this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(5, new NearestAttackableTargetGoal(this, Player.class, false, false));
	}

	@Override
	public MobType getMobType() {
		return MobType.UNDEFINED;
	}

	@Override
	public SoundEvent getHurtSound(DamageSource ds) {
		return ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.parse("entity.generic.hurt"));
	}

	@Override
	public SoundEvent getDeathSound() {
		return ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.parse("entity.generic.death"));
	}

	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData livingdata, @Nullable CompoundTag tag) {
		SpawnGroupData retval = super.finalizeSpawn(world, difficulty, reason, livingdata, tag);

		// Store spawn Y for flight height clamping
		this.spawnY = this.getY();

		// Load configuration values
		double health = JaumlConfigLib.getNumberValue("remnant/balance", "wraith_stats", "wraith_health");
		this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
		this.setHealth(this.getMaxHealth());

		double armor = JaumlConfigLib.getNumberValue("remnant/balance", "wraith_stats", "wraith_armor");
		this.getAttribute(Attributes.ARMOR).setBaseValue(armor);

		double damage = JaumlConfigLib.getNumberValue("remnant/balance", "wraith_stats", "wraith_attack_damage");
		this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(damage);

		double speed = JaumlConfigLib.getNumberValue("remnant/balance", "wraith_stats", "wraith_movement_speed");
		this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(speed);

		double range = JaumlConfigLib.getNumberValue("remnant/balance", "wraith_stats", "wraith_follow_range");
		this.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(range);

		return retval;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag compound) {
		super.addAdditionalSaveData(compound);
		compound.putDouble("SpawnY", this.spawnY);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag compound) {
		super.readAdditionalSaveData(compound);
		if (compound.contains("SpawnY")) {
			this.spawnY = compound.getDouble("SpawnY");
		}
	}

	public static void init() {
	}

	public static AttributeSupplier.Builder createAttributes() {
		AttributeSupplier.Builder builder = Mob.createMobAttributes();
		builder = builder.add(Attributes.MOVEMENT_SPEED, 0.25);
		builder = builder.add(Attributes.MAX_HEALTH, 20);
		builder = builder.add(Attributes.ARMOR, 0);
		builder = builder.add(Attributes.ATTACK_DAMAGE, 3);
		builder = builder.add(Attributes.FOLLOW_RANGE, 16);
		return builder;
	}
}
