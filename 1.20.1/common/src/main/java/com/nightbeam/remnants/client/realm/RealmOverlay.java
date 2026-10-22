package com.nightbeam.remnants.client.realm;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.nightbeam.remnants.Constants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * The Realm's cinematic HUD layer: the crossing (screen closing into a gold eclipse ring,
 * then fading open), area and region titles, LOST GRACE DISCOVERED, REMNANT FELLED and the Waygate's flash.
 * Banners are a dark band across the screen with large spaced capitals between two gold rules.
 */
public final class RealmOverlay {
	private static final ResourceLocation ECLIPSE = new ResourceLocation(Constants.MOD_ID, "textures/environment/eclipse.png");
	private static final int GOLD = 0xE8C77A;
	private static final int PALE_GOLD = 0xF3E3B5;
	private static final int ARRIVE_HOLD = 8;
	private static final int ARRIVE_FADE = 36;

	public enum Style {
		AREA(130, 3.0f, 44, GOLD, 0.62f),
		REGION(90, 2.0f, 30, PALE_GOLD, 0.45f),
		DISCOVERED(100, 2.6f, 40, GOLD, 0.6f),
		FELLED(140, 3.2f, 48, GOLD, 0.7f),
		RESTED(50, 1.6f, 24, PALE_GOLD, 0.35f);

		final int length;
		final float scale;
		final int band;
		final int color;
		final float bandAlpha;

		Style(int length, float scale, int band, int color, float bandAlpha) {
			this.length = length;
			this.scale = scale;
			this.band = band;
			this.color = color;
			this.bandAlpha = bandAlpha;
		}
	}

	private record Banner(Component title, Component subtitle, Style style, int delay) {
	}

	private static final Deque<Banner> QUEUE = new ArrayDeque<>();
	private static Banner current;
	private static int currentStart;
	private static int ticks;
	private static int crossStart = -1;
	private static int crossLength = 40;
	private static int arriveStart = -1;
	private static int flashStart = -1;
	private static float flashStrength;

	private RealmOverlay() {
	}

	/**
	 * Advance one client tick. While {@code paused} (the loading screen after a dimension change) every running
	 * effect is held in place, so the arrival fade and title play once the world is actually visible.
	 */
	public static void tick(boolean paused) {
		if (paused) {
			if (arriveStart >= 0) arriveStart++;
			if (current != null) currentStart++;
			if (flashStart >= 0) flashStart++;
			ticks++;
			return;
		}
		ticks++;
		if (current != null && ticks - currentStart >= current.style.length + current.delay) current = null;
		if (current == null && !QUEUE.isEmpty()) {
			current = QUEUE.poll();
			currentStart = ticks;
		}
		if (arriveStart >= 0 && ticks - arriveStart > ARRIVE_HOLD + ARRIVE_FADE) arriveStart = -1;
		if (crossStart >= 0 && ticks - crossStart > crossLength + 100) crossStart = -1;
	}

	public static void reset() {
		QUEUE.clear();
		current = null;
		crossStart = arriveStart = flashStart = -1;
	}

	public static void beginCrossing(int length) {
		crossStart = ticks;
		crossLength = Math.max(1, length);
		arriveStart = -1;
	}

	public static void arrive() {
		crossStart = -1;
		arriveStart = ticks;
	}

	public static void cancelCrossing() {
		crossStart = -1;
		arriveStart = ticks;
	}

	public static void flash(float strength) {
		flashStart = ticks;
		flashStrength = Mth.clamp(strength, 0, 1);
	}

	public static void banner(Component title, Component subtitle, Style style, int delay) {
		if (style == Style.REGION && current != null) return;
		QUEUE.add(new Banner(title, subtitle, style, Math.max(0, delay)));
	}

	public static boolean showingTitle() {
		return current != null || !QUEUE.isEmpty();
	}

