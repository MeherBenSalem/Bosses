package com.nightbeam.remnants.client.journal;

import com.nightbeam.remnants.journal.BossHunt;
import com.nightbeam.remnants.journal.HuntState;
import com.nightbeam.remnants.journal.RitualInfo;
import com.nightbeam.remnants.network.ClientboundJournalPacket;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

/** Personal, server-authored journal. All detail content is clipped to its own page. */
public final class BossHuntsScreen extends Screen {
    private static final int INK = 0xFF17131E;
    private static final int PAGE = 0xFF211B28;
    private static final int BORDER = 0xFF62516E;
    private static final int GOLD = 0xFFD4BC87;
    private static final int TEXT = 0xFFE6DBCA;
    private static final int MUTED = 0xFFB2A8B8;
    private static final int PURPLE = 0xFFC6A1E2;
    private static final String PREFIX = "gui.remnant_bosses.journal.";
    private static final String[] DIRECTIONS = {"east", "west", "south", "north"};
    private final EnumMap<BossHunt, ClientboundJournalPacket.Entry> entries = new EnumMap<>(BossHunt.class);
    private final List<DetailLine> lines = new ArrayList<>();
    private final List<EntryButton> entryButtons = new ArrayList<>();
    private JournalLayout layout;
    private BossHunt selected = BossHunt.OSSUKAGE;
    private int scroll;
    private int contentHeight;
    private Button up;
    private Button down;
    private Component detailNarration = Component.empty();

    public BossHuntsScreen(List<ClientboundJournalPacket.Entry> entries) {
        super(tr("title"));
        copyEntries(entries);
    }

    public void update(List<ClientboundJournalPacket.Entry> snapshot) {
        copyEntries(snapshot);
        if (minecraft != null && layout != null) {
            rebuildDetails();
            for (EntryButton button : entryButtons) button.updateDescription();
        }
    }

    private void copyEntries(List<ClientboundJournalPacket.Entry> snapshot) {
        entries.clear();
        for (ClientboundJournalPacket.Entry entry : snapshot) {
            BossHunt boss = BossHunt.fromId(entry.bossId());
            if (boss != null) entries.put(boss, entry);
        }
    }

    private static Component tr(String key, Object... arguments) {
        return Component.translatable(PREFIX + key, arguments);
    }

    private static Component bossName(BossHunt boss) {
        return Component.translatable("entity.remnants." + boss.key());
    }

    private HuntState state(BossHunt boss) {
        ClientboundJournalPacket.Entry entry = entries.get(boss);
        return entry == null ? HuntState.UNDISCOVERED : entry.state();
    }

    private Component stateName(BossHunt boss) {
        return tr("state." + state(boss).name().toLowerCase(Locale.ROOT));
    }

    @Override
    protected void init() {
        layout = JournalLayout.calculate(width, height);
        entryButtons.clear();
        for (BossHunt boss : BossHunt.values()) {
            EntryButton button = new EntryButton(boss, layout.entry(boss.ordinal()));
            entryButtons.add(addRenderableWidget(button));
        }
        JournalLayout.Rect footer = layout.footer();
        addRenderableWidget(Button.builder(tr("back"), button -> onClose())
                .bounds(footer.x(), footer.y(), 58, footer.height()).build());
        up = addRenderableWidget(Button.builder(tr("scroll_up"), button -> scrollBy(-36))
                .bounds(footer.right() - 48, footer.y(), 22, footer.height()).build());
        down = addRenderableWidget(Button.builder(tr("scroll_down"), button -> scrollBy(36))
                .bounds(footer.right() - 22, footer.y(), 22, footer.height()).build());
        up.setTooltip(Tooltip.create(tr("scroll_up_help")));
        down.setTooltip(Tooltip.create(tr("scroll_down_help")));
        rebuildDetails();
        setInitialFocus(entryButtons.get(selected.ordinal()));
    }

    private void select(BossHunt boss) {
        if (selected != boss) {
            selected = boss;
            scroll = 0;
            rebuildDetails();
            for (EntryButton button : entryButtons) button.updateDescription();
        }
        setFocused(entryButtons.get(selected.ordinal()));
    }

