package com.nightbeam.remnants;

import com.nightbeam.remnants.journal.BossHunt;
import com.nightbeam.remnants.journal.BossHunts;
import com.nightbeam.remnants.journal.HuntState;
import com.nightbeam.remnants.journal.RitualInfo;
import com.nightbeam.remnants.network.ClientboundJournalPacket;
import com.nightbeam.remnants.network.ServerboundJournalRequestPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class JournalPacketRegression {
	private JournalPacketRegression() {
	}

	public static void run() {
		List<ClientboundJournalPacket.Entry> entries = new ArrayList<>();
		for (BossHunt boss : BossHunt.values()) {
			entries.add(new ClientboundJournalPacket.Entry(boss.id(), HuntState.DEFEATED,
					new RitualInfo("minecraft:stick", List.of("minecraft:bone", "minecraft:bone",
							"minecraft:bone", "minecraft:bone"), true)));
		}
		ClientboundJournalPacket snapshot = new ClientboundJournalPacket(true, entries);
		entries.clear();
		check(snapshot.entries().size() == 4, "snapshot copies caller list");
		FriendlyByteBuf roundTrip = buffer();
		try {
			ClientboundJournalPacket.encode(snapshot, roundTrip);
			check(snapshot.equals(ClientboundJournalPacket.decode(roundTrip)), "personal snapshot round trip");
			check(roundTrip.readableBytes() == 0, "snapshot consumed exactly");
		} finally {
			roundTrip.release();
		}
		ServerboundJournalRequestPacket request = new ServerboundJournalRequestPacket(BossHunts.SCHEMA_VERSION);
		FriendlyByteBuf requestBuf = buffer();
		try {
			ServerboundJournalRequestPacket.encode(request, requestBuf);
			check(request.equals(ServerboundJournalRequestPacket.decode(requestBuf)), "version-only request round trip");
			check(requestBuf.readableBytes() == 0, "request consumed exactly");
		} finally {
			requestBuf.release();
		}
		rejectSnapshot(buf -> buf.writeVarInt(BossHunts.SCHEMA_VERSION + 1), "future snapshot schema");
		rejectSnapshot(buf -> header(buf, 5), "excess entries");
		rejectSnapshot(buf -> header(buf, -1), "negative entry count");
		rejectSnapshot(buf -> { header(buf, 1); buf.writeUtf("remnants:unknown"); }, "unknown boss");
		rejectSnapshot(buf -> { header(buf, 1); buf.writeUtf(BossHunt.OSSUKAGE.id()); buf.writeVarInt(3); }, "unknown state");
		rejectSnapshot(buf -> { header(buf, 1); buf.writeUtf(BossHunt.OSSUKAGE.id()); buf.writeVarInt(-1); }, "negative state");
		rejectSnapshot(buf -> { header(buf, 1); buf.writeUtf("x".repeat(257)); }, "oversized boss ID");
		rejectSnapshot(buf -> {
			header(buf, 2);
			writeEntry(buf, BossHunt.OSSUKAGE.id(), "minecraft:stick", 4);
			writeEntry(buf, BossHunt.OSSUKAGE.id(), "minecraft:stick", 4);
		}, "duplicate bosses");
		rejectSnapshot(buf -> { header(buf, 1); writeEntry(buf, BossHunt.OSSUKAGE.id(), "x".repeat(257), 4); },
				"oversized activation item");
		rejectSnapshot(buf -> { header(buf, 1); writeEntry(buf, BossHunt.OSSUKAGE.id(), "minecraft:stick", 5); },
				"excess offerings");
		rejectSnapshot(buf -> { header(buf, 1); writeEntry(buf, BossHunt.OSSUKAGE.id(), "minecraft:stick", 3); },
				"missing offering");
		rejectSnapshot(buf -> {
			header(buf, 1);
			buf.writeUtf(BossHunt.OSSUKAGE.id());
			buf.writeVarInt(0);
			buf.writeBoolean(true);
			buf.writeUtf("minecraft:stick");
			buf.writeVarInt(4).writeUtf("x".repeat(257));
		}, "oversized offering");
		rejectSnapshot(buf -> header(buf, 1), "truncated snapshot");
		rejectSnapshot(buf -> { header(buf, 0); buf.writeBoolean(true); }, "trailing snapshot data");
		rejectRequest(buf -> buf.writeVarInt(BossHunts.SCHEMA_VERSION + 1), "future request schema");
		rejectRequest(buf -> buf.writeVarInt(-1), "negative request schema");
		rejectRequest(buf -> { }, "truncated request");
		rejectRequest(buf -> buf.writeVarInt(BossHunts.SCHEMA_VERSION).writeLong(42), "client identity/trailing request data");
		rejectValue(() -> new ClientboundJournalPacket(false, java.util.Collections.nCopies(5, snapshot.entries().get(0))),
				"writer entry bound");
		rejectValue(() -> new ClientboundJournalPacket(false, List.of(snapshot.entries().get(0), snapshot.entries().get(0))),
				"writer duplicate bosses");
		rejectValue(() -> new ClientboundJournalPacket.Entry("remnants:unknown", HuntState.UNDISCOVERED,
				snapshot.entries().get(0).ritual()), "writer unknown boss");
		rejectValue(() -> new ClientboundJournalPacket.Entry(BossHunt.OSSUKAGE.id(), HuntState.ENCOUNTERED,
				new RitualInfo("x".repeat(257), List.of("", "", "", ""), false)), "writer string bound");
		rejectValue(() -> new ClientboundJournalPacket.Entry(BossHunt.OSSUKAGE.id(), HuntState.ENCOUNTERED,
				new RitualInfo("", List.of("", "", ""), false)), "writer offering count");
		ClientboundJournalPacket boundary = new ClientboundJournalPacket(false,
				List.of(new ClientboundJournalPacket.Entry(BossHunt.OSSUKAGE.id(), HuntState.UNDISCOVERED,
						new RitualInfo("x".repeat(256), List.of("", "", "", "x".repeat(256)), false))));
		FriendlyByteBuf boundaryBuf = buffer();
		try {
			ClientboundJournalPacket.encode(boundary, boundaryBuf);
			check(boundary.equals(ClientboundJournalPacket.decode(boundaryBuf)), "256-character unavailable ritual boundary");
		} finally {
			boundaryBuf.release();
		}
		System.out.println("Journal packet regressions passed");
	}

	private static void header(FriendlyByteBuf buf, int count) {
		buf.writeVarInt(BossHunts.SCHEMA_VERSION);
		buf.writeBoolean(true);
		buf.writeVarInt(count);
	}

	private static void writeEntry(FriendlyByteBuf buf, String bossId, String activation, int offerings) {
		buf.writeUtf(bossId);
		buf.writeVarInt(0);
		buf.writeBoolean(true);
		buf.writeUtf(activation);
		buf.writeVarInt(offerings);
		for (int i = 0; i < offerings; i++) buf.writeUtf("minecraft:bone");
	}

	private static FriendlyByteBuf buffer() {
		return new FriendlyByteBuf(Unpooled.buffer());
	}

	private static void rejectSnapshot(Consumer<FriendlyByteBuf> fixture, String name) {
		reject(fixture, buf -> ClientboundJournalPacket.decode(buf), name);
	}

	private static void rejectRequest(Consumer<FriendlyByteBuf> fixture, String name) {
		reject(fixture, buf -> ServerboundJournalRequestPacket.decode(buf), name);
	}

	private static void reject(Consumer<FriendlyByteBuf> fixture, Consumer<FriendlyByteBuf> decode, String name) {
		FriendlyByteBuf buf = buffer();
		try {
			fixture.accept(buf);
			try {
				decode.accept(buf);
			} catch (RuntimeException expected) {
				return;
			}
			throw new AssertionError("Accepted " + name);
		} finally {
			buf.release();
		}
	}

	private static void check(boolean condition, String name) {
		if (!condition) throw new AssertionError(name);
	}

	private static void rejectValue(Runnable fixture, String name) {
		try {
			fixture.run();
		} catch (IllegalArgumentException expected) {
			return;
		}
		throw new AssertionError("Accepted " + name);
	}
}