	public static void render(GuiGraphics graphics, float partialTick) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null) return;
		float now = ticks + partialTick;
		int width = graphics.guiWidth(), height = graphics.guiHeight();
		RenderSystem.enableBlend();
		if (crossStart >= 0) renderCrossing(graphics, width, height, (now - crossStart) / crossLength);
		if (arriveStart >= 0) renderArrival(graphics, width, height, now - arriveStart);
		if (current != null) renderBanner(graphics, minecraft.font, width, height, now - currentStart - current.delay);
		if (flashStart >= 0) renderFlash(graphics, width, height, now - flashStart);
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShaderColor(1, 1, 1, 1);
	}

	/** 0..1: darkness rises from the edges while the gold eclipse ring grows, then the ring swallows the view. */
	private static void renderCrossing(GuiGraphics graphics, int width, int height, float progress) {
		float p = Mth.clamp(progress, 0, 1);
		float dark = smooth(Math.min(1, p / 0.85f));
		graphics.fill(0, 0, width, height, argb(dark * 0.92f + (p >= 1 ? 0.08f : 0), 0x000000));
		float base = Math.min(width, height);
		float size = p < 0.6f ? Mth.lerp(smooth(p / 0.6f), base * 0.15f, base * 0.55f)
				: Mth.lerp(smooth((p - 0.6f) / 0.4f), base * 0.55f, Math.max(width, height) * 3.2f);
		float alpha = p < 0.6f ? smooth(p / 0.6f) : 1 - smooth((p - 0.6f) / 0.4f);
		drawEclipse(graphics, width / 2f, height / 2f, size, alpha);
		if (p >= 1) graphics.fill(0, 0, width, height, 0xFF000000);
	}

	private static void renderArrival(GuiGraphics graphics, int width, int height, float age) {
		float alpha = age < ARRIVE_HOLD ? 1 : 1 - smooth(Math.min(1, (age - ARRIVE_HOLD) / ARRIVE_FADE));
		if (alpha <= 0.003f) return;
		graphics.fill(0, 0, width, height, argb(alpha, 0x000000));
		float base = Math.min(width, height);
		drawEclipse(graphics, width / 2f, height / 2f, base * Mth.lerp(1 - alpha, 0.55f, 0.1f), alpha * 0.8f);
	}

	private static void drawEclipse(GuiGraphics graphics, float cx, float cy, float size, float alpha) {
		if (alpha <= 0.01f || size < 1) return;
		RenderSystem.setShaderColor(1, 1, 1, alpha);
		graphics.pose().pushPose();
		graphics.pose().translate(cx - size / 2, cy - size / 2, 0);
		graphics.pose().scale(size / 128f, size / 128f, 1);
		graphics.blit(ECLIPSE, 0, 0, 0, 0, 128, 128, 128, 128);
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
		RenderSystem.setShaderColor(1, 0.92f, 0.75f, alpha * 0.6f);
		graphics.blit(ECLIPSE, 0, 0, 0, 0, 128, 128, 128, 128);
		RenderSystem.defaultBlendFunc();
		graphics.pose().popPose();
		RenderSystem.setShaderColor(1, 1, 1, 1);
	}

	private static void renderBanner(GuiGraphics graphics, Font font, int width, int height, float age) {
		if (age < 0) return;
		Style style = current.style;
		float in = smooth(Math.min(1, age / 14f));
		float out = 1 - smooth(Mth.clamp((age - (style.length - 22)) / 22f, 0, 1));
		float alpha = in * out;
		if (alpha <= 0.02f) return;
		int cy = (int) (height * (style == Style.REGION ? 0.30f : 0.42f));
		int band = style.band;
		// Dark band, strongest in the middle and fading to nothing at both screen edges.
		int slices = 48;
		for (int i = 0; i < slices; i++) {
			int x0 = width * i / slices, x1 = width * (i + 1) / slices;
			float edge = (float) Math.pow(Math.sin(Math.PI * (i + 0.5) / slices), 0.8);
			graphics.fill(x0, cy - band / 2, x1, cy + band / 2, argb(alpha * style.bandAlpha * edge, 0x050407));
			int rule = argb(alpha * 0.85f * edge * edge, style.color);
			graphics.fill(x0, cy - band / 2 - 1, x1, cy - band / 2, rule);
			graphics.fill(x0, cy + band / 2, x1, cy + band / 2 + 1, rule);
		}
		// Letters settle from wide to normal spacing as the title fades in.
		float spacing = Mth.lerp(in, 4.0f, 1.6f);
		boolean hasSubtitle = !current.subtitle.getString().isEmpty();
		float titleY = cy - (hasSubtitle ? 6 : 0) - font.lineHeight * style.scale / 2f;
		drawSpaced(graphics, font, current.title, width / 2f, titleY, style.scale, spacing, argb(alpha, style.color));
		if (hasSubtitle) {
			drawSpaced(graphics, font, current.subtitle, width / 2f, cy + band / 2f - font.lineHeight * 1.25f - 2,
					1.25f, 0.6f, argb(alpha * 0.9f, PALE_GOLD));
		}
	}

	private static void drawSpaced(GuiGraphics graphics, Font font, Component text, float cx, float y, float scale,
			float spacing, int color) {
		if (((color >>> 24) & 0xFF) < 6) return;
		String raw = text.getString();
		float width = 0;
		for (int i = 0; i < raw.length(); i++) width += font.width(String.valueOf(raw.charAt(i))) + (i > 0 ? spacing : 0);
		graphics.pose().pushPose();
		graphics.pose().translate(cx - width * scale / 2f, y, 0);
		graphics.pose().scale(scale, scale, 1);
		float x = 0;
		for (int i = 0; i < raw.length(); i++) {
			String c = String.valueOf(raw.charAt(i));
			FormattedCharSequence seq = Component.literal(c).withStyle(text.getStyle()).getVisualOrderText();
			graphics.drawString(font, seq, (int) x, 0, color, true);
			x += font.width(c) + spacing;
		}
		graphics.pose().popPose();
	}

	private static void renderFlash(GuiGraphics graphics, int width, int height, float age) {
		float alpha = flashStrength * (1 - smooth(Math.min(1, age / 26f)));
		if (alpha <= 0.01f) {
			flashStart = -1;
			return;
		}
		graphics.fill(0, 0, width, height, argb(alpha * 0.85f, 0xFFF4D6));
	}

	private static float smooth(float t) {
		t = Mth.clamp(t, 0, 1);
		return t * t * (3 - 2 * t);
	}

	private static int argb(float alpha, int rgb) {
		int a = Mth.clamp((int) (alpha * 255), 0, 255);
		return a << 24 | (rgb & 0xFFFFFF);
	}
}
