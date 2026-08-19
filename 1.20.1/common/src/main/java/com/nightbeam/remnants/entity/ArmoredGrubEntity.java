package com.nightbeam.remnants.entity;

import com.nightbeam.remnants.init.ModSounds;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class ArmoredGrubEntity extends PathfinderMob implements GeoEntity {

	protected static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");

	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

	public ArmoredGrubEntity(EntityType<ArmoredGrubEntity> type, Level level) {
		super(type, level);
		this.moveControl = new FlyingMoveControl(this, 20, true);
		setNoGravity(true);
		xpReward = 0;
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
		nav.setCanOpenDoors(false);
		nav.setCanFloat(true);
		nav.setCanPassDoors(true);
		return nav;
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));

		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, false) {
			@Override
			protected double getAttackReachSqr(LivingEntity target) {
				return 1.75D + target.getBbWidth();
			}
		});

		this.goalSelector.addGoal(2, new WaterAvoidingRandomFlyingGoal(this, 1.0));
		this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0f));
		this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 40.0)
				.add(Attributes.MOVEMENT_SPEED, 0.45)
				.add(Attributes.ATTACK_DAMAGE, 6.0)
				.add(Attributes.FLYING_SPEED, 0.6)
				.add(Attributes.FOLLOW_RANGE, 32.0)
				.add(Attributes.ARMOR, 4.0);
	}

	public static void init() {
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return ModSounds.ARMORED_GRUB_AMBIENT.get();
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ModSounds.ARMORED_GRUB_DEATH.get();
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.GENERIC_HURT;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>(this, "Flying", 5, this::flyController));
	}

	protected <E extends ArmoredGrubEntity> PlayState flyController(AnimationState<E> state) {
		return state.setAndContinue(IDLE);
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.geoCache;
	}
}
