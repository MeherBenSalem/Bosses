package com.nightbeam.remnants.entity;

import com.nightbeam.remnants.Constants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Keeps a Remnant entity savable when another mod hands it an item that cannot be written to disk.
 *
 * <p>Minecraft 1.21.1 saves a mob's hand and armor items with {@code ItemStack.save}, which throws on
 * an encoding error. A stack whose {@code minecraft:entity_data} component has no {@code id} (seen as
 * {@code {mob_target:"remnants:burial_guardian"}} in ticket-0229) therefore crashes the server the
 * next time the chunk is saved or unloaded, for example right after a player teleports away. Before
 * the mob saves, such a stack loses only that component; a stack that still cannot be written is
 * dropped from the slot, which is better than a crash loop that also rolls the world back.
 */
public final class EquipmentSaveGuard {
	private EquipmentSaveGuard() {
	}

	/** Only Remnant entities are guarded; other mods' and vanilla mobs keep vanilla behaviour. */
	public static boolean covers(Mob mob) {
		return covers(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()));
	}

	public static boolean covers(ResourceLocation typeId) {
		return typeId != null && Constants.ENTITY_NAMESPACE.equals(typeId.getNamespace());
	}

	/**
	 * {@code stack} itself when it saves, a copy without {@code entity_data} when that component alone
	 * is unsavable, otherwise {@link ItemStack#EMPTY}. The given stack is never changed.
	 */
	public static ItemStack savable(ItemStack stack, HolderLookup.Provider registries) {
		if (stack.isEmpty() || saves(stack, registries)) return stack;
		ItemStack repaired = stack.copy();
		repaired.remove(DataComponents.ENTITY_DATA);
		return saves(repaired, registries) ? repaired : ItemStack.EMPTY;
	}

	public static boolean saves(ItemStack stack, HolderLookup.Provider registries) {
		return ItemStack.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), stack).result().isPresent();
	}

	/** Repairs every slot of {@code slots} in place. */
	public static void repair(Mob mob, List<ItemStack> slots, HolderLookup.Provider registries) {
		for (int i = 0; i < slots.size(); i++) {
			ItemStack stack = slots.get(i);
			ItemStack fixed = repair(mob, stack, registries);
			if (fixed != stack) slots.set(i, fixed);
		}
	}

	/** The savable form of one equipped stack; logs when it had to change. */
	public static ItemStack repair(Mob mob, ItemStack stack, HolderLookup.Provider registries) {
		ItemStack fixed = savable(stack, registries);
		if (fixed != stack) {
			Constants.LOG.warn("{} at {} held {} that cannot be saved; {} so the chunk can save", mob.getType().getDescriptionId(),
					mob.blockPosition().toShortString(), BuiltInRegistries.ITEM.getKey(stack.getItem()),
					fixed.isEmpty() ? "removed it" : "dropped its invalid entity_data");
		}
		return fixed;
	}
}
