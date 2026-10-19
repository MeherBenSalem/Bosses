package com.nightbeam.remnants.client.realm;

import com.nightbeam.remnants.network.ClientboundGraceMenuPacket;
import com.nightbeam.remnants.network.ServerboundGraceActionPacket;
import com.nightbeam.remnants.platform.Services;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * The grace menu, opened after resting. Lists every discovered grace to travel to, plus
 * departing the Realm. The server re-validates every choice.
 */
public class GraceScreen extends Screen {
	private static final int PER_PAGE = 7;
	private static final int ROW = 22;
	private static final int GOLD = 0xE8C77A;
	private final ClientboundGraceMenuPacket menu;
	private final List<ClientboundGraceMenuPacket.Entry> entries;
	private int page;

	public GraceScreen(ClientboundGraceMenuPacket menu) {
		super(Component.translatable("gui.remnant_bosses.grace.title"));
		this.menu = menu;
		this.entries = menu.entries();
	}

	@Override
	protected void init() {
		int cx = width / 2;
		int top = height / 2 - 70;
		int pages = Math.max(1, (entries.size() + PER_PAGE - 1) / PER_PAGE);
		page = Math.min(page, pages - 1);
		int first = page * PER_PAGE;
		for (int i = first; i < Math.min(entries.size(), first + PER_PAGE); i++) {
			ClientboundGraceMenuPacket.Entry entry = entries.get(i);
			Component label = RealmClient.graceName(entry.name());
			if (entry.here()) label = Component.translatable("gui.remnant_bosses.grace.here", label);
			Button button = Button.builder(label, b -> travel(entry)).bounds(cx - 110, top + (i - first) * ROW, 220, 20).build();
			button.active = !entry.here();
			addRenderableWidget(button);
		}
		int bottom = top + PER_PAGE * ROW + 8;
		if (pages > 1) {
			Button prev = Button.builder(Component.literal("<"), b -> turn(-1)).bounds(cx - 110, bottom, 20, 20).build();
			Button next = Button.builder(Component.literal(">"), b -> turn(1)).bounds(cx + 90, bottom, 20, 20).build();
			prev.active = page > 0;
			next.active = page < pages - 1;
			addRenderableWidget(prev);
			addRenderableWidget(next);
		}
		Button depart = Button.builder(Component.translatable("gui.remnant_bosses.grace.depart"), b -> depart())
				.bounds(cx - 86, bottom, 84, 20).build();
		depart.active = menu.inRealm();
		addRenderableWidget(depart);
		addRenderableWidget(Button.builder(Component.translatable("gui.remnant_bosses.grace.leave"), b -> onClose())
				.bounds(cx + 2, bottom, 84, 20).build());
	}

	private void turn(int delta) {
		page += delta;
		rebuildWidgets();
	}

	private void travel(ClientboundGraceMenuPacket.Entry entry) {
		Services.NETWORK.sendGraceAction(new ServerboundGraceActionPacket(menu.grace(), ServerboundGraceActionPacket.TRAVEL,
				entry.dimension(), entry.pos()));
		onClose();
	}

	private void depart() {
		Services.NETWORK.sendGraceAction(new ServerboundGraceActionPacket(menu.grace(), ServerboundGraceActionPacket.DEPART,
				ResourceLocation.withDefaultNamespace("overworld"), menu.grace()));
		onClose();
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.renderBackground(graphics, mouseX, mouseY, partialTick);
		int cx = width / 2;
		int top = height / 2 - 70;
		graphics.fillGradient(cx - 130, top - 46, cx + 130, top + PER_PAGE * ROW + 36, 0xC0100D0A, 0xD8050407);
		graphics.fill(cx - 120, top - 14, cx + 120, top - 13, 0xA0E8C77A);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		int cx = width / 2;
		int top = height / 2 - 70;
		graphics.pose().pushPose();
		graphics.pose().translate(cx, top - 38, 0);
		graphics.pose().scale(1.6f, 1.6f, 1);
		graphics.drawCenteredString(font, RealmClient.graceName(menu.name()), 0, 0, GOLD);
		graphics.pose().popPose();
		graphics.drawCenteredString(font, Component.translatable("gui.remnant_bosses.grace.travel"), cx, top - 26 + 2, 0xBFB59A);
		if (entries.isEmpty()) graphics.drawCenteredString(font, Component.translatable("gui.remnant_bosses.grace.none"), cx, top + 10, 0x9A9080);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
