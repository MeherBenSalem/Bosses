package com.nightbeam.remnants.neoforge.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nightbeam.remnants.config.JaumlConfigLib;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

public record ConfigurableSpawnBiomeModifier(HolderSet<Biome> biomes, EntityType<?> entityType)
		implements BiomeModifier {

	public static final MapCodec<ConfigurableSpawnBiomeModifier> CODEC = RecordCodecBuilder.mapCodec(builder -> builder
			.group(
					Biome.LIST_CODEC.fieldOf("biomes").forGetter(ConfigurableSpawnBiomeModifier::biomes),
					BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("entity")
							.forGetter(ConfigurableSpawnBiomeModifier::entityType))
			.apply(builder, ConfigurableSpawnBiomeModifier::new));

	@Override
	public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
		if (phase == Phase.ADD && biomes.contains(biome)) {
			if (JaumlConfigLib.getNumberValue("remnant/spawning", "rat_spawns", "enable_natural_spawning") > 0) {
				int weight = (int) JaumlConfigLib.getNumberValue("remnant/spawning", "rat_spawns", "spawn_weight");
				int min = (int) JaumlConfigLib.getNumberValue("remnant/spawning", "rat_spawns", "min_group_size");
				int max = (int) JaumlConfigLib.getNumberValue("remnant/spawning", "rat_spawns", "max_group_size");

				String biomeKey = biome.unwrapKey().map(k -> k.location().toString()).orElse("");
				java.util.List<String> blacklist = JaumlConfigLib.getStringListValue("remnant/spawning", "rat_spawns",
						"biome_blacklist");
				if (blacklist.contains(biomeKey)) {
					return;
				}

				builder.getMobSpawnSettings().addSpawn(MobCategory.MONSTER,
						new MobSpawnSettings.SpawnerData(entityType, weight, min, max));
			}
		}
	}

	@Override
	public MapCodec<? extends BiomeModifier> codec() {
		return CODEC;
	}
}
