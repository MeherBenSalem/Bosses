package tn.naizo.remnants.event;

import tn.naizo.remnants.block.AncientAltarBlock;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Handles player interactions with blocks.
 * Replaces the deleted AncientRuinBlockOnBlockRightClickedProcedure.
 */
public class BlockInteractionEvents {

	@SubscribeEvent
	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		Player player = event.getEntity();
		Level level = player.level();
		Block block = event.getLevel().getBlockState(event.getPos()).getBlock();

		// Only run on server
		if (level.isClientSide) {
			return;
		}

		// Handle Ancient Altar right-click
		if (block instanceof AncientAltarBlock) {
			handleAncientAltarClick(player, event.getPos(), level);
			event.setCanceled(true);
		}
	}

	/**
	 * Handle Ancient Altar block right-click.
	 * Original procedure performed ritual or entity summoning.
	 */
	private static void handleAncientAltarClick(Player player, net.minecraft.core.BlockPos pos, Level level) {
		// Server-only guard
		if (level.isClientSide())
			return;

		// Check activation item in hand
		String heldKey = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem())
				.toString();
		if (!heldKey.equalsIgnoreCase(tn.naizo.remnants.config.JaumlConfigLib.getStringValue("remnant/bosses",
				"ossukage_summon", "portal_activation_item")))
			return;

		// Verify pedestals are placed at +/-3 on X/Z
		if (level.getBlockState(pos.offset(3, 0, 0)).getBlock() != tn.naizo.remnants.init.ModBlocks.ANCIENT_PEDESTAL
				.get()
				|| level.getBlockState(pos.offset(-3, 0, 0))
						.getBlock() != tn.naizo.remnants.init.ModBlocks.ANCIENT_PEDESTAL.get()
				|| level.getBlockState(pos.offset(0, 0, 3))
						.getBlock() != tn.naizo.remnants.init.ModBlocks.ANCIENT_PEDESTAL.get()
				|| level.getBlockState(pos.offset(0, 0, -3))
						.getBlock() != tn.naizo.remnants.init.ModBlocks.ANCIENT_PEDESTAL.get()) {
			return;
		}

		// Validate pedestal-top activation blocks (configurable)
		boolean topsValid = true;
		java.util.Locale LO = java.util.Locale.ENGLISH;
		net.minecraft.resources.ResourceLocation cfgOne = net.minecraft.resources.ResourceLocation
				.parse(tn.naizo.remnants.config.JaumlConfigLib
						.getStringValue("remnant/bosses", "ossukage_summon", "pedestal_one_activation_block")
						.toLowerCase(LO));
		net.minecraft.resources.ResourceLocation cfgTwo = net.minecraft.resources.ResourceLocation
				.parse(tn.naizo.remnants.config.JaumlConfigLib
						.getStringValue("remnant/bosses", "ossukage_summon", "pedestal_two_activation_block")
						.toLowerCase(LO));
		net.minecraft.resources.ResourceLocation cfgThree = net.minecraft.resources.ResourceLocation
				.parse(tn.naizo.remnants.config.JaumlConfigLib
						.getStringValue("remnant/bosses", "ossukage_summon", "pedestal_three_activation_block")
						.toLowerCase(LO));
		net.minecraft.resources.ResourceLocation cfgFour = net.minecraft.resources.ResourceLocation
				.parse(tn.naizo.remnants.config.JaumlConfigLib
						.getStringValue("remnant/bosses", "ossukage_summon", "pedestal_four_activation_block")
						.toLowerCase(LO));

		if (net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(cfgOne) != level
				.getBlockState(pos.offset(3, 1, 0)).getBlock())
			topsValid = false;
		if (net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(cfgTwo) != level
				.getBlockState(pos.offset(-3, 1, 0)).getBlock())
			topsValid = false;
		if (net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(cfgThree) != level
				.getBlockState(pos.offset(0, 1, 3)).getBlock())
			topsValid = false;
		if (net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(cfgFour) != level
				.getBlockState(pos.offset(0, 1, -3)).getBlock())
			topsValid = false;
		if (!topsValid)
			return;

		// Ritual visual/audio: set day, lightning on pedestals and remove pedestal-top
		// blocks
		if (level instanceof net.minecraft.server.level.ServerLevel _slevel) {
			_slevel.setDayTime(0);
			for (net.minecraft.core.BlockPos bp : java.util.List.of(pos.offset(3, 1, 0), pos.offset(-3, 1, 0),
					pos.offset(0, 1, 3), pos.offset(0, 1, -3))) {
				net.minecraft.world.entity.LightningBolt bolt = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT
						.create(_slevel);
				if (bolt != null) {
					bolt.moveTo(net.minecraft.world.phys.Vec3.atBottomCenterOf(bp));
					bolt.setVisualOnly(true);
					_slevel.addFreshEntity(bolt);
				}
				_slevel.setBlock(bp, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
			}
			// Use generic growl sound (ender dragon)
			_slevel.playSound(null, pos, net.minecraft.sounds.SoundEvents.ENDER_DRAGON_GROWL,
					net.minecraft.sounds.SoundSource.NEUTRAL, 1f, 1f);
		}

		// Notify nearby players, consume activation item, play music and schedule boss
		// spawn
		final net.minecraft.world.phys.Vec3 _center = new net.minecraft.world.phys.Vec3(pos.getX(), pos.getY(),
				pos.getZ());
		java.util.List<net.minecraft.world.entity.Entity> ents = level
				.getEntitiesOfClass(net.minecraft.world.entity.Entity.class,
						new net.minecraft.world.phys.AABB(_center, _center).inflate(70d / 2d), e -> true)
				.stream().toList();
		for (net.minecraft.world.entity.Entity e : ents) {
			if (e instanceof net.minecraft.server.level.ServerPlayer _sp) {
				// remove one activation item from invoking player only
				if (player.getInventory().contains(player.getMainHandItem())) {
					if (net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem())
							.toString()
							.equalsIgnoreCase(tn.naizo.remnants.config.JaumlConfigLib.getStringValue("remnant/bosses",
									"ossukage_summon", "portal_activation_item"))) {
						if (!_sp.getAbilities().instabuild) {
							player.getMainHandItem().shrink(1);
						}
					}
				}

				_sp.displayClientMessage(
						net.minecraft.network.chat.Component.literal(
								"\u00A76The \u00A7cRemnant Warriors \u00A76rise from the \u00A78shadows\u00A76!"),
						false);
				try {
					_sp.playNotifySound(net.minecraft.sounds.SoundEvent.createVariableRangeEvent(
							net.minecraft.resources.ResourceLocation.parse("remnant_bosses:skeletonfight_theme")),
							net.minecraft.sounds.SoundSource.MUSIC, 1f, 1f);
				} catch (Exception ex) {
					// Ignore missing sound
				}
			}
		}

		// Schedule actual spawn after delay
		tn.naizo.remnants.RemnantBossesMod.queueServerWork(80, () -> {
			if (!(level instanceof net.minecraft.server.level.ServerLevel _s))
				return;
			// lightning center
			net.minecraft.world.entity.LightningBolt lb = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT
					.create(_s);
			if (lb != null) {
				lb.moveTo(net.minecraft.world.phys.Vec3.atBottomCenterOf(pos));
				lb.setVisualOnly(true);
				_s.addFreshEntity(lb);
			}
			// spawn boss
			net.minecraft.world.entity.Entity boss = tn.naizo.remnants.init.ModEntities.REMNANT_OSSUKAGE.get().spawn(_s,
					pos, net.minecraft.world.entity.MobSpawnType.MOB_SUMMONED);
			if (boss != null) {
				boss.setDeltaMovement(0, 0, 0);
				// Trigger initial spawn logic
				tn.naizo.remnants.procedures.OssukageOnInitialEntitySpawnProcedure.execute(_s, boss);
			}
			// spawn minions
			for (int i = 0; i < (int) tn.naizo.remnants.config.JaumlConfigLib.getNumberValue("remnant/bosses",
					"ossukage", "on_spawn_skeletons"); i++) {
				net.minecraft.world.entity.Entity min = tn.naizo.remnants.init.ModEntities.SKELETON_MINION.get().spawn(
						_s,
						pos.offset(net.minecraft.util.Mth.nextInt(net.minecraft.util.RandomSource.create(), -1, 1), 1,
								net.minecraft.util.Mth.nextInt(net.minecraft.util.RandomSource.create(), -1, 1)),
						net.minecraft.world.entity.MobSpawnType.MOB_SUMMONED);
				if (min != null)
					min.setDeltaMovement(0, 0, 0);
			}
		});
	}
}
