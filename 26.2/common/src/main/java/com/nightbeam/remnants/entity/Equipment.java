package com.nightbeam.remnants.entity;

import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Small entity helpers for APIs that moved since 1.21.1. */
public final class Equipment {
	private Equipment() {
	}

	/** The worn armor pieces (head, chest, legs, feet), like the old {@code LivingEntity#getArmorSlots}. */
	public static List<ItemStack> armor(LivingEntity entity) {
		List<ItemStack> worn = new ArrayList<>(4);
		for (EquipmentSlot slot : EquipmentSlot.VALUES) {
			if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) worn.add(entity.getItemBySlot(slot));
		}
		return worn;
	}

	/** A sound only this player hears, at their position (the old {@code ServerPlayer#playNotifySound}). */
	public static void playTo(ServerPlayer player, SoundEvent sound, SoundSource source, float volume, float pitch) {
		player.connection.send(new ClientboundSoundPacket(Holder.direct(sound), source, player.getX(), player.getY(), player.getZ(),
				volume, pitch, player.getRandom().nextLong()));
	}
}
