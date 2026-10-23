package com.nightbeam.remnants.realm;

import com.nightbeam.remnants.Constants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.structure.Structure;

import org.jetbrains.annotations.Nullable;

/** Keys and small helpers for the Eclipsed Realm dimension. */
public final class EclipsedRealm {
	public static final Identifier ID = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "eclipsed_realm");
	public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION, ID);
	/** Arenas the guidance of grace points toward. */
	public static final TagKey<Structure> ARENAS = TagKey.create(Registries.STRUCTURE, Identifier.fromNamespaceAndPath(Constants.MOD_ID, "remnant_arenas"));
	public static final Identifier ARRIVAL_SHRINE = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "realm/arrival_shrine");

	private EclipsedRealm() {
	}

	public static boolean isRealm(@Nullable Level level) {
		return level != null && LEVEL.equals(level.dimension());
	}

	@Nullable
	public static ServerLevel level(MinecraftServer server) {
		return server.getLevel(LEVEL);
	}

	/** Translation key of the biome at {@code pos}, e.g. biome.remnant_bosses.tarnished_fields. */
	public static String biomeKey(LevelReader level, BlockPos pos) {
		return level.getBiome(pos).unwrapKey()
				.map(key -> "biome." + key.identifier().getNamespace() + "." + key.identifier().getPath())
				.orElse("");
	}
}
