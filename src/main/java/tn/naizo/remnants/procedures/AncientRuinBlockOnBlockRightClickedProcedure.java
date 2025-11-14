package tn.naizo.remnants.procedures;

import tn.naizo.remnants.init.RemnantBossesModEntities;
import tn.naizo.remnants.init.RemnantBossesModBlocks;
import tn.naizo.remnants.RemnantBossesMod;
import tn.naizo.jauml.JaumlConfigLib;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Comparator;

public class AncientRuinBlockOnBlockRightClickedProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		boolean found = false;
		double sx = 0;
		double sy = 0;
		double sz = 0;
		if ((ForgeRegistries.ITEMS.getKey((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()).toString())
				.equals(JaumlConfigLib.getStringValue("remnant/bosses", "ossukage_summon", "portal_activation_item"))) {
			if ((world.getBlockState(BlockPos.containing(x + 3, y, z))).getBlock() == RemnantBossesModBlocks.ANCIENT_PEDESTAL.get() && (world.getBlockState(BlockPos.containing(x - 3, y, z))).getBlock() == RemnantBossesModBlocks.ANCIENT_PEDESTAL.get()
					&& (world.getBlockState(BlockPos.containing(x, y, z + 3))).getBlock() == RemnantBossesModBlocks.ANCIENT_PEDESTAL.get()
					&& (world.getBlockState(BlockPos.containing(x, y, z - 3))).getBlock() == RemnantBossesModBlocks.ANCIENT_PEDESTAL.get()) {
				if ((world.getBlockState(BlockPos.containing(x + 3, y + 1, z))).getBlock() == ForgeRegistries.BLOCKS
						.getValue(ResourceLocation.parse((JaumlConfigLib.getStringValue("remnant/bosses", "ossukage_summon", "pedestal_one_activation_block")).toLowerCase(java.util.Locale.ENGLISH)))
						&& (world.getBlockState(BlockPos.containing(x - 3, y + 1, z))).getBlock() == ForgeRegistries.BLOCKS
								.getValue(ResourceLocation.parse((JaumlConfigLib.getStringValue("remnant/bosses", "ossukage_summon", "pedestal_two_activation_block")).toLowerCase(java.util.Locale.ENGLISH)))
						&& (world.getBlockState(BlockPos.containing(x, y + 1, z + 3))).getBlock() == ForgeRegistries.BLOCKS
								.getValue(ResourceLocation.parse((JaumlConfigLib.getStringValue("remnant/bosses", "ossukage_summon", "pedestal_three_activation_block")).toLowerCase(java.util.Locale.ENGLISH)))
						&& (world.getBlockState(BlockPos.containing(x, y + 1, z - 3))).getBlock() == ForgeRegistries.BLOCKS
								.getValue(ResourceLocation.parse((JaumlConfigLib.getStringValue("remnant/bosses", "ossukage_summon", "pedestal_four_activation_block")).toLowerCase(java.util.Locale.ENGLISH)))) {
					if (world instanceof ServerLevel _level)
						_level.setDayTime(0);
					if (world instanceof ServerLevel _level) {
						LightningBolt entityToSpawn = EntityType.LIGHTNING_BOLT.create(_level);
						entityToSpawn.moveTo(Vec3.atBottomCenterOf(BlockPos.containing(x + 3, y + 1, z)));
						entityToSpawn.setVisualOnly(true);
						_level.addFreshEntity(entityToSpawn);
					}
					if (world instanceof ServerLevel _level) {
						LightningBolt entityToSpawn = EntityType.LIGHTNING_BOLT.create(_level);
						entityToSpawn.moveTo(Vec3.atBottomCenterOf(BlockPos.containing(x - 3, y + 1, z)));
						entityToSpawn.setVisualOnly(true);
						_level.addFreshEntity(entityToSpawn);
					}
					if (world instanceof ServerLevel _level) {
						LightningBolt entityToSpawn = EntityType.LIGHTNING_BOLT.create(_level);
						entityToSpawn.moveTo(Vec3.atBottomCenterOf(BlockPos.containing(x, y + 1, z + 3)));
						entityToSpawn.setVisualOnly(true);
						_level.addFreshEntity(entityToSpawn);
					}
					if (world instanceof ServerLevel _level) {
						LightningBolt entityToSpawn = EntityType.LIGHTNING_BOLT.create(_level);
						entityToSpawn.moveTo(Vec3.atBottomCenterOf(BlockPos.containing(x, y + 1, z - 3)));
						entityToSpawn.setVisualOnly(true);
						_level.addFreshEntity(entityToSpawn);
					}
					world.setBlock(BlockPos.containing(x + 3, y + 1, z), Blocks.AIR.defaultBlockState(), 3);
					world.setBlock(BlockPos.containing(x - 3, y + 1, z), Blocks.AIR.defaultBlockState(), 3);
					world.setBlock(BlockPos.containing(x, y + 1, z + 3), Blocks.AIR.defaultBlockState(), 3);
					world.setBlock(BlockPos.containing(x, y + 1, z - 3), Blocks.AIR.defaultBlockState(), 3);
					if (world instanceof Level _level) {
						if (!_level.isClientSide()) {
							_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.parse("entity.ender_dragon.growl")), SoundSource.NEUTRAL, 1, 1);
						} else {
							_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.parse("entity.ender_dragon.growl")), SoundSource.NEUTRAL, 1, 1, false);
						}
					}
					{
						final Vec3 _center = new Vec3(x, y, z);
						List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(70 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
						for (Entity entityiterator : _entfound) {
							if (entityiterator instanceof Player || entityiterator instanceof ServerPlayer) {
								if (entity instanceof Player _player) {
									ItemStack _stktoremove = new ItemStack(
											ForgeRegistries.ITEMS.getValue(ResourceLocation.parse((JaumlConfigLib.getStringValue("remnant/bosses", "ossukage_summon", "portal_activation_item")).toLowerCase(java.util.Locale.ENGLISH))));
									_player.getInventory().clearOrCountMatchingItems(p -> _stktoremove.getItem() == p.getItem(), 1, _player.inventoryMenu.getCraftSlots());
								}
								if (world instanceof Level _level) {
									if (!_level.isClientSide()) {
										_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.parse("remnant_bosses:skeletonfight_theme")), SoundSource.MUSIC, 1, 1);
									} else {
										_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.parse("remnant_bosses:skeletonfight_theme")), SoundSource.MUSIC, 1, 1, false);
									}
								}
								if (entityiterator instanceof Player _player && !_player.level().isClientSide())
									_player.displayClientMessage(Component.literal("\u00A76The \u00A7cRemnant Warriors \u00A76rise from the \u00A78shadows\u00A76, their \u00A74blades \u00A76thirsting for \u00A7cbattle\u00A76!"), false);
								RemnantBossesMod.queueServerWork(80, () -> {
									if (world instanceof ServerLevel _level) {
										LightningBolt entityToSpawn = EntityType.LIGHTNING_BOLT.create(_level);
										entityToSpawn.moveTo(Vec3.atBottomCenterOf(BlockPos.containing(x, y, z)));
										entityToSpawn.setVisualOnly(true);
										_level.addFreshEntity(entityToSpawn);
									}
									if (world instanceof ServerLevel _level) {
										Entity entityToSpawn = RemnantBossesModEntities.REMNANT_OSSUKAGE.get().spawn(_level, BlockPos.containing(x, y + 1, z), MobSpawnType.MOB_SUMMONED);
										if (entityToSpawn != null) {
											entityToSpawn.setDeltaMovement(0, 0, 0);
										}
									}
									for (int index0 = 0; index0 < (int) JaumlConfigLib.getNumberValue("remnant/bosses", "ossukage", "on_spawn_skeletons"); index0++) {
										if (world instanceof ServerLevel _level) {
											Entity entityToSpawn = RemnantBossesModEntities.SKELETON_MINION.get().spawn(_level, BlockPos.containing(x + Mth.nextInt(RandomSource.create(), -1, 1), y + 1, z + Mth.nextInt(RandomSource.create(), -1, 1)),
													MobSpawnType.MOB_SUMMONED);
											if (entityToSpawn != null) {
												entityToSpawn.setDeltaMovement(0, 0, 0);
											}
										}
									}
								});
							}
						}
					}
				}
			}
		}
	}
}