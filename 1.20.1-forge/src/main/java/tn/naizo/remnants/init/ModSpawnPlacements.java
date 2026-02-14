package tn.naizo.remnants.init;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.Difficulty;

@Mod.EventBusSubscriber(modid = "remnant_bosses", bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModSpawnPlacements {

	@SubscribeEvent
	public static void registerSpawnPlacements(SpawnPlacementRegisterEvent event) {
		event.register(ModEntities.RAT.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				(entityType, world, reason, pos, random) ->
					world.getDifficulty() != Difficulty.PEACEFUL
					&& Monster.isDarkEnoughToSpawn(world, pos, random)
					&& net.minecraft.world.entity.Mob.checkMobSpawnRules(entityType, world, reason, pos, random),
				SpawnPlacementRegisterEvent.Operation.REPLACE);

		event.register(ModEntities.SKELETON_MINION.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				(entityType, world, reason, pos, random) ->
					world.getDifficulty() != Difficulty.PEACEFUL
					&& Monster.isDarkEnoughToSpawn(world, pos, random)
					&& net.minecraft.world.entity.Mob.checkMobSpawnRules(entityType, world, reason, pos, random),
				SpawnPlacementRegisterEvent.Operation.REPLACE);

		event.register(ModEntities.WRAITH.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				(entityType, world, reason, pos, random) ->
					world.getDifficulty() != Difficulty.PEACEFUL
					&& Monster.isDarkEnoughToSpawn(world, pos, random)
					&& net.minecraft.world.entity.Mob.checkMobSpawnRules(entityType, world, reason, pos, random),
				SpawnPlacementRegisterEvent.Operation.REPLACE);
	}
}
