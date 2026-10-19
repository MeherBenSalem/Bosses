package com.nightbeam.remnants.event;

import com.nightbeam.remnants.Constants;
import com.nightbeam.remnants.entity.SpectralRoyalGuardEntity;
import com.nightbeam.remnants.init.ModEntities;
import com.nightbeam.remnants.init.ModItems;
import com.nightbeam.remnants.init.ModSounds;
import com.nightbeam.remnants.item.RegaliaArmorItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Server-side rules for the Hollow Sovereign's rewards, loader-agnostic: Regalia set bonuses, the
 * Sovereign's Sigil passive and its max-health toll. Loaders call {@link #serverTick},
 * {@link #afterDamage}, {@link #onLogin}, {@link #onRespawn}, {@link #onLogout} and {@link #stop}.
 */
public final class SovereignRelics {
	public static final ResourceLocation SIGIL_TOLL = id("sigil_toll");
	public static final ResourceLocation SIGIL_COMMAND = id("sigil_command");
	public static final ResourceLocation ROYAL_BEARING = id("royal_bearing");
	public static final double TOLL = -4, COMMAND_BONUS = 0.10, BEARING_BONUS = 0.1, SIGIL_RADIUS = 16;
	public static final float UNBOWED_THRESHOLD = 6;
	public static final int UNBOWED_COOLDOWN = 400, UNBOWED_DURATION = 60;
	private static final DustParticleOptions GILT = new DustParticleOptions(new Vector3f(0.85f, 0.72f, 0.38f), 1.2f);
	private static final Map<UUID, Set<UUID>> SIGIL_GUARDS = new HashMap<>();
	private static final Map<UUID, Long> UNBOWED_READY = new HashMap<>();
	private static final Map<LivingEntity, Boolean> COMMANDED = new WeakHashMap<>();

	private SovereignRelics() {
	}

	private static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, path);
	}

	/** Once per second: Royal Bearing, Sigil command aura and stale Sigil tolls. */
	public static void serverTick(MinecraftServer server) {
		if (server.getTickCount() % 20 != 0) return;
		Set<LivingEntity> commanded = Collections.newSetFromMap(new IdentityHashMap<>());
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			royalBearing(player);
			if (hasToll(player) && !guardsAlive(server, player.getUUID())) removeToll(player);
			if (player.isAlive() && !player.isSpectator() && player.getOffhandItem().is(ModItems.SOVEREIGNS_SIGIL.get())) {
				for (LivingEntity ally : player.serverLevel().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(SIGIL_RADIUS),
						e -> e.isAlive() && e instanceof OwnableEntity owned && player.getUUID().equals(owned.getOwnerUUID())
								&& e.distanceToSqr(player) <= SIGIL_RADIUS * SIGIL_RADIUS)) {
					AttributeInstance damage = ally.getAttribute(Attributes.ATTACK_DAMAGE);
					if (damage == null) continue;
					if (!damage.hasModifier(SIGIL_COMMAND)) {
						damage.addTransientModifier(new AttributeModifier(SIGIL_COMMAND, COMMAND_BONUS, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
					}
					commanded.add(ally);
				}
			}
		}
		for (LivingEntity previous : new ArrayList<>(COMMANDED.keySet())) {
			if (commanded.contains(previous)) continue;
			AttributeInstance damage = previous.getAttribute(Attributes.ATTACK_DAMAGE);
			if (damage != null) damage.removeModifier(SIGIL_COMMAND);
		}
		COMMANDED.clear();
		for (LivingEntity ally : commanded) COMMANDED.put(ally, Boolean.TRUE);
	}

	public static int regaliaPieces(LivingEntity entity) {
		int pieces = 0;
		for (ItemStack armor : entity.getArmorSlots()) {
			if (armor.getItem() instanceof RegaliaArmorItem) pieces++;
		}
		return pieces;
	}

	/** Two pieces: +0.1 knockback resistance. */
	private static void royalBearing(ServerPlayer player) {
		AttributeInstance knockback = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
		if (knockback == null) return;
		boolean wanted = regaliaPieces(player) >= 2;
		if (wanted && !knockback.hasModifier(ROYAL_BEARING)) {
			knockback.addTransientModifier(new AttributeModifier(ROYAL_BEARING, BEARING_BONUS, AttributeModifier.Operation.ADD_VALUE));
		} else if (!wanted && knockback.hasModifier(ROYAL_BEARING)) {
			knockback.removeModifier(ROYAL_BEARING);
		}
	}

	/** Four pieces, Unbowed: a hit dealing 6 or more grants Resistance I for 60 ticks, every 400 ticks at most. */
	public static void afterDamage(LivingEntity victim, DamageSource source, float dealt) {
		if (!(victim instanceof ServerPlayer player) || dealt < UNBOWED_THRESHOLD || !player.isAlive()) return;
		if (regaliaPieces(player) < 4) return;
		long now = player.level().getGameTime();
		Long ready = UNBOWED_READY.get(player.getUUID());
		if (ready != null && now < ready) return;
		UNBOWED_READY.put(player.getUUID(), now + UNBOWED_COOLDOWN);
		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, UNBOWED_DURATION, 0));
		ServerLevel level = player.serverLevel();
		level.sendParticles(GILT, player.getX(), player.getY() + 1.0, player.getZ(), 24, 0.4, 0.6, 0.4, 0);
		level.sendParticles(ParticleTypes.WAX_ON, player.getX(), player.getY() + 1.0, player.getZ(), 8, 0.4, 0.6, 0.4, 0.1);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARMOR_EQUIP_GOLD.value(), SoundSource.PLAYERS, 1.0f, 0.7f);
	}

	/** Sigil active: two allied guards beside the player; 4 max health is held until they are gone. */
	public static boolean summonSigilGuards(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		Set<UUID> summoned = new HashSet<>();
		for (int side = -1; side <= 1; side += 2) {
			Vec3 around = player.position().add(Vec3.directionFromRotation(0, player.getYRot() + side * 90).scale(1.6));
			Vec3 spot = groundNear(level, around, player.getY());
			SpectralRoyalGuardEntity guard = SpectralRoyalGuardEntity.forPlayer(level, player, spot == null ? player.position() : spot);
			if (guard != null && !guard.isRemoved()) summoned.add(guard.getUUID());
		}
		if (summoned.isEmpty()) return false;
		SIGIL_GUARDS.computeIfAbsent(player.getUUID(), id -> new HashSet<>()).addAll(summoned);
		AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
		if (health != null) {
			health.addOrReplacePermanentModifier(new AttributeModifier(SIGIL_TOLL, TOLL, AttributeModifier.Operation.ADD_VALUE));
			if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
		}
		level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.SOVEREIGN_TOLL.get(), SoundSource.PLAYERS, 1.2f, 1.2f);
		level.sendParticles(ParticleTypes.SOUL, player.getX(), player.getY() + 1.2, player.getZ(), 16, 0.5, 0.6, 0.5, 0.02);
		return true;
	}

	private static Vec3 groundNear(ServerLevel level, Vec3 around, double y) {
		var type = ModEntities.SPECTRAL_ROYAL_GUARD.get();
		for (int dy = 1; dy >= -3; dy--) {
			Vec3 p = new Vec3(around.x, Math.floor(y) + dy, around.z);
			BlockPos below = BlockPos.containing(p).below();
			if (!level.getBlockState(below).getCollisionShape(level, below).isEmpty() && level.noCollision(type.getSpawnAABB(p.x, p.y, p.z))) {
				return p;
			}
		}
		return null;
	}

	private static boolean hasToll(ServerPlayer player) {
		AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
		return health != null && health.hasModifier(SIGIL_TOLL);
	}

	private static boolean guardsAlive(MinecraftServer server, UUID owner) {
		Set<UUID> guards = SIGIL_GUARDS.get(owner);
		if (guards == null) return false;
		guards.removeIf(id -> {
			for (ServerLevel level : server.getAllLevels()) {
				Entity entity = level.getEntity(id);
				if (entity != null && entity.isAlive()) return false;
			}
			return true;
		});
		if (guards.isEmpty()) SIGIL_GUARDS.remove(owner);
		return !guards.isEmpty();
	}

	private static void removeToll(ServerPlayer player) {
		AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
		if (health != null) health.removeModifier(SIGIL_TOLL);
		SIGIL_GUARDS.remove(player.getUUID());
	}

	/** A toll saved with the player outlives its guards (they are never saved): clear it. */
	public static void onLogin(ServerPlayer player) {
		if (hasToll(player) && !guardsAlive(player.server, player.getUUID())) removeToll(player);
	}

	public static void onRespawn(ServerPlayer player) {
		onLogin(player);
	}

	public static void onLogout(ServerPlayer player) {
		UNBOWED_READY.remove(player.getUUID());
		SIGIL_GUARDS.remove(player.getUUID());
	}

	public static void stop() {
		SIGIL_GUARDS.clear();
		UNBOWED_READY.clear();
		COMMANDED.clear();
	}
}
