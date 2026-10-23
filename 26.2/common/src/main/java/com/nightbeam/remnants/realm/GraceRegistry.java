package com.nightbeam.remnants.realm;

import com.nightbeam.remnants.init.ModBlocks;
import net.minecraft.util.Mth;
import net.minecraft.world.level.storage.LevelData;
import com.nightbeam.remnants.Constants;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.core.UUIDUtil;
import com.nightbeam.remnants.init.ModParticles;
import com.nightbeam.remnants.init.ModSounds;
import com.nightbeam.remnants.network.ClientboundGraceMenuPacket;
import com.nightbeam.remnants.network.ClientboundRealmFxPacket;
import com.nightbeam.remnants.network.ServerboundGraceActionPacket;
import com.nightbeam.remnants.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Every grace a player has rested at, and who discovered which. Saved on the overworld.
 * A grace without a custom name is shown by its biome, numbered when the biome repeats ("@biome.key#2").
 */
public final class GraceRegistry extends SavedData {
	public static final SavedDataType<GraceRegistry> TYPE = new SavedDataType<>(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "graces"),
			GraceRegistry::new, CompoundTag.CODEC.xmap(GraceRegistry::load, data -> data.save(new CompoundTag())), DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
	private static final double MENU_REACH_SQR = 8 * 8;

	private final Map<GlobalPos, String> names = new LinkedHashMap<>();
	private final Map<UUID, Set<GlobalPos>> discovered = new HashMap<>();

	public static GraceRegistry get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	/** Right-click on a grace: rename with a name tag, or rest, show the guidance and open the menu. */
	public static void use(ServerPlayer player, ServerLevel level, BlockPos pos, InteractionHand hand) {
		GraceRegistry registry = get(level.getServer());
		GlobalPos grace = GlobalPos.of(level.dimension(), pos.immutable());
		ItemStack held = player.getItemInHand(hand);
		registry.ensure(level, grace);
		if (held.is(Items.NAME_TAG) && held.has(DataComponents.CUSTOM_NAME)) {
			String name = held.getHoverName().getString().trim();
			if (!name.isEmpty()) {
				registry.names.put(grace, name.length() > 32 ? name.substring(0, 32) : name);
				registry.setDirty();
				if (!player.getAbilities().instabuild) held.shrink(1);
				level.playSound(null, pos, ModSounds.GRACE_REST.get(), SoundSource.BLOCKS, 1.0f, 1.3f);
				player.sendOverlayMessage(Component.translatable("message.remnant_bosses.grace.renamed", name));
				return;
			}
		}
		boolean first = registry.discover(player.getUUID(), grace);
		rest(player, level, pos, first);
		GraceGuidance.show(player, level, pos);
		Services.NETWORK.sendGraceMenu(player, registry.menu(player, grace, EclipsedRealm.isRealm(level)));
	}

