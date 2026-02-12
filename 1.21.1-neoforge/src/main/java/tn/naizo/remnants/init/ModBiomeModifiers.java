package tn.naizo.remnants.init;

import tn.naizo.remnants.RemnantBossesMod;
import tn.naizo.remnants.worldgen.ConfigurableSpawnBiomeModifier;

import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import com.mojang.serialization.MapCodec;

public class ModBiomeModifiers {
    public static final DeferredRegister<MapCodec<? extends BiomeModifier>> BIOME_MODIFIER_SERIALIZERS = DeferredRegister
            .create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, RemnantBossesMod.MODID);

    public static final DeferredHolder<MapCodec<? extends BiomeModifier>, MapCodec<ConfigurableSpawnBiomeModifier>> CONFIGURABLE_SPAWN = BIOME_MODIFIER_SERIALIZERS
            .register("configurable_spawn", () -> ConfigurableSpawnBiomeModifier.CODEC);
}
