package com.nightbeam.remnants.entity;

import com.nightbeam.remnants.init.ModSounds;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * ArmoredGrubEntity â€” a neutral flying armoured insect.
 *
 * Uses GeckoLib 5 for animated rendering.
 * AI: passive aerial wandering, then melee retaliation when hurt.
 * Stats: 40 HP / 6 ATK / 4 ARMOR / 32 follow range.
 */
public class ArmoredGrubEntity extends PathfinderMob implements GeoEntity {

    // The single looping idle animation â€” wing-flap + body bob + jaw clench (2 s loop)
    protected static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");

    // GeckoLib: one cache instance per entity â€” do NOT use the cache constructor directly
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    // ------------------------------------------------------------------ constructor

    public ArmoredGrubEntity(EntityType<ArmoredGrubEntity> type, Level level) {
        super(type, level);
        // FlyingMoveControl: max 20Â° yaw/pitch turn per tick, can hover in place
        this.moveControl = new FlyingMoveControl(this, 20, true);
        setNoGravity(true);
        this.xpReward = 0;
    }

    // ------------------------------------------------------------------ navigation

    @Override
    protected PathNavigation createNavigation(Level level) {
        // FlyingPathNavigation generates 3-D aerial paths (ignores blocks below)
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        nav.setCanPassDoors(true);
        return nav;
    }

    // ------------------------------------------------------------------ AI goals

    @Override
    protected void registerGoals() {
        // 0 â€” emergency float when submerged
        this.goalSelector.addGoal(0, new FloatGoal(this));

        // 1 â€” melee bite when a target is set by retaliation
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, false));

        // 2 â€” random 3-D aerial wandering (avoids water surfaces)
        this.goalSelector.addGoal(2, new WaterAvoidingRandomFlyingGoal(this, 1.0));

        // 3 / 4 â€” passive look-at behaviours
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        // Neutral behaviour: only retaliate when attacked
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    // ------------------------------------------------------------------ attributes

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.45)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.FLYING_SPEED, 0.6)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ARMOR, 4.0);
    }

    /** Empty hook â€” matches the convention used by other entities in this mod. */
    public static void init() {}

    // ------------------------------------------------------------------ sounds

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

    // ------------------------------------------------------------------ GeckoLib

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Single controller â€” the idle animation covers all flight states.
        // GeckoLib 5: AnimationController does NOT take `this` as the first argument.
        controllers.add(new AnimationController<>(this, "Flying", 5, this::flyController));
    }

    protected <E extends ArmoredGrubEntity> PlayState flyController(AnimationState<E> state) {
        // Always loop the idle (the only animation in the JSON)
        return state.setAndContinue(IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
