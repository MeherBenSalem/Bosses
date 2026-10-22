package com.nightbeam.remnants.block.realm;

import com.nightbeam.remnants.client.realm.WaygateClientFx;
import com.nightbeam.remnants.config.RealmConfig;
import com.nightbeam.remnants.init.ModBlockEntities;
import com.nightbeam.remnants.realm.Waygates;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Drives the Waygate's kindle timer on the server and its GeckoLib clips and ambience on the client. */
public class EclipseWaygateBlockEntity extends BlockEntity implements GeoBlockEntity {
	private static final RawAnimation DORMANT = RawAnimation.begin().thenLoop("dormant");
	private static final RawAnimation KINDLE = RawAnimation.begin().thenPlayAndHold("kindle");
	private static final RawAnimation OPEN = RawAnimation.begin().thenLoop("open");

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private int kindleTicks;
	/** Client only: the phase seen last tick, so the open flash and shake fire once. */
	public WaygatePhase clientPhase;
	/** Client only: ticks spent kindling as this client saw it. */
	public int clientKindleTicks;

	public EclipseWaygateBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.ECLIPSE_WAYGATE.get(), pos, state);
	}

	public void startKindle() {
		kindleTicks = 0;
		setChanged();
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, EclipseWaygateBlockEntity gate) {
		if (state.getValue(EclipseWaygateBlock.PHASE) != WaygatePhase.KINDLING || !(level instanceof ServerLevel server)) return;
		gate.kindleTicks++;
		int total = RealmConfig.kindleTicks();
		Waygates.kindleTick(server, pos, state, gate.kindleTicks, total);
		if (gate.kindleTicks >= total) {
			gate.kindleTicks = 0;
			Waygates.open(server, pos, state);
		}
		gate.setChanged();
	}

	public static void clientTick(Level level, BlockPos pos, BlockState state, EclipseWaygateBlockEntity gate) {
		WaygateClientFx.tick(level, pos, state, gate);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>(this, "gate", 0, animation -> {
			WaygatePhase phase = getBlockState().getValue(EclipseWaygateBlock.PHASE);
			return animation.setAndContinue(switch (phase) {
				case DORMANT -> DORMANT;
				case KINDLING -> KINDLE;
				case OPEN -> OPEN;
			});
		}));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		kindleTicks = tag.getInt("KindleTicks");
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		if (kindleTicks > 0) tag.putInt("KindleTicks", kindleTicks);
	}

	/** Forge culls block entities by this box; the arch reaches a block to each side and four up. */
	public AABB getRenderBoundingBox() {
		return new AABB(worldPosition).inflate(2.0, 0.0, 2.0).expandTowards(0.0, 4.0, 0.0);
	}
}