	private static void rest(ServerPlayer player, ServerLevel level, BlockPos pos, boolean first) {
		player.setHealth(player.getMaxHealth());
		player.clearFire();
		List<Holder<MobEffect>> harmful = new ArrayList<>();
		for (MobEffectInstance effect : player.getActiveEffects()) {
			if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) harmful.add(effect.getEffect());
		}
		harmful.forEach(player::removeEffect);
		BlockPos spot = standSpot(level, pos);
		float angle = (float) (Math.toDegrees(Math.atan2(pos.getX() - spot.getX(), spot.getZ() - pos.getZ())));
		player.setRespawnPosition(new ServerPlayer.RespawnConfig(new LevelData.RespawnData(GlobalPos.of(level.dimension(), spot), Mth.wrapDegrees(angle), 0), true), false);
		level.playSound(null, pos, first ? ModSounds.GRACE_DISCOVER.get() : ModSounds.GRACE_REST.get(), SoundSource.BLOCKS,
				first ? 1.6f : 1.0f, 1.0f);
		level.sendParticles(ModParticles.GRACE_MOTE.get(), pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5,
				first ? 70 : 30, 0.5, 0.6, 0.5, 0.03);
		Services.NETWORK.sendRealmFx(player, ClientboundRealmFxPacket.of(first
				? ClientboundRealmFxPacket.GRACE_DISCOVERED : ClientboundRealmFxPacket.GRACE_RESTED));
	}

	/** A free, floored spot beside the grace to respawn on; south first, then the other sides and corners. */
	public static BlockPos standSpot(ServerLevel level, BlockPos grace) {
		int[][] offsets = {{0, 1}, {1, 0}, {-1, 0}, {0, -1}, {1, 1}, {-1, 1}, {1, -1}, {-1, -1}, {0, 2}};
		for (int[] o : offsets) {
			BlockPos feet = grace.offset(o[0], 0, o[1]);
			for (int dy = 0; dy <= 1; dy++) {
				BlockPos candidate = feet.above(dy);
				if (free(level, candidate) && free(level, candidate.above())
						&& level.getBlockState(candidate.below()).isFaceSturdy(level, candidate.below(), Direction.UP)) {
					return candidate;
				}
			}
		}
		return grace.above();
	}

	private static boolean free(ServerLevel level, BlockPos pos) {
		return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() && level.getFluidState(pos).isEmpty();
	}

	/** A grace block was removed: forget it everywhere. */
	public static void removed(ServerLevel level, BlockPos pos) {
		GraceRegistry registry = get(level.getServer());
		GlobalPos grace = GlobalPos.of(level.dimension(), pos);
		if (registry.names.remove(grace) != null) {
			registry.discovered.values().forEach(set -> set.remove(grace));
			registry.setDirty();
		}
	}

	public static void handleAction(ServerPlayer player, ServerboundGraceActionPacket packet) {
		ServerLevel level = player.level();
		if (player.distanceToSqr(Vec3.atCenterOf(packet.from())) > MENU_REACH_SQR
				|| !level.getBlockState(packet.from()).is(ModBlocks.GRACE_EMBER.get())) return;
		if (packet.action() == ServerboundGraceActionPacket.DEPART) {
			RealmTravel.beginDepart(player);
			return;
		}
		if (packet.action() != ServerboundGraceActionPacket.TRAVEL) return;
		GraceRegistry registry = get(player.level().getServer());
		GlobalPos target = GlobalPos.of(ResourceKey.create(Registries.DIMENSION, packet.targetDimension()), packet.target());
		Set<GlobalPos> known = registry.discovered.get(player.getUUID());
		if (known == null || !known.contains(target)) return;
		ServerLevel targetLevel = player.level().getServer().getLevel(target.dimension());
		if (targetLevel == null || !targetLevel.getBlockState(target.pos()).is(ModBlocks.GRACE_EMBER.get())) {
			if (targetLevel != null) removed(targetLevel, target.pos());
			player.sendOverlayMessage(Component.translatable("message.remnant_bosses.grace.lost"));
			return;
		}
		RealmTravel.beginGrace(player, target, registry.names.getOrDefault(target, ""));
	}

	private void ensure(ServerLevel level, GlobalPos grace) {
		if (names.containsKey(grace)) return;
		String biome = EclipsedRealm.biomeKey(level, grace.pos());
		int ordinal = 1;
		for (String name : names.values()) {
			if (name.startsWith("@" + biome + "#")) ordinal++;
		}
		names.put(grace, "@" + biome + "#" + ordinal);
		setDirty();
	}

	private boolean discover(UUID player, GlobalPos grace) {
		boolean added = discovered.computeIfAbsent(player, id -> new HashSet<>()).add(grace);
		if (added) setDirty();
		return added;
	}

	private ClientboundGraceMenuPacket menu(ServerPlayer player, GlobalPos here, boolean inRealm) {
		List<ClientboundGraceMenuPacket.Entry> entries = new ArrayList<>();
		for (GlobalPos grace : discovered.getOrDefault(player.getUUID(), Set.of())) {
			String name = names.get(grace);
			if (name == null) continue;
			entries.add(new ClientboundGraceMenuPacket.Entry(grace.dimension().identifier(), grace.pos(), name, grace.equals(here)));
		}
		entries.sort(Comparator.comparing((ClientboundGraceMenuPacket.Entry e) -> !e.here())
				.thenComparing(e -> e.dimension().toString()).thenComparing(ClientboundGraceMenuPacket.Entry::name));
		return new ClientboundGraceMenuPacket(here.pos(), names.getOrDefault(here, ""), inRealm, entries);
	}

	@Nullable
	public String name(GlobalPos grace) {
		return names.get(grace);
	}

	CompoundTag save(CompoundTag tag) {
		ListTag graces = new ListTag();
		names.forEach((grace, name) -> {
			CompoundTag entry = RealmLinks.writeGlobal(grace);
			entry.putString("Name", name);
			graces.add(entry);
		});
		tag.put("Graces", graces);
		ListTag players = new ListTag();
		discovered.forEach((player, set) -> {
			CompoundTag entry = new CompoundTag();
			entry.store("Player", UUIDUtil.CODEC, player);
			ListTag known = new ListTag();
			for (GlobalPos grace : set) known.add(RealmLinks.writeGlobal(grace));
			entry.put("Discovered", known);
			players.add(entry);
		});
		tag.put("Players", players);
		return tag;
	}

	static GraceRegistry load(CompoundTag tag) {
		GraceRegistry registry = new GraceRegistry();
		for (Tag t : tag.getListOrEmpty("Graces")) {
			if (!(t instanceof CompoundTag entry)) continue;
			GlobalPos grace = RealmLinks.readGlobal(entry);
			if (grace != null) registry.names.put(grace, entry.getStringOr("Name", ""));
		}
		for (Tag t : tag.getListOrEmpty("Players")) {
			if (!(t instanceof CompoundTag entry)) continue;
			if (entry.read("Player", UUIDUtil.CODEC).isEmpty()) continue;
			Set<GlobalPos> set = new HashSet<>();
			for (Tag k : entry.getListOrEmpty("Discovered")) {
				GlobalPos grace = k instanceof CompoundTag c ? RealmLinks.readGlobal(c) : null;
				if (grace != null) set.add(grace);
			}
			registry.discovered.put(entry.read("Player", UUIDUtil.CODEC).orElseThrow(), set);
		}
		return registry;
	}
}
