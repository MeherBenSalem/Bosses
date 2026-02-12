package tn.naizo.remnants.init;

import tn.naizo.remnants.RemnantBossesMod;
import tn.naizo.remnants.worldgen.ConfigurableSpawnBiomeModifier;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.common.world.BiomeModifier;
import com.mojang.serialization.Codec;

public class ModBiomeModifiers {
    public static final DeferredRegister<Codec<? extends BiomeModifier>> BIOME_MODIFIER_SERIALIZERS = DeferredRegister
            .create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, RemnantBossesMod.MODID);

    public static final RegistryObject<Codec<ConfigurableSpawnBiomeModifier>> CONFIGURABLE_SPAWN = BIOME_MODIFIER_SERIALIZERS
            .register("configurable_spawn", () -> ConfigurableSpawnBiomeModifier.CODEC);
}
