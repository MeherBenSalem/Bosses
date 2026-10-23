package com.nightbeam.remnants.realm;

import com.nightbeam.remnants.block.realm.EclipseWaygateBlock;
import com.nightbeam.remnants.Constants;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.core.UUIDUtil;
import com.nightbeam.remnants.config.RealmConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Pairs Waygates across the eclipse: every Waygate outside the Realm leads to one
 * inside it, and each Realm Waygate remembers the gate it was opened from. Saved on the overworld.
 */
public final class RealmLinks extends SavedData {
	public static final SavedDataType<RealmLinks> TYPE = new SavedDataType<>(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "realm_links"),
			RealmLinks::new, CompoundTag.CODEC.xmap(RealmLinks::load, data -> data.save(new CompoundTag())), DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

	private final Map<GlobalPos, BlockPos> outsideToRealm = new HashMap<>();
	private final Map<BlockPos, GlobalPos> realmToOutside = new HashMap<>();
	private final Map<UUID, GlobalPos> lastWaygate = new HashMap<>();

	public static RealmLinks get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	/** The Realm Waygate that {@code outside} opens onto, raising an Arrival Shrine the first time. */
	public BlockPos realmSideFor(ServerLevel realm, GlobalPos outside) {
		BlockPos linked = outsideToRealm.get(outside);
		if (linked != null && isCore(realm, linked)) return linked;
		int radius = (int) RealmConfig.value("arrival_search_radius", 0, 512);
		BlockPos target = outside.pos();
		BlockPos best = null;
		long bestDistance = Long.MAX_VALUE;
		for (BlockPos candidate : realmToOutside.keySet().toArray(new BlockPos[0])) {
			long dx = candidate.getX() - target.getX(), dz = candidate.getZ() - target.getZ();
			long distance = dx * dx + dz * dz;
			if (distance > (long) radius * radius || distance >= bestDistance) continue;
			if (isCore(realm, candidate)) {
				best = candidate;
				bestDistance = distance;
			} else {
				forgetRealm(candidate);
			}
		}
		if (best == null) best = ArrivalShrine.build(realm, target.getX(), target.getZ());
		link(outside, best);
		return best;
	}

	/** The gate a Realm Waygate was opened from, if it still stands. */
	@Nullable
	public GlobalPos outsideFor(MinecraftServer server, BlockPos realmCore) {
		GlobalPos outside = realmToOutside.get(realmCore);
		if (outside == null) return null;
		ServerLevel level = server.getLevel(outside.dimension());
		return level != null && isCore(level, outside.pos()) ? outside : null;
	}

	public void link(GlobalPos outside, BlockPos realmCore) {
		outsideToRealm.put(outside, realmCore.immutable());
		realmToOutside.put(realmCore.immutable(), outside);
		setDirty();
	}

	public void setLast(UUID player, GlobalPos waygate) {
		lastWaygate.put(player, waygate);
		setDirty();
	}

	@Nullable
	public GlobalPos last(MinecraftServer server, UUID player) {
		GlobalPos gate = lastWaygate.get(player);
		if (gate == null) return null;
		ServerLevel level = server.getLevel(gate.dimension());
		return level != null && isCore(level, gate.pos()) ? gate : null;
	}

	/** A Waygate core was removed: drop every link through it. */
	public void forget(ServerLevel level, BlockPos core) {
		if (EclipsedRealm.isRealm(level)) {
			forgetRealm(core);
		} else {
			GlobalPos outside = GlobalPos.of(level.dimension(), core);
			BlockPos realm = outsideToRealm.remove(outside);
			if (realm != null && outside.equals(realmToOutside.get(realm))) realmToOutside.remove(realm);
			lastWaygate.values().removeIf(outside::equals);
			setDirty();
		}
	}

	private void forgetRealm(BlockPos core) {
		realmToOutside.remove(core);
		outsideToRealm.values().removeIf(core::equals);
		setDirty();
	}

	public static boolean isCore(ServerLevel level, BlockPos pos) {
		return level.getBlockState(pos).getBlock() instanceof EclipseWaygateBlock;
	}

	CompoundTag save(CompoundTag tag) {
		ListTag links = new ListTag();
		outsideToRealm.forEach((outside, realm) -> {
			CompoundTag entry = writeGlobal(outside);
			entry.putLong("Realm", realm.asLong());
			links.add(entry);
		});
		tag.put("Links", links);
		ListTag reverse = new ListTag();
		realmToOutside.forEach((realm, outside) -> {
			CompoundTag entry = writeGlobal(outside);
			entry.putLong("Realm", realm.asLong());
			reverse.add(entry);
		});
		tag.put("Returns", reverse);
		ListTag last = new ListTag();
		lastWaygate.forEach((player, gate) -> {
			CompoundTag entry = writeGlobal(gate);
			entry.store("Player", UUIDUtil.CODEC, player);
			last.add(entry);
		});
		tag.put("Last", last);
		return tag;
	}

	static RealmLinks load(CompoundTag tag) {
		RealmLinks links = new RealmLinks();
		for (Tag t : tag.getListOrEmpty("Links")) {
			if (!(t instanceof CompoundTag entry)) continue;
			GlobalPos outside = readGlobal(entry);
			if (outside != null) links.outsideToRealm.put(outside, BlockPos.of(entry.getLongOr("Realm", 0L)));
		}
		for (Tag t : tag.getListOrEmpty("Returns")) {
			if (!(t instanceof CompoundTag entry)) continue;
			GlobalPos outside = readGlobal(entry);
			if (outside != null) links.realmToOutside.put(BlockPos.of(entry.getLongOr("Realm", 0L)), outside);
		}
		for (Tag t : tag.getListOrEmpty("Last")) {
			if (!(t instanceof CompoundTag entry)) continue;
			GlobalPos gate = readGlobal(entry);
			if (gate != null && entry.read("Player", UUIDUtil.CODEC).isPresent()) links.lastWaygate.put(entry.read("Player", UUIDUtil.CODEC).orElseThrow(), gate);
		}
		return links;
	}

	static CompoundTag writeGlobal(GlobalPos pos) {
		CompoundTag tag = new CompoundTag();
		tag.putString("Dimension", pos.dimension().identifier().toString());
		tag.putLong("Pos", pos.pos().asLong());
		return tag;
	}

	@Nullable
	static GlobalPos readGlobal(CompoundTag tag) {
		Identifier dimension = Identifier.tryParse(tag.getStringOr("Dimension", ""));
		if (dimension == null) return null;
		ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, dimension);
		return GlobalPos.of(key, BlockPos.of(tag.getLongOr("Pos", 0L)));
	}
}