    private void rebuildDetails() {
        if (layout == null) return;
        lines.clear();
        detailNarration = Component.empty();
        contentHeight = 10;
        paragraph(bossName(selected), GOLD, 6);
        paragraph(stateName(selected), state(selected) == HuntState.DEFEATED ? GOLD : PURPLE, 12);
        if (state(selected) == HuntState.UNDISCOVERED) {
            paragraph(tr("clue_heading"), TEXT, 6);
            paragraph(tr("clue." + selected.key()), MUTED, 12);
            paragraph(tr("discover_hint"), MUTED, 8);
        } else {
            if (state(selected) == HuntState.DEFEATED) {
                paragraph(tr("completed"), GOLD, 12);
            }
            ClientboundJournalPacket.Entry entry = entries.get(selected);
            RitualInfo ritual = entry == null ? null : entry.ritual();
            if (!validRitual(ritual)) {
                paragraph(tr("ritual_unavailable"), MUTED, 8);
            } else {
                paragraph(tr("ritual_heading"), TEXT, 6);
                paragraph(tr("activation_heading"), PURPLE, 4);
                itemLine(item(ritual.activationItem()), 12);
                paragraph(tr("offerings_heading"), PURPLE, 4);
                for (int i = 0; i < DIRECTIONS.length; i++) {
                    paragraph(tr("direction." + DIRECTIONS[i]), MUTED, 2);
                    ResourceLocation blockId = ResourceLocation.tryParse(ritual.offerings().get(i));
                    var block = BuiltInRegistries.BLOCK.get(blockId);
                    itemLine(new ItemStack(block.asItem()), block.getName(), 8);
                }
                paragraph(tr("spacing"), MUTED, 10);
                paragraph(tr("activation_hint"), MUTED, 8);
            }
        }
        contentHeight += 4;
        scroll = JournalLayout.clampScroll(scroll, contentHeight, layout.details().height());
        updateScrollButtons();
    }

    private void paragraph(Component text, int color, int gap) {
        detailNarration = detailNarration.copy().append(text).append(Component.literal(". "));
        for (FormattedCharSequence wrapped : font.split(text, Math.max(12, layout.details().width() - 24))) {
            lines.add(new DetailLine(contentHeight, wrapped, ItemStack.EMPTY, color));
            contentHeight += font.lineHeight + 2;
        }
        contentHeight += gap;
    }

    private void itemLine(ItemStack stack, int gap) {
        itemLine(stack, stack.isEmpty() ? tr("unavailable") : stack.getHoverName(), gap);
    }

    private void itemLine(ItemStack stack, Component name, int gap) {
        detailNarration = detailNarration.copy().append(name).append(Component.literal(". "));
        List<FormattedCharSequence> wrapped = font.split(name, Math.max(12, layout.details().width() - 48));
        int top = contentHeight;
        for (int i = 0; i < wrapped.size(); i++) {
            lines.add(new DetailLine(contentHeight, wrapped.get(i), i == 0 ? stack : ItemStack.EMPTY, TEXT, true));
            contentHeight += font.lineHeight + 2;
        }
        contentHeight = Math.max(contentHeight, top + 18) + gap;
    }

    private static ItemStack item(String id) {
        if (id == null) return ItemStack.EMPTY;
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null || !BuiltInRegistries.ITEM.containsKey(location)) return ItemStack.EMPTY;
        Item item = BuiltInRegistries.ITEM.get(location);
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static boolean validRitual(RitualInfo ritual) {
        if (ritual == null || !ritual.valid() || ritual.offerings().size() != 4
                || item(ritual.activationItem()).isEmpty()) return false;
        for (String offering : ritual.offerings()) {
            ResourceLocation id = ResourceLocation.tryParse(offering);
            if (id == null || !BuiltInRegistries.BLOCK.containsKey(id)
                    || BuiltInRegistries.BLOCK.get(id) == net.minecraft.world.level.block.Blocks.AIR) return false;
        }
        return true;
    }

    private void scrollBy(int amount) {
        scroll = JournalLayout.clampScroll(scroll + amount, contentHeight, layout.details().height());
        updateScrollButtons();
    }

    private void updateScrollButtons() {
        if (up == null || down == null) return;
        up.active = scroll > 0;
        down.active = scroll < Math.max(0, contentHeight - layout.details().height());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xC0100D16);
        JournalLayout.Rect panel = layout.panel();
        graphics.fill(panel.x() + 3, panel.y() + 3, panel.right() + 3, panel.bottom() + 3, 0x80000000);
        frame(graphics, panel, INK, BORDER);
        graphics.fill(panel.x() + 8, panel.y() + 27, panel.right() - 8, panel.y() + 28, BORDER);
        graphics.drawString(font, elide(title, panel.width() - 24), panel.x() + 12, panel.y() + 11, GOLD, false);
        JournalLayout.Rect detail = layout.details();
        frame(graphics, detail, PAGE, 0xFF44394E);
        graphics.enableScissor(detail.x() + 1, detail.y() + 1, detail.right() - 1, detail.bottom() - 1);
        ItemStack hovered = ItemStack.EMPTY;
        for (DetailLine line : lines) {
            int y = detail.y() + line.y() - scroll;
            int textX = detail.x() + (line.itemRow() ? 32 : 10);
            graphics.drawString(font, line.text(), textX, y, line.color(), false);
            if (!line.item().isEmpty()) {
                graphics.renderItem(line.item(), detail.x() + 10, y - 2);
                if (detail.contains(mouseX, mouseY) && mouseX >= detail.x() + 8 && mouseX < detail.x() + 29
                        && mouseY >= y - 2 && mouseY < y + 16) hovered = line.item();
            }
        }
        graphics.disableScissor();
        if (contentHeight > detail.height() && detail.height() > 12) {
            int track = Math.max(1, detail.height() - 8);
            int thumb = Math.max(6, track * detail.height() / contentHeight);
            thumb = Math.min(track, thumb);
            int thumbY = detail.y() + 4 + (track - thumb) * scroll / (contentHeight - detail.height());
            graphics.fill(detail.right() - 5, detail.y() + 4, detail.right() - 3, detail.bottom() - 4, BORDER);
            graphics.fill(detail.right() - 5, thumbY, detail.right() - 3, thumbY + thumb, GOLD);
        }
        JournalLayout.Rect footer = layout.footer();
        if (footer.width() > 230) {
            graphics.drawString(font, elide(tr("controls"), footer.width() - 128), footer.x() + 66,
                    footer.y() + 5, MUTED, false);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        if (!hovered.isEmpty()) graphics.renderTooltip(font, hovered, mouseX, mouseY);
    }

