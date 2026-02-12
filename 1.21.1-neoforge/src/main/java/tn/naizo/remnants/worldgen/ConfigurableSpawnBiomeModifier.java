package tn.naizo.remnants.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import net.minecraft.core.registries.BuiltInRegistries;

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
            if (tn.naizo.remnants.config.JaumlConfigLib.getNumberValue("remnant/spawning", "rat_spawns",
                    "enable_natural_spawning") > 0) {
                int weight = (int) tn.naizo.remnants.config.JaumlConfigLib.getNumberValue("remnant/spawning",
                        "rat_spawns",
                        "spawn_weight");
                int min = (int) tn.naizo.remnants.config.JaumlConfigLib.getNumberValue("remnant/spawning", "rat_spawns",
                        "min_group_size");
                int max = (int) tn.naizo.remnants.config.JaumlConfigLib.getNumberValue("remnant/spawning", "rat_spawns",
                        "max_group_size");

                String biomeKey = biome.unwrapKey().map(k -> k.location().toString()).orElse("");
                java.util.List<String> blacklist = tn.naizo.remnants.config.JaumlConfigLib
                        .getStringListValue("remnant/spawning", "rat_spawns", "biome_blacklist");
                if (blacklist.contains(biomeKey))
                    return;

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
