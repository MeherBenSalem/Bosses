package com.nightbeam.remnants.item;

import javax.annotation.Nullable;
import com.nightbeam.remnants.config.SovereignConfig;
import com.nightbeam.remnants.entity.MemoryBoltEntity;
import com.nightbeam.remnants.init.ModItems;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/**
 * Memory Staff: the Watcher's focus. Hold use to charge. Released before 20 ticks it
 * fires one homing memory bolt (6 damage, turns toward hostile creatures near its path; 10-tick cooldown,
 * 1 durability); released after 20 ticks it releases a ring burst of 12 bolts around the player (5 damage
 * each, 16 blocks; 80-tick cooldown, 4 durability). Repaired with an Ancient Knowledge Fragment.
 */
public class MemoryStaffItem extends Item {
	public static final int RING_CHARGE = 20, BOLT_COOLDOWN = 10, RING_COOLDOWN = 80, BOLT_COST = 1, RING_COST = 4, RING_BOLTS = 12;
	public static final float BOLT_DAMAGE = 6, RING_DAMAGE = 5;
	public static final double BOLT_SPEED = 0.9, BOLT_RANGE = 32, RING_RANGE = 16, HOMING_TURN = 8;
	private static final DustParticleOptions VIOLET = new DustParticleOptions(new Vector3f(0.62f, 0.53f, 0.88f), 0.8f);

	public MemoryStaffItem() {
		super(new Properties().durability(1200).rarity(Rarity.EPIC));
	}

	@Override
	public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
		return repair.is(ModItems.KNOWLEDGE_FRAGMENT.get());
	}

	@Override
	public int getEnchantmentValue() {
		return 15;
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.BOW;
	}

	@Override
	public int getUseDuration(ItemStack stack) {
		return 72000;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
		player.startUsingItem(hand);
		return InteractionResultHolder.consume(stack);
	}

	@Override
	public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
		int charged = getUseDuration(stack) - remaining;
		if (!level.isClientSide) {
			if (charged == RING_CHARGE) {
				level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.9f, 1.5f);
			}
			return;
		}
		double density = SovereignConfig.visualDensity();
		if (density <= 0 || entity.getRandom().nextDouble() > density) return;
		// Violet motes gather at the staff's crystal tip.
		Vec3 side = Vec3.directionFromRotation(0, entity.getYRot() + (entity.getUsedItemHand() == InteractionHand.MAIN_HAND ? 90 : -90));
		Vec3 tip = entity.position().add(side.scale(-0.45)).add(Vec3.directionFromRotation(0, entity.getYRot()).scale(0.4)).add(0, 1.9, 0);
		level.addParticle(charged >= RING_CHARGE ? ParticleTypes.END_ROD : VIOLET, tip.x + entity.getRandom().nextGaussian() * 0.1,
				tip.y + entity.getRandom().nextGaussian() * 0.1, tip.z + entity.getRandom().nextGaussian() * 0.1, 0, 0.01, 0);
	}

	@Override
	public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
		if (!(level instanceof ServerLevel server)) return;
		int charged = getUseDuration(stack) - timeLeft;
		boolean ring = charged >= RING_CHARGE;
		if (ring) {
			Vec3 chest = entity.position().add(0, entity.getBbHeight() * 0.6, 0);
			for (int i = 0; i < RING_BOLTS; i++) {
				Vec3 dir = Vec3.directionFromRotation(0, entity.getYRot() + i * 360f / RING_BOLTS);
				MemoryBoltEntity.fire(server, entity, chest.add(dir.scale(0.8)), dir, BOLT_SPEED, RING_DAMAGE, RING_RANGE);
			}
			level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.0f, 1.2f);
		} else {
			Vec3 look = entity.getLookAngle();
			Vec3 origin = entity.getEyePosition().add(0, -0.2, 0).add(look.scale(0.8));
			MemoryBoltEntity bolt = MemoryBoltEntity.fire(server, entity, origin, look, BOLT_SPEED, BOLT_DAMAGE, BOLT_RANGE);
			if (bolt != null) bolt.seekHostiles(HOMING_TURN);
			level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8f, 1.8f);
		}
		if (entity instanceof Player player) {
			player.getCooldowns().addCooldown(this, ring ? RING_COOLDOWN : BOLT_COOLDOWN);
			player.awardStat(Stats.ITEM_USED.get(this));
		}
		InteractionHand hand = entity.getUsedItemHand();
		stack.hurtAndBreak(ring ? RING_COST : BOLT_COST, entity, user -> user.broadcastBreakEvent(hand));
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		LoreItem.appendLore(this, 3, tooltip);
	}
}
