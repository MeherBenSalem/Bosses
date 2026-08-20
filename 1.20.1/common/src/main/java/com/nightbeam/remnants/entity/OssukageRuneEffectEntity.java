package com.nightbeam.remnants.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class OssukageRuneEffectEntity extends Entity implements GeoEntity {
	public static final int SPAWN_VARIANT = 0;
	public static final int LEAVES_VARIANT = 1;
	private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(OssukageRuneEffectEntity.class, EntityDataSerializers.INT);
	private static final RawAnimation SPAWN = RawAnimation.begin().thenPlayAndHold("spawn");
	private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
	private int life = 45;

	public OssukageRuneEffectEntity(EntityType<? extends OssukageRuneEffectEntity> type, Level level) {
		super(type, level);
		this.noPhysics = true;
		this.setNoGravity(true);
	}

	@Override
	protected void defineSynchedData() {
		this.entityData.define(VARIANT, SPAWN_VARIANT);
	}

	public int getVariant() {
		return this.entityData.get(VARIANT);
	}

	public void setVariant(int variant) {
		this.entityData.set(VARIANT, variant == LEAVES_VARIANT ? LEAVES_VARIANT : SPAWN_VARIANT);
		this.life = variant == LEAVES_VARIANT ? 45 : 42;
	}

	@Override
	public void tick() {
		super.tick();
		this.setDeltaMovement(Vec3.ZERO);
		if (!this.level().isClientSide() && --this.life <= 0) this.discard();
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		setVariant(tag.getInt("Variant"));
		this.life = tag.getInt("Life");
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		tag.putInt("Variant", getVariant());
		tag.putInt("Life", this.life);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>(this, "spawn", 0, state -> state.setAndContinue(SPAWN)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.geoCache;
	}
}
