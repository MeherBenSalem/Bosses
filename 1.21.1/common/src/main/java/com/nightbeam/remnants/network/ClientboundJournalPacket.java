package com.nightbeam.remnants.network;

import com.nightbeam.remnants.journal.BossHunt;
import com.nightbeam.remnants.journal.BossHunts;
import com.nightbeam.remnants.journal.HuntState;
import com.nightbeam.remnants.journal.RitualInfo;
import com.nightbeam.remnants.Constants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record ClientboundJournalPacket(boolean open, List<Entry> entries) implements CustomPacketPayload {
	public static final Type<ClientboundJournalPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "boss_journal"));
	public static final StreamCodec<FriendlyByteBuf, ClientboundJournalPacket> STREAM_CODEC = StreamCodec.of(
			(buf, packet) -> encode(packet, buf), ClientboundJournalPacket::decode);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	private static final int MAX_ENTRIES = 4;
	private static final int MAX_STRING_LENGTH = 256;
	private static final int OFFERING_COUNT = 4;

	public ClientboundJournalPacket {
		entries = List.copyOf(entries);
		if (entries.size() > MAX_ENTRIES) throw new IllegalArgumentException("Too many journal entries");
		Set<String> ids = new HashSet<>();
		for (Entry entry : entries) {
			if (!ids.add(entry.bossId())) throw new IllegalArgumentException("Duplicate journal boss");
		}
	}

	public record Entry(String bossId, HuntState state, RitualInfo ritual) {
		public Entry {
			checkString(bossId);
			if (BossHunt.fromId(bossId) == null) throw new IllegalArgumentException("Unknown journal boss");
			Objects.requireNonNull(state, "state");
			Objects.requireNonNull(ritual, "ritual");
			checkString(ritual.activationItem());
			if (ritual.offerings().size() != OFFERING_COUNT) throw new IllegalArgumentException("Invalid offering count");
			for (String offering : ritual.offerings()) checkString(offering);
			ritual = new RitualInfo(ritual.activationItem(), List.copyOf(ritual.offerings()), ritual.valid());
		}
	}

	private static void checkString(String value) {
		if (value == null || value.length() > MAX_STRING_LENGTH) throw new IllegalArgumentException("Invalid journal string");
	}

	public static void encode(ClientboundJournalPacket packet, FriendlyByteBuf buf) {
		buf.writeVarInt(BossHunts.SCHEMA_VERSION);
		buf.writeBoolean(packet.open());
		buf.writeVarInt(packet.entries().size());
		for (Entry entry : packet.entries()) {
			buf.writeUtf(entry.bossId(), MAX_STRING_LENGTH);
			buf.writeVarInt(entry.state().ordinal());
			buf.writeBoolean(entry.ritual().valid());
			buf.writeUtf(entry.ritual().activationItem(), MAX_STRING_LENGTH);
			buf.writeVarInt(OFFERING_COUNT);
			for (String offering : entry.ritual().offerings()) buf.writeUtf(offering, MAX_STRING_LENGTH);
		}
	}

	public static ClientboundJournalPacket decode(FriendlyByteBuf buf) {
		if (buf.readVarInt() != BossHunts.SCHEMA_VERSION) throw new IllegalArgumentException("Unsupported journal schema");
		boolean open = buf.readBoolean();
		int count = buf.readVarInt();
		if (count < 0 || count > MAX_ENTRIES) throw new IllegalArgumentException("Invalid journal entry count");
		List<Entry> entries = new ArrayList<>(count);
		Set<String> ids = new HashSet<>();
		for (int i = 0; i < count; i++) {
			String bossId = buf.readUtf(MAX_STRING_LENGTH);
			if (BossHunt.fromId(bossId) == null || !ids.add(bossId)) throw new IllegalArgumentException("Invalid journal boss");
			int state = buf.readVarInt();
			if (state < 0 || state >= HuntState.values().length) throw new IllegalArgumentException("Invalid journal state");
			boolean valid = buf.readBoolean();
			String activationItem = buf.readUtf(MAX_STRING_LENGTH);
			if (buf.readVarInt() != OFFERING_COUNT) throw new IllegalArgumentException("Invalid offering count");
			List<String> offerings = new ArrayList<>(OFFERING_COUNT);
			for (int j = 0; j < OFFERING_COUNT; j++) offerings.add(buf.readUtf(MAX_STRING_LENGTH));
			entries.add(new Entry(bossId, HuntState.values()[state], new RitualInfo(activationItem, offerings, valid)));
		}
		if (buf.readableBytes() != 0) throw new IllegalArgumentException("Trailing journal data");
		return new ClientboundJournalPacket(open, entries);
	}
}
