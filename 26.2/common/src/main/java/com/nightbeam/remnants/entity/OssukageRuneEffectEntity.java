package com.nightbeam.remnants.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

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
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		return false;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(VARIANT, SPAWN_VARIANT);
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
	protected void readAdditionalSaveData(ValueInput tag) {
		setVariant(tag.getIntOr("Variant", 0));
		this.life = tag.getIntOr("Life", 0);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput tag) {
		tag.putInt("Variant", getVariant());
		tag.putInt("Life", this.life);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>("spawn", 0, state -> state.setAndContinue(SPAWN)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.geoCache;
	}
}