    private String elide(Component component, int available) {
        String text = component.getString();
        if (font.width(text) <= available) return text;
        return font.plainSubstrByWidth(text, Math.max(0, available - font.width("…"))) + "…";
    }

    private static void frame(GuiGraphics graphics, JournalLayout.Rect rect, int fill, int border) {
        graphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), border);
        graphics.fill(rect.x() + 1, rect.y() + 1, rect.right() - 1, rect.bottom() - 1, fill);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double delta) {
        if (layout.details().contains(mouseX, mouseY)) {
            scrollBy((int) (-delta * 24));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_DOWN || keyCode == GLFW.GLFW_KEY_RIGHT
                || keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_LEFT) {
            int direction = keyCode == GLFW.GLFW_KEY_DOWN || keyCode == GLFW.GLFW_KEY_RIGHT ? 1 : -1;
            select(BossHunt.values()[Math.floorMod(selected.ordinal() + direction, 4)]);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_PAGE_DOWN || keyCode == GLFW.GLFW_KEY_PAGE_UP) {
            scrollBy((keyCode == GLFW.GLFW_KEY_PAGE_DOWN ? 1 : -1) * Math.max(12, layout.details().height() - 12));
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_HOME || keyCode == GLFW.GLFW_KEY_END) {
            scrollBy(keyCode == GLFW.GLFW_KEY_HOME ? -contentHeight : contentHeight);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public Component getNarrationMessage() { return title.copy().append(Component.literal(". ")).append(detailNarration); }

    private record DetailLine(int y, FormattedCharSequence text, ItemStack item, int color, boolean itemRow) {
        private DetailLine(int y, FormattedCharSequence text, ItemStack item, int color) {
            this(y, text, item, color, false);
        }
    }

    private final class EntryButton extends Button {
        private final BossHunt boss;
        private final ItemStack icon;

        private EntryButton(BossHunt boss, JournalLayout.Rect rect) {
            super(rect.x(), rect.y(), rect.width(), rect.height(), bossName(boss), button -> select(boss), DEFAULT_NARRATION);
            this.boss = boss;
            icon = item("remnant_bosses:" + (boss == BossHunt.OSSUKAGE ? "remnant_ossukage" : boss.key()) + "_spawn_egg");
            updateDescription();
        }

        private void updateDescription() {
            setTooltip(Tooltip.create(bossName(boss).copy().append(Component.literal(" — ")).append(stateName(boss))));
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean chosen = selected == boss;
            int border = isFocused() ? GOLD : chosen ? PURPLE : isHovered ? BORDER : 0xFF3B3244;
            frame(graphics, new JournalLayout.Rect(getX(), getY(), getWidth(), getHeight()), chosen ? 0xFF35273F : PAGE, border);
            graphics.fill(getX() + 1, getY() + 1, getX() + 3, getY() + getHeight() - 1, chosen ? PURPLE : BORDER);
            boolean showIcon = getWidth() >= (layout.compact() ? 100 : 64);
            if (showIcon && !icon.isEmpty()) graphics.renderItem(icon, getX() + 7, getY() + (getHeight() - 16) / 2);
            int labelX = getX() + (showIcon ? 29 : 7);
            int available = getWidth() - (showIcon ? 36 : 14);
            int labelY = getY() + (layout.compact() ? 8 : 9);
            Component label = layout.compact() ? tr("boss." + boss.key()) : bossName(boss);
            graphics.drawString(font, elide(label, available), labelX, labelY, chosen ? TEXT : MUTED, false);
            int stateColor = state(boss) == HuntState.DEFEATED ? GOLD : state(boss) == HuntState.ENCOUNTERED ? PURPLE : MUTED;
            if (layout.compact()) {
                graphics.fill(getX() + 7, getY() + getHeight() - 4, getX() + getWidth() - 7, getY() + getHeight() - 3, stateColor);
            } else {
                String badge = elide(stateName(boss), available - 6);
                graphics.fill(labelX - 2, getY() + 23, labelX + font.width(badge) + 3, getY() + 36, 0xFF17131E);
                graphics.drawString(font, badge, labelX, getY() + 26, stateColor, false);
            }
        }

        @Override
        public void updateWidgetNarration(NarrationElementOutput output) {
            output.add(NarratedElementType.TITLE, tr(selected == boss ? "selected_entry" : "entry", bossName(boss), stateName(boss)));
            output.add(NarratedElementType.HINT, selected == boss ? detailNarration : tr("select_hint"));
        }
    }
}
