package com.nightbeam.remnants.client.realm;

import com.nightbeam.remnants.network.ClientboundGraceMenuPacket;
import com.nightbeam.remnants.network.ServerboundGraceActionPacket;
import com.nightbeam.remnants.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The grace menu, opened after resting: every discovered grace with its distance in meters, nearest first,
 * plus departing the Realm. The server re-validates every choice.
 */
public class GraceScreen extends Screen {
	private static final int PER_PAGE = 7;
	private static final int ROW = 22;
	private static final int PANEL_W = 260;
	private static final int GOLD = 0xE8C77A;
	private static final int PALE = 0xBFB59A;
	private final ClientboundGraceMenuPacket menu;
	private final List<ClientboundGraceMenuPacket.Entry> entries;
	private int page;

	public GraceScreen(ClientboundGraceMenuPacket menu) {
		super(Component.translatable("gui.remnant_bosses.grace.title"));
		this.menu = menu;
		this.entries = new ArrayList<>(menu.entries());
		this.entries.sort(Comparator.comparing((ClientboundGraceMenuPacket.Entry e) -> !e.here())
				.thenComparingDouble(GraceScreen::distance));
	}

	/** Meters from the player to a grace in this world, or infinity for a grace in another one. */
	static double distance(ClientboundGraceMenuPacket.Entry entry) {
		Player player = Minecraft.getInstance().player;
		if (player == null || !player.level().dimension().location().equals(entry.dimension())) return Double.POSITIVE_INFINITY;
		return Math.sqrt(player.distanceToSqr(Vec3.atBottomCenterOf(entry.pos())));
	}

	static Component distanceLabel(ClientboundGraceMenuPacket.Entry entry) {
		if (entry.here()) return Component.translatable("gui.remnant_bosses.grace.you_are_here");
		double meters = distance(entry);
		if (Double.isInfinite(meters)) return Component.translatable("gui.remnant_bosses.grace.elsewhere");
		return Component.translatable("gui.remnant_bosses.grace.distance", String.format("%,d", Math.round(meters)));
	}

	@Override
	protected void init() {
		int cx = width / 2;
		int top = height / 2 - 70;
		int pages = Math.max(1, (entries.size() + PER_PAGE - 1) / PER_PAGE);
		page = Math.min(page, pages - 1);
		int first = page * PER_PAGE;
		for (int i = first; i < Math.min(entries.size(), first + PER_PAGE); i++) {
			addRenderableWidget(new GraceRow(cx - PANEL_W / 2 + 10, top + (i - first) * ROW, PANEL_W - 20, entries.get(i)));
		}
		int bottom = top + PER_PAGE * ROW + 8;
		if (pages > 1) {
			Button prev = Button.builder(Component.literal("<"), b -> turn(-1)).bounds(cx - PANEL_W / 2 + 10, bottom, 20, 20).build();
			Button next = Button.builder(Component.literal(">"), b -> turn(1)).bounds(cx + PANEL_W / 2 - 30, bottom, 20, 20).build();
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
				new ResourceLocation("minecraft", "overworld"), menu.grace()));
		onClose();
	}

	private void drawPanel(GuiGraphics graphics) {
		int cx = width / 2;
		int top = height / 2 - 70;
		int x0 = cx - PANEL_W / 2, x1 = cx + PANEL_W / 2, y0 = top - 50, y1 = top + PER_PAGE * ROW + 36;
		graphics.fillGradient(x0, y0, x1, y1, 0xD0120E08, 0xE6050407);
		graphics.fill(x0, y0, x1, y0 + 1, 0x90E8C77A);
		graphics.fill(x0, y1 - 1, x1, y1, 0x90E8C77A);
		graphics.fill(x0 + 10, top - 14, x1 - 10, top - 13, 0xA0E8C77A);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		renderBackground(graphics);
		drawPanel(graphics);
		int cx = width / 2;
		int top = height / 2 - 70;
		graphics.pose().pushPose();
		graphics.pose().translate(cx, top - 42, 0);
		graphics.pose().scale(1.6f, 1.6f, 1);
		graphics.drawCenteredString(font, RealmClient.graceName(menu.name()), 0, 0, GOLD);
		graphics.pose().popPose();
		graphics.drawCenteredString(font, Component.translatable("gui.remnant_bosses.grace.travel")
				.append("  ·  ").append(Component.translatable("gui.remnant_bosses.grace.count", entries.size())), cx, top - 26, PALE);
		if (entries.isEmpty()) graphics.drawCenteredString(font, Component.translatable("gui.remnant_bosses.grace.none"), cx, top + 10, 0x9A9080);
		super.render(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	/** One grace: its name on the left, the distance on the right; click to travel there. */
	private final class GraceRow extends AbstractButton {
		private final ClientboundGraceMenuPacket.Entry entry;

		GraceRow(int x, int y, int width, ClientboundGraceMenuPacket.Entry entry) {
			super(x, y, width, 20, RealmClient.graceName(entry.name()));
			this.entry = entry;
			this.active = !entry.here();
		}

		@Override
		public void onPress() {
			travel(entry);
		}

		@Override
		protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
			boolean hot = active && isHoveredOrFocused();
			int x0 = getX(), y0 = getY(), x1 = getX() + getWidth(), y1 = getY() + getHeight();
			graphics.fillGradient(x0, y0, x1, y1, hot ? 0x60E8C77A : 0x30201810, hot ? 0x40B8913F : 0x20100C08);
			if (hot || entry.here()) graphics.fill(x0, y0, x0 + 2, y1, entry.here() ? 0xFFF6D068 : 0xFFE8C77A);
			int textY = y0 + (getHeight() - 8) / 2;
			Component marker = entry.here() ? Component.literal("✦ ").append(getMessage()) : getMessage();
			graphics.drawString(font, marker, x0 + 8, textY, entry.here() ? 0xFFF6D068 : hot ? 0xFFFFF0C4 : GOLD, true);
			Component distance = distanceLabel(entry);
			graphics.drawString(font, distance, x1 - 8 - font.width(distance), textY, hot ? 0xFFFFF0C4 : PALE, true);
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
			defaultButtonNarrationText(output);
		}
	}
}
