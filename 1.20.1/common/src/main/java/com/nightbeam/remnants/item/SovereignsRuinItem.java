package com.nightbeam.remnants.item;

import com.nightbeam.remnants.config.SovereignConfig;
import com.nightbeam.remnants.entity.SpectralCrescentEntity;
import com.nightbeam.remnants.init.ModItems;
import com.nightbeam.remnants.init.ModSounds;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import javax.annotation.Nullable;

import java.util.List;

/**
 * Sovereign's Ruin: the Sovereign's greatsword. Hold use for at least 15 ticks and release to loose a
 * spectral crescent (10 damage, pierces 3, 20 blocks); 60-tick cooldown, 3 durability per slash.
 */
public class SovereignsRuinItem extends SwordItem {
	public static final int MIN_CHARGE = 15, COOLDOWN = 60, SLASH_COST = 3, PIERCE = 3;
	public static final float CRESCENT_DAMAGE = 10, CRESCENT_SPEED = 1.3f, RANGE = 20;
	private static final DustParticleOptions SOUL_LIGHT = new DustParticleOptions(new Vector3f(0.31f, 0.79f, 0.76f), 0.8f);
	/** Diamond-level tier (1.20.1 {@code getLevel} 3 stands in for 1.21's incorrect-for-diamond block tag). */
	public static final Tier TIER = new Tier() {
		@Override public int getUses() { return 1800; }
		@Override public float getSpeed() { return 8f; }
		@Override public float getAttackDamageBonus() { return 4f; }
		@Override public int getLevel() { return 3; }
		@Override public int getEnchantmentValue() { return 15; }
		@Override public Ingredient getRepairIngredient() { return Ingredient.of(ModItems.ROYAL_SOUL_FRAGMENT.get()); }
	};

	public SovereignsRuinItem() {
		// 1 (player base) + 6 + 4 (tier) = 11 attack damage; 4 - 3.2 = 0.8 attack speed.
		super(TIER, 6, -3.2f, new Properties().fireResistant().rarity(Rarity.EPIC));
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.SPEAR;
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
			if (charged == MIN_CHARGE) {
				level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 0.8f, 1.4f);
			}
			return;
		}
		double density = SovereignConfig.visualDensity();
		if (density <= 0 || entity.getRandom().nextDouble() > density) return;
		// Soul light gathers along the blade, held up and back over the shoulder.
		Vec3 look = Vec3.directionFromRotation(0, entity.getYRot());
		Vec3 side = Vec3.directionFromRotation(0, entity.getYRot() + (entity.getUsedItemHand() == InteractionHand.MAIN_HAND ? 90 : -90));
		double reach = Math.min(1, charged / (double) MIN_CHARGE);
		Vec3 base = entity.position().add(side.scale(0.4)).add(look.scale(0.2)).add(0, 1.4, 0);
		Vec3 tip = base.add(0, 0.6 + 0.7 * reach, 0).add(look.scale(-0.2));
		Vec3 p = base.lerp(tip, entity.getRandom().nextDouble());
		level.addParticle(charged >= MIN_CHARGE ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.SOUL, p.x, p.y, p.z, 0, 0.01, 0);
		if (charged >= MIN_CHARGE) level.addParticle(SOUL_LIGHT, tip.x, tip.y, tip.z, 0, 0, 0);
	}

	@Override
	public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
		int charged = getUseDuration(stack) - timeLeft;
		if (charged < MIN_CHARGE || !(level instanceof ServerLevel server)) return;
		Vec3 look = entity.getLookAngle();
		Vec3 origin = entity.getEyePosition().add(0, -0.35, 0).add(look.scale(1.0));
		SpectralCrescentEntity.fire(server, entity, origin, look, CRESCENT_SPEED, CRESCENT_DAMAGE, PIERCE, RANGE);
		level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), ModSounds.SPECTRAL_SLASH.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
		if (entity instanceof Player player) {
			player.getCooldowns().addCooldown(this, COOLDOWN);
			player.awardStat(Stats.ITEM_USED.get(this));
		}
		InteractionHand hand = entity.getUsedItemHand();
		stack.hurtAndBreak(SLASH_COST, entity, user -> user.broadcastBreakEvent(hand));
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		LoreItem.appendLore(this, 2, tooltip);
	}
}
