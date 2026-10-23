package com.nightbeam.remnants.item;

import com.nightbeam.remnants.entity.WardenChainEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;
import com.nightbeam.remnants.registry.RegistryKeys;
import com.nightbeam.remnants.init.ModToolMaterials;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Warden's Grasp: the Warden's chain hook as a weapon. 7 attack damage, 1.4 attack
 * speed. Use throws a {@code warden_chain} hook: a creature it catches is pulled to the player, while a
 * block (or a creature too heavy to move) pulls the player instead; 40-tick cooldown. Sweeping hits apply
 * Slowness I ({@code WardenRelics.onSweep}).
 */
public class WardensGraspItem extends Item {
	public static final int COOLDOWN = 40, THROW_COST = 1;
	public static final double HOOK_SPEED = 1.6, HOOK_RANGE = 16;
	public WardensGraspItem() {
		// 1 (player base) + 4 + 2 (tier) = 7 attack damage; 4 - 2.6 = 1.4 attack speed.
		super(RegistryKeys.item().rarity(Rarity.RARE).sword(ModToolMaterials.WARDENS_GRASP, 4, -2.6f));
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (player.getCooldowns().isOnCooldown(stack)) return InteractionResult.FAIL;
		if (level instanceof ServerLevel server) {
			Vec3 look = player.getLookAngle();
			Vec3 from = player.getEyePosition().add(0, -0.3, 0).add(look.scale(0.6));
			if (WardenChainEntity.forGrasp(server, player, from, look, HOOK_SPEED, HOOK_RANGE) == null) return InteractionResult.FAIL;
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CHAIN_FALL, SoundSource.PLAYERS, 1.0f, 1.1f);
			player.awardStat(Stats.ITEM_USED.get(this));
			stack.hurtAndBreak(THROW_COST, player, hand);
			// The server's cooldown is synced to the client.
			player.getCooldowns().addCooldown(stack, COOLDOWN);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		LoreItem.appendLore(this, 3, tooltip);
	}
}
