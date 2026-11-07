package com.mythweave.client.journal;

import com.mythweave.client.MythweaveClient;
import com.mythweave.entity.base.MythlingEntity;
import com.mythweave.evolution.EvolutionLine;
import com.mythweave.evolution.EvolutionRules;
import com.mythweave.evolution.EvolutionRules.Branch;
import com.mythweave.journal.JournalAttachments;
import com.mythweave.journal.JournalData;
import com.mythweave.journal.MythlingCatalog;
import com.mythweave.journal.MythlingProfile;
import com.mythweave.journal.TamedMythling;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The Mythweave creature journal: the player's bonded Mythlings on one tab, every Mythling
 * form on the other. Undiscovered forms are shown as {@code ???} until the player has seen
 * one in the world or bonded with it.
 *
 * <p>The screen is a thin client of {@link JournalData}, which the server keeps authoritative
 * and syncs only to its owner. It draws everything with plain fills, vanilla item icons and
 * the existing entity renderers, so it needs no new textures.
 */
public class MythlingJournalScreen extends Screen {
	// Layout: a fixed-size panel centred on screen. The list is a column of cards on the
	// left; the selected entry's full page fills the right.
	private static final int PANEL_MAX_WIDTH = 440;
	private static final int PANEL_MAX_HEIGHT = 300;
	private static final int CARD_HEIGHT = 34;
	private static final int CARD_GAP = 3;
	private static final int LIST_WIDTH = 140;
	private static final int PAGE_BUTTON = 16;
	private static final int DETAIL_PAD = 8;
	private static final int FILTER_HEIGHT = 14;
	private static final int FILTER_GAP = 3;

	// Vanilla-ish journal colours: warm dark browns with an ember accent.
	private static final int PANEL_BG = 0xF01B1410;
	private static final int PANEL_INNER = 0xE0241A13;
	private static final int PANEL_BORDER = 0xFF4A382B;
	private static final int CARD_BG = 0xE0332820;
	private static final int CARD_HOVER = 0xE044352A;
	private static final int CARD_SELECTED = 0xE05A4330;
	private static final int CARD_BORDER = 0xFF3A2C22;
	private static final int TAB_ACTIVE = 0xFF5A4330;
	private static final int TAB_INACTIVE = 0xE02A2018;
	private static final int TEXT = 0xFFF0E6D2;
	private static final int TEXT_DIM = 0xFFA89880;
	private static final int ACCENT = 0xFFFFB347;
	private static final int GOOD = 0xFF7CCF6A;
	private static final int BAR_BG = 0xFF2A2018;
	private static final int BAR_BORDER = 0xFF4A382B;

	private Tab tab = Tab.TAMED;
	private int page;
	private UUID selectedTamedId;
	private String selectedFormId;
	/** How far the detail page is scrolled, in pixels, when its entry is taller than the panel. */
	private int detailScroll;
	/** Measured last frame; used to clamp scrolling before the next draw. */
	private int detailContentHeight;

	/** One dummy entity per form, used only for the 3D preview. */
	private final Map<String, LivingEntity> previews = new HashMap<>();

	/** The name search field in the tab row. */
	private EditBox searchBox;
	private String searchQuery = "";
	/** The chosen rarity label, or null for "Any". */
	private String rarityFilter;
	/** The chosen element translation key, or null for "Any". */
	private String elementFilter;

	// Computed in init(); the click handlers use the same numbers as the renderer.
	private int panelLeft;
	private int panelTop;
	private int panelWidth;
	private int panelHeight;
	private int tabTamedX;
	private int tabMythlingsX;
	private int tabY;
	private int tabTamedWidth;
	private int tabMythlingsWidth;
	private int tabHeight;
	private int closeX;
	private int closeY;
	private int closeSize;
	private int searchX;
	private int searchY;
	private int searchWidth;
	private int searchHeight;
	private int listX;
	private int rarityFilterY;
	private int elementFilterY;
	private int listHeaderY;
	private int listY;
	private int contentBottom;
	private int cardsPerPage;
	private int pageY;
	private int detailX;
	private int detailY;
	private int detailWidth;
	private int detailHeight;

	public MythlingJournalScreen() {
		super(Component.translatable("screen.mythweave.journal"));
	}

	@Override
	protected void init() {
		this.computeLayout();
		this.createSearchBox();
	}

	@Override
	protected void repositionElements() {
		this.computeLayout();
		if (this.searchBox != null) {
			this.placeSearchBox();
		}
	}

	private void createSearchBox() {
		this.searchBox = new EditBox(this.font, this.searchX, this.searchY, this.searchWidth, this.searchHeight,
				Component.translatable("screen.mythweave.journal.search_hint"));
		this.searchBox.setBordered(false);
		this.searchBox.setMaxLength(40);
		this.searchBox.setHint(Component.translatable("screen.mythweave.journal.search_hint"));
		this.searchBox.setTextColor(TEXT);
		this.searchBox.setTextColorUneditable(TEXT);
		this.searchBox.setResponder(value -> {
			this.searchQuery = value;
			this.page = 0;
		});
		this.placeSearchBox();
		this.addRenderableWidget(this.searchBox);
	}

	/** Keeps the widget's text off the hand-drawn border. */
	private void placeSearchBox() {
		int padX = 4;
		int padY = (this.searchHeight - this.font.lineHeight) / 2;
		this.searchBox.setX(this.searchX + padX);
		this.searchBox.setY(this.searchY + padY);
		this.searchBox.setWidth(Math.max(0, this.searchWidth - padX * 2));
		this.searchBox.setHeight(Math.max(this.font.lineHeight, this.searchHeight - padY * 2));
	}

	@Override
	protected void setInitialFocus() {
		// Leave keyboard focus off the search field so the journal key can close the screen.
		// Clicking the field focuses it; Escape still closes while typing.
	}

	/** Recomputes every panel rectangle. Called on open and again whenever the window resizes. */
	private void computeLayout() {
		this.panelWidth = Math.min(PANEL_MAX_WIDTH, this.width - 20);
		this.panelHeight = Math.min(PANEL_MAX_HEIGHT, this.height - 20);
		this.panelLeft = (this.width - this.panelWidth) / 2;
		this.panelTop = (this.height - this.panelHeight) / 2;

		this.tabY = this.panelTop + 26;
		this.tabHeight = 18;
		this.tabTamedX = this.panelLeft + 10;
		this.tabTamedWidth = 64;
		this.tabMythlingsX = this.tabTamedX + this.tabTamedWidth + 4;
		this.tabMythlingsWidth = 80;

		this.closeSize = 12;
		this.closeX = this.panelLeft + this.panelWidth - this.closeSize - 8;
		this.closeY = this.panelTop + 8;

		// Search sits in the tab row, between the tabs and the close button.
		this.searchX = this.tabMythlingsX + this.tabMythlingsWidth + 6;
		this.searchY = this.tabY;
		this.searchWidth = Math.max(0, this.closeX - this.searchX - 6);
		this.searchHeight = this.tabHeight;

		int contentTop = this.panelTop + 48;
		this.listX = this.panelLeft + 10;
		this.rarityFilterY = contentTop;
		this.elementFilterY = contentTop + FILTER_HEIGHT + FILTER_GAP;
		this.listHeaderY = this.elementFilterY + FILTER_HEIGHT + 5;
		this.listY = this.listHeaderY + 12;
		this.contentBottom = this.panelTop + this.panelHeight - 10;
		int available = Math.max(CARD_HEIGHT, this.contentBottom - this.listY - 22);
		this.cardsPerPage = Math.max(1, (available + CARD_GAP) / (CARD_HEIGHT + CARD_GAP));
		this.pageY = this.listY + this.cardsPerPage * (CARD_HEIGHT + CARD_GAP) - CARD_GAP + 6;

		this.detailX = this.listX + LIST_WIDTH + 12;
		// The detail page keeps its full height even though the filters push the list down.
		this.detailY = contentTop;
		this.detailWidth = this.panelLeft + this.panelWidth - 10 - this.detailX;
		this.detailHeight = this.contentBottom - this.detailY;

		// A form selected before a resize is still valid; the entry lookup revalidates it.
	}

	@Override
	public boolean isPauseScreen() {
		// Keep the world running so tamed progress and the live journal keep updating.
		return false;
	}

	@Override
	public boolean isInGameUi() {
		// Like a book or inventory: the world stays visible behind the panel.
		return true;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		// Dim the world instead of the dirt/blur menu background.
		this.extractTransparentBackground(graphics);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		JournalData data = this.data();
		this.sanitizeFilters(data);

		graphics.fill(this.panelLeft, this.panelTop,
				this.panelLeft + this.panelWidth, this.panelTop + this.panelHeight, PANEL_BG);
		graphics.outline(this.panelLeft, this.panelTop, this.panelWidth, this.panelHeight, PANEL_BORDER);
		graphics.horizontalLine(this.panelLeft + 1, this.panelLeft + this.panelWidth - 1,
				this.panelTop + 24, PANEL_BORDER);
		graphics.centeredText(this.font, this.title,
				this.panelLeft + this.panelWidth / 2, this.panelTop + 8, ACCENT);

		this.drawCloseButton(graphics, mouseX, mouseY);
		this.drawTabs(graphics);
		this.drawSearchField(graphics, mouseX, mouseY);
		this.drawFilters(graphics, mouseX, mouseY, data);
		this.drawList(graphics, mouseX, mouseY, data);
		this.drawDetail(graphics, mouseX, mouseY, data);

		// Widgets (the search box) draw last so they sit on top of the custom panel.
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
	}

	// ------------------------------------------------------------------
	// Header, tabs and list
	// ------------------------------------------------------------------

	private void drawCloseButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		boolean hovered = this.overClose(mouseX, mouseY);
		if (hovered) {
			graphics.fill(this.closeX - 2, this.closeY - 2,
					this.closeX + this.closeSize + 2, this.closeY + this.closeSize + 2, 0x40FFFFFF);
		}
		graphics.centeredText(this.font, "x",
				this.closeX + this.closeSize / 2, this.closeY + 2, hovered ? TEXT : TEXT_DIM);
	}

	private void drawTabs(GuiGraphicsExtractor graphics) {
		this.drawTab(graphics, this.tabTamedX, this.tabTamedWidth,
				Component.translatable("screen.mythweave.journal.tab.tamed"), this.tab == Tab.TAMED);
		this.drawTab(graphics, this.tabMythlingsX, this.tabMythlingsWidth,
				Component.translatable("screen.mythweave.journal.tab.mythlings"), this.tab == Tab.MYTHLINGS);
	}

	private void drawTab(GuiGraphicsExtractor graphics, int x, int width, Component label, boolean active) {
		graphics.fill(x, this.tabY, x + width, this.tabY + this.tabHeight, active ? TAB_ACTIVE : TAB_INACTIVE);
		graphics.outline(x, this.tabY, width, this.tabHeight, active ? ACCENT : CARD_BORDER);
		if (active) {
			graphics.fill(x + 1, this.tabY + this.tabHeight - 2, x + width - 1, this.tabY + this.tabHeight, ACCENT);
		}
		graphics.centeredText(this.font, label, x + width / 2, this.tabY + 5, active ? TEXT : TEXT_DIM);
	}

	private void drawSearchField(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		if (this.searchWidth <= 0) {
			return;
		}
		boolean focused = this.searchBox != null && this.searchBox.isFocused();
		graphics.fill(this.searchX, this.searchY,
				this.searchX + this.searchWidth, this.searchY + this.searchHeight, CARD_BG);
		graphics.outline(this.searchX, this.searchY, this.searchWidth, this.searchHeight,
				focused ? ACCENT : CARD_BORDER);
	}

	private void drawFilters(GuiGraphicsExtractor graphics, int mouseX, int mouseY, JournalData data) {
		Component any = Component.translatable("screen.mythweave.journal.filter.any");
		Component rarityLabel = Component.translatable("screen.mythweave.journal.filter.rarity",
				this.rarityFilter == null ? any : this.rarityFilter);
		Component elementLabel = Component.translatable("screen.mythweave.journal.filter.element",
				this.elementFilter == null ? any : Component.translatable(this.elementFilter));

		boolean rarityHovered = this.isInside(mouseX, mouseY,
				this.listX, this.rarityFilterY, LIST_WIDTH, FILTER_HEIGHT);
		boolean elementHovered = this.isInside(mouseX, mouseY,
				this.listX, this.elementFilterY, LIST_WIDTH, FILTER_HEIGHT);
		boolean rarityDisabled = this.rarityOptions(data).isEmpty();
		boolean elementDisabled = this.elementOptions(data).isEmpty();

		this.drawFilterChip(graphics, this.listX, this.rarityFilterY, FILTER_HEIGHT,
				rarityLabel, rarityHovered, rarityDisabled);
		this.drawFilterChip(graphics, this.listX, this.elementFilterY, FILTER_HEIGHT,
				elementLabel, elementHovered, elementDisabled);

		if (rarityHovered) {
			graphics.setTooltipForNextFrame(this.font, rarityLabel, mouseX, mouseY);
		}
		if (elementHovered) {
			graphics.setTooltipForNextFrame(this.font, elementLabel, mouseX, mouseY);
		}
	}

	private void drawFilterChip(GuiGraphicsExtractor graphics, int x, int y, int height,
			Component label, boolean hovered, boolean disabled) {
		int background = disabled ? 0x401B1410 : hovered ? CARD_HOVER : CARD_BG;
		graphics.fill(x, y, x + LIST_WIDTH, y + height, background);
		graphics.outline(x, y, LIST_WIDTH, height,
				disabled ? 0xFF2A2018 : hovered ? ACCENT : CARD_BORDER);
		String text = this.ellipsize(label.getString(), LIST_WIDTH - 8);
		graphics.centeredText(this.font, text, x + LIST_WIDTH / 2,
				y + (height - this.font.lineHeight) / 2, disabled ? 0xFF5A4A3A : TEXT);
	}

	private void drawList(GuiGraphicsExtractor graphics, int mouseX, int mouseY, JournalData data) {
		List<Entry> entries = this.entries(data);
		int pages = this.pageCount(entries.size());
		this.page = Mth.clamp(this.page, 0, pages - 1);

		Component count;
		if (this.tab == Tab.TAMED) {
			count = this.hasSearchOrFilter()
					? Component.translatable("screen.mythweave.journal.tamed_count_filtered",
							entries.size(), data.tamed().size())
					: Component.translatable("screen.mythweave.journal.tamed_count", entries.size());
		} else {
			count = Component.translatable("screen.mythweave.journal.discovered_count",
					this.discoveredCount(data), MythlingCatalog.profiles().size());
		}
		graphics.text(this.font, count, this.listX, this.listHeaderY, TEXT_DIM);

		int start = this.page * this.cardsPerPage;
		int end = Math.min(entries.size(), start + this.cardsPerPage);
		for (int i = start; i < end; i++) {
			Entry entry = entries.get(i);
			int cardY = this.listY + (i - start) * (CARD_HEIGHT + CARD_GAP);
			boolean hovered = this.isInside(mouseX, mouseY, this.listX, cardY, LIST_WIDTH, CARD_HEIGHT);
			this.drawCard(graphics, entry, this.listX, cardY, LIST_WIDTH, CARD_HEIGHT,
					this.isSelected(entry), hovered);
		}

		if (entries.isEmpty()) {
			Component empty = this.hasSearchOrFilter()
					? Component.translatable("screen.mythweave.journal.no_results")
					: Component.translatable("screen.mythweave.journal.empty_tamed");
			this.drawWrapped(graphics, empty, this.listX, this.listY + 4, LIST_WIDTH, TEXT_DIM, 5);
		}

		this.drawPageControls(graphics, mouseX, mouseY, entries.size());
	}

	private void drawCard(GuiGraphicsExtractor graphics, Entry entry, int x, int y,
			int width, int height, boolean selected, boolean hovered) {
		int background = selected ? CARD_SELECTED : hovered ? CARD_HOVER : CARD_BG;
		graphics.fill(x, y, x + width, y + height, background);
		graphics.outline(x, y, width, height, selected ? ACCENT : CARD_BORDER);

		if (entry.discovered()) {
			graphics.fill(x + 3, y + 7, x + 23, y + 27, 0x8018100C);
			graphics.item(new ItemStack(MythlingCatalog.spawnEgg(entry.formId())), x + 5, y + 9);
		} else {
			graphics.fill(x + 3, y + 7, x + 23, y + 27, 0xFF1A130E);
			graphics.outline(x + 3, y + 7, 20, 20, CARD_BORDER);
			graphics.centeredText(this.font, "?", x + 13, y + 13, TEXT_DIM);
		}

		// Never leak an undiscovered form's name, even in the list.
		String title = entry.discovered()
				? entry.displayName()
				: Component.translatable("screen.mythweave.journal.undiscovered").getString();
		graphics.text(this.font, this.ellipsize(title, width - 30), x + 28, y + 6, TEXT);
		String subtitle = this.ellipsize(this.subtitle(entry).getString(), width - 30);
		graphics.text(this.font, subtitle, x + 28, y + 18, TEXT_DIM);
	}

	private Component subtitle(Entry entry) {
		if (!entry.discovered()) {
			return Component.translatable("screen.mythweave.journal.undiscovered_subtitle");
		}
		if (entry.tamed() != null) {
			// A named companion already shows its name on the top line, so the second line
			// names the form; an unnamed one has room to show its Kindle instead.
			if (!entry.tamed().name().isEmpty()) {
				return EvolutionLine.formName(entry.formId());
			}
			return Component.translatable("screen.mythweave.journal.card_kindle", entry.tamed().kindle());
		}
		int stage = MythlingCatalog.stageOf(entry.formId());
		if (stage == 3) {
			return Component.translatable("screen.mythweave.journal.stage.final");
		}
		if (stage > 0) {
			return Component.translatable("screen.mythweave.journal.stage", stage);
		}
		return Component.translatable("screen.mythweave.journal.undiscovered");
	}

	private void drawPageControls(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int size) {
		int pages = this.pageCount(size);
		boolean prev = this.page > 0;
		boolean next = this.page < pages - 1;
		boolean prevHovered = prev && this.isInside(mouseX, mouseY, this.listX, this.pageY, PAGE_BUTTON, PAGE_BUTTON);
		boolean nextHovered = next && this.isInside(mouseX, mouseY,
				this.listX + LIST_WIDTH - PAGE_BUTTON, this.pageY, PAGE_BUTTON, PAGE_BUTTON);

		this.drawArrowButton(graphics, this.listX, this.pageY, prev, prevHovered, "<");
		this.drawArrowButton(graphics, this.listX + LIST_WIDTH - PAGE_BUTTON, this.pageY, next, nextHovered, ">");
		graphics.centeredText(this.font,
				Component.translatable("screen.mythweave.journal.page", this.page + 1, pages),
				this.listX + LIST_WIDTH / 2, this.pageY + 4, TEXT_DIM);
	}

	private void drawArrowButton(GuiGraphicsExtractor graphics, int x, int y,
			boolean active, boolean hovered, String arrow) {
		int background = hovered ? CARD_HOVER : CARD_BG;
		graphics.fill(x, y, x + PAGE_BUTTON, y + PAGE_BUTTON, active ? background : 0x601B1410);
		graphics.outline(x, y, PAGE_BUTTON, PAGE_BUTTON, active ? CARD_BORDER : 0xFF2A2018);
		graphics.centeredText(this.font, arrow, x + PAGE_BUTTON / 2, y + 4,
				active ? (hovered ? TEXT : TEXT_DIM) : 0xFF5A4A3A);
	}

	// ------------------------------------------------------------------
	// Detail page
	// ------------------------------------------------------------------

	private void drawDetail(GuiGraphicsExtractor graphics, int mouseX, int mouseY, JournalData data) {
		graphics.fill(this.detailX, this.detailY,
				this.detailX + this.detailWidth, this.detailY + this.detailHeight, PANEL_INNER);
		graphics.outline(this.detailX, this.detailY, this.detailWidth, this.detailHeight, PANEL_BORDER);

		Entry entry = this.selectedEntry(data);
		if (entry == null) {
			this.detailContentHeight = 0;
			Component message = this.hasSearchOrFilter()
					? Component.translatable("screen.mythweave.journal.no_results")
					: Component.translatable("screen.mythweave.journal.select");
			this.drawCenteredWrapped(graphics, message,
					this.detailX + this.detailWidth / 2, this.detailY + this.detailHeight / 2 - 10,
					this.detailWidth - 24, TEXT_DIM);
			return;
		}

		// The page can be taller than the panel on small windows, so scroll it instead of
		// letting the evolution details fall off the bottom.
		this.detailScroll = Mth.clamp(this.detailScroll, 0, this.maxDetailScroll());
		graphics.enableScissor(this.detailX, this.detailY,
				this.detailX + this.detailWidth, this.detailY + this.detailHeight);
		if (!entry.discovered()) {
			this.detailContentHeight = 0;
			this.drawUndiscovered(graphics);
		} else {
			int bottom = this.drawEntry(graphics, entry, mouseX, mouseY);
			this.detailContentHeight = bottom + this.detailScroll - (this.detailY + DETAIL_PAD);
		}
		graphics.disableScissor();
		this.drawScrollbar(graphics);
	}

	private int maxDetailScroll() {
		int viewport = this.detailHeight - DETAIL_PAD * 2;
		return Math.max(0, this.detailContentHeight - viewport);
	}

	private void drawScrollbar(GuiGraphicsExtractor graphics) {
		int maxScroll = this.maxDetailScroll();
		if (maxScroll <= 0) {
			return;
		}
		int x = this.detailX + this.detailWidth - 4;
		int top = this.detailY + 6;
		int height = this.detailHeight - 12;
		graphics.fill(x, top, x + 2, top + height, 0x40FFFFFF);
		int viewport = this.detailHeight - DETAIL_PAD * 2;
		int thumb = Math.max(12, height * viewport / (viewport + maxScroll));
		int thumbY = top + (height - thumb) * this.detailScroll / maxScroll;
		graphics.fill(x, thumbY, x + 2, thumbY + thumb, 0x80FFB347);
	}

	private void drawUndiscovered(GuiGraphicsExtractor graphics) {
		int centerX = this.detailX + this.detailWidth / 2;
		graphics.centeredText(this.font, Component.translatable("screen.mythweave.journal.undiscovered"),
				centerX, this.detailY + 10, TEXT_DIM);

		int boxWidth = 80;
		int boxHeight = 60;
		int boxX = centerX - boxWidth / 2;
		int boxY = this.detailY + 30;
		graphics.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0x8018100C);
		graphics.outline(boxX, boxY, boxWidth, boxHeight, CARD_BORDER);
		graphics.centeredText(this.font, "?", centerX, boxY + boxHeight / 2 - 4, TEXT_DIM);

		this.drawCenteredWrapped(graphics,
				Component.translatable("screen.mythweave.journal.undiscovered_hint"),
				centerX, boxY + boxHeight + 12, this.detailWidth - 24, TEXT_DIM);
	}

	/** Draws one full entry and returns the y just past its last line (in screen coordinates). */
	private int drawEntry(GuiGraphicsExtractor graphics, Entry entry, int mouseX, int mouseY) {
		MythlingProfile profile = MythlingCatalog.profile(entry.formId()).orElse(null);
		int x = this.detailX + DETAIL_PAD;
		int y = this.detailY + DETAIL_PAD - this.detailScroll;

		String display = entry.displayName();
		if (entry.tamed() != null && !entry.tamed().name().isEmpty()) {
			display += " (" + EvolutionLine.formName(entry.formId()).getString() + ")";
		}
		graphics.text(this.font, this.ellipsize(display, this.detailWidth - 70), x, y, TEXT);
		int stage = MythlingCatalog.stageOf(entry.formId());
		String stageText = stage == 3
				? Component.translatable("screen.mythweave.journal.stage.final").getString()
				: stage > 0 ? Component.translatable("screen.mythweave.journal.stage", stage).getString() : "";
		if (!stageText.isEmpty()) {
			graphics.text(this.font, stageText,
					this.detailX + this.detailWidth - 8 - this.font.width(stageText), y, TEXT_DIM);
		}
		y += this.font.lineHeight + 4;

		int previewWidth = 60;
		int previewHeight = 50;
		int previewX = x;
		int previewY = y;
		graphics.fill(previewX, previewY, previewX + previewWidth, previewY + previewHeight, 0x8018100C);
		graphics.outline(previewX, previewY, previewWidth, previewHeight, CARD_BORDER);
		LivingEntity preview = this.previewEntity(entry.formId());
		if (preview != null) {
			InventoryScreen.extractEntityInInventoryFollowsMouse(graphics,
					previewX, previewY, previewX + previewWidth, previewY + previewHeight,
					30, 0.0625F, mouseX, mouseY, preview);
		}

		int infoX = previewX + previewWidth + 10;
		int infoWidth = this.detailX + this.detailWidth - 8 - infoX;
		int infoY = previewY + 2;
		if (profile != null) {
			infoY = this.drawKeyLine(graphics, Component.translatable("screen.mythweave.journal.element"),
					this.elements(profile), infoX, infoY, infoWidth);
			infoY = this.drawKeyLine(graphics, Component.translatable("screen.mythweave.journal.rarity"),
					Component.translatable(profile.rarityKey()), infoX, infoY, infoWidth);
			infoY = this.drawKeyLine(graphics, Component.translatable("screen.mythweave.journal.habitat"),
					Component.translatable(profile.habitatKey()), infoX, infoY, infoWidth);
		}
		if (entry.tamed() != null) {
			infoY = this.drawKeyLine(graphics, Component.translatable("screen.mythweave.journal.status"),
					this.status(entry.tamed()), infoX, infoY, infoWidth);
		}

		int bodyY = Math.max(previewY + previewHeight, infoY) + 6;

		if (profile != null) {
			bodyY = this.drawWrapped(graphics, Component.translatable(profile.descriptionKey()),
					x, bodyY, this.detailWidth - 16, TEXT_DIM, 3);
			bodyY += 3;
			bodyY = this.drawWrapped(graphics, Component.translatable(
							"screen.mythweave.journal.stats_line",
							this.stat(MythlingCatalog.maxHealth(entry.formId())),
							this.stat(MythlingCatalog.armor(entry.formId())),
							this.stat(MythlingCatalog.attackDamage(entry.formId())),
							this.stat(MythlingCatalog.width(entry.formId())),
							this.stat(MythlingCatalog.height(entry.formId()))),
					x, bodyY, this.detailWidth - 16, TEXT, 2);
			if (!profile.abilityKeys().isEmpty()) {
				bodyY = this.drawWrapped(graphics, this.traits(profile),
						x, bodyY, this.detailWidth - 16, TEXT_DIM, 2);
			}
		}

		if (entry.tamed() != null) {
			bodyY = this.drawHealthBar(graphics, entry.tamed(), x, bodyY, this.detailWidth - 16);
		}

		if (profile != null) {
			ItemStack bonding = new ItemStack(profile.bondingItem());
			graphics.item(bonding, x, bodyY);
			graphics.text(this.font, Component.translatable("screen.mythweave.journal.bonding_item",
					bonding.getHoverName()), x + 20, bodyY + 4, TEXT_DIM);
			if (this.isInside(mouseX, mouseY, x, bodyY, 16, 16)) {
				graphics.setTooltipForNextFrame(this.font, bonding, mouseX, mouseY);
			}
			bodyY += 20;
		}

		bodyY += 2;
		return this.drawEvolution(graphics, x, bodyY, this.detailWidth - 16, entry);
	}

	private int drawKeyLine(GuiGraphicsExtractor graphics, Component label, Component value,
			int x, int y, int width) {
		graphics.text(this.font, label, x, y, TEXT_DIM);
		int labelWidth = this.font.width(label.getString()) + 4;
		if (labelWidth + this.font.width(value.getString()) <= width) {
			graphics.text(this.font, value, x + labelWidth, y, TEXT);
			return y + this.font.lineHeight;
		}
		return this.drawWrapped(graphics, value, x, y + this.font.lineHeight, width, TEXT, 2);
	}

	private int drawHealthBar(GuiGraphicsExtractor graphics, TamedMythling mythling,
			int x, int y, int width) {
		graphics.text(this.font, Component.translatable("screen.mythweave.journal.health",
				this.stat(mythling.health()), this.stat(mythling.maxHealth())), x, y, TEXT_DIM);
		y += this.font.lineHeight;
		float ratio = mythling.maxHealth() <= 0.0F
				? 0.0F : Mth.clamp(mythling.health() / mythling.maxHealth(), 0.0F, 1.0F);
		int colour = ratio > 0.5F ? GOOD : ratio > 0.25F ? 0xFFFFC04D : 0xFFE05A4D;
		y = this.drawBar(graphics, x, y, width, ratio, colour);
		return y + 2;
	}

	/** Draws the evolution diagram and returns the y just past its last line. */
	private int drawEvolution(GuiGraphicsExtractor graphics, int x, int y, int width, Entry entry) {
		graphics.text(this.font, Component.translatable("screen.mythweave.journal.evolution"), x, y, ACCENT);
		y += this.font.lineHeight + 1;

		int stage = MythlingCatalog.stageOf(entry.formId());
		Optional<EvolutionLine> maybeLine = MythlingCatalog.lineOf(entry.formId());
		if (maybeLine.isEmpty() || stage <= 0) {
			graphics.text(this.font, Component.translatable("screen.mythweave.journal.evolution.unknown"),
					x, y, TEXT_DIM);
			return y + this.font.lineHeight;
		}
		EvolutionLine line = maybeLine.get();
		int needed = EvolutionRules.kindleToEvolveFrom(stage);
		if (needed < 0) {
			graphics.text(this.font, Component.translatable("screen.mythweave.journal.evolution.final"),
					x, y, TEXT_DIM);
			return y + this.font.lineHeight;
		}

		TamedMythling tamed = entry.tamed();
		int kindle = tamed == null ? 0 : tamed.kindle();
		Branch leaning = tamed == null
				? Branch.NONE : EvolutionRules.leaning(tamed.combat(), tamed.exploration());

		if (tamed != null) {
			graphics.text(this.font, Component.translatable(
					"screen.mythweave.journal.affinity", tamed.combat(), tamed.exploration()), x, y, TEXT_DIM);
			y += this.font.lineHeight;
			graphics.text(this.font, Component.translatable(
					"screen.mythweave.journal.evolution.progress", kindle, needed), x, y, TEXT);
			y += this.font.lineHeight;
			y = this.drawBar(graphics, x, y, width, kindle / (float) needed, ACCENT);
			y += 2;
		}

		String next = EvolutionRules.nextForm(line, entry.formId(), kindle, leaning);
		if (next != null) {
			graphics.text(this.font, Component.translatable(
					"screen.mythweave.journal.evolution.ready", EvolutionLine.formName(next)), x, y, GOOD);
			return y + this.font.lineHeight;
		}

		if (stage == 1) {
			graphics.text(this.font, Component.translatable("screen.mythweave.journal.evolution.next",
					EvolutionLine.formName(line.stage2()), EvolutionRules.STAGE_2_KINDLE), x, y, TEXT_DIM);
			return y + this.font.lineHeight;
		}

		if (tamed != null && leaning != Branch.NONE) {
			String target = line.formFor(leaning);
			graphics.text(this.font, Component.translatable("screen.mythweave.journal.evolution.next",
					EvolutionLine.formName(target), EvolutionRules.STAGE_3_KINDLE), x, y, TEXT_DIM);
			y += this.font.lineHeight;
			graphics.text(this.font, Component.translatable(
							leaning == Branch.COMBAT
									? "screen.mythweave.journal.leaning.combat"
									: "screen.mythweave.journal.leaning.exploration",
							EvolutionLine.formName(target)),
					x, y, TEXT_DIM);
			return y + this.font.lineHeight;
		}

		graphics.text(this.font, Component.translatable("screen.mythweave.journal.evolution.branches",
				EvolutionLine.formName(line.combatForm()),
				EvolutionLine.formName(line.explorationForm()),
				EvolutionRules.STAGE_3_KINDLE), x, y, TEXT_DIM);
		y += this.font.lineHeight;
		if (tamed != null) {
			graphics.text(this.font, Component.translatable("screen.mythweave.journal.leaning.none"),
					x, y, TEXT_DIM);
			y += this.font.lineHeight;
		}
		return y;
	}

	// ------------------------------------------------------------------
	// Data and entry helpers
	// ------------------------------------------------------------------

	private JournalData data() {
		if (this.minecraft == null || this.minecraft.player == null) {
			return JournalData.empty();
		}
		return this.minecraft.player.getAttachedOrCreate(JournalAttachments.JOURNAL);
	}

	private List<Entry> entries(JournalData data) {
		if (this.tab == Tab.TAMED) {
			List<Entry> entries = new ArrayList<>(data.tamed().size());
			for (TamedMythling mythling : data.tamed()) {
				Entry entry = Entry.tamed(mythling);
				if (this.matches(entry, data)) {
					entries.add(entry);
				}
			}
			return entries;
		}
		List<Entry> entries = new ArrayList<>(MythlingCatalog.profiles().size());
		for (MythlingProfile profile : MythlingCatalog.profiles()) {
			Entry entry = Entry.catalog(profile.formId(), data.isDiscovered(profile.formId()));
			if (this.matches(entry, data)) {
				entries.add(entry);
			}
		}
		return entries;
	}

	private boolean matches(Entry entry, JournalData data) {
		// A search or filter must never reveal that an undiscovered form exists.
		if (this.hasSearchOrFilter() && !entry.discovered()) {
			return false;
		}
		if (!this.matchesQuery(entry)) {
			return false;
		}
		MythlingProfile profile = MythlingCatalog.profile(entry.formId()).orElse(null);
		if (profile == null) {
			return this.rarityFilter == null && this.elementFilter == null;
		}
		if (this.rarityFilter != null
				&& !this.rarityFilter.equals(Component.translatable(profile.rarityKey()).getString())) {
			return false;
		}
		return this.elementFilter == null || profile.elementKeys().contains(this.elementFilter);
	}

	private boolean matchesQuery(Entry entry) {
		String query = this.searchQuery.trim().toLowerCase(Locale.ROOT);
		if (query.isEmpty()) {
			return true;
		}
		String formName = EvolutionLine.formName(entry.formId()).getString().toLowerCase(Locale.ROOT);
		if (formName.contains(query)) {
			return true;
		}
		return entry.tamed() != null && !entry.tamed().name().isEmpty()
				&& entry.tamed().name().toLowerCase(Locale.ROOT).contains(query);
	}

	private boolean hasSearchOrFilter() {
		return !this.searchQuery.isBlank() || this.rarityFilter != null || this.elementFilter != null;
	}

	/** Rarity labels offered as filters, in catalog order, from forms the player knows. */
	private List<String> rarityOptions(JournalData data) {
		Set<String> options = new LinkedHashSet<>();
		for (MythlingProfile profile : MythlingCatalog.profiles()) {
			if (this.isKnown(data, profile.formId())) {
				options.add(Component.translatable(profile.rarityKey()).getString());
			}
		}
		return List.copyOf(options);
	}

	/** Element keys offered as filters, in catalog order, from forms the player knows. */
	private List<String> elementOptions(JournalData data) {
		Set<String> options = new LinkedHashSet<>();
		for (MythlingProfile profile : MythlingCatalog.profiles()) {
			if (this.isKnown(data, profile.formId())) {
				options.addAll(profile.elementKeys());
			}
		}
		return List.copyOf(options);
	}

	private boolean isKnown(JournalData data, String formId) {
		if (data.isDiscovered(formId)) {
			return true;
		}
		for (TamedMythling mythling : data.tamed()) {
			if (mythling.formId().equals(formId)) {
				return true;
			}
		}
		return false;
	}

	private String nextFilter(List<String> options, String current) {
		if (options.isEmpty()) {
			return null;
		}
		if (current == null) {
			return options.get(0);
		}
		int index = options.indexOf(current);
		if (index < 0 || index + 1 >= options.size()) {
			return null;
		}
		return options.get(index + 1);
	}

	/** Drops a filter whose value is no longer offered, e.g. after the tab or data changes. */
	private void sanitizeFilters(JournalData data) {
		if (this.rarityFilter != null && !this.rarityOptions(data).contains(this.rarityFilter)) {
			this.rarityFilter = null;
		}
		if (this.elementFilter != null && !this.elementOptions(data).contains(this.elementFilter)) {
			this.elementFilter = null;
		}
	}

	private Entry selectedEntry(JournalData data) {
		List<Entry> entries = this.entries(data);
		if (entries.isEmpty()) {
			return null;
		}
		if (this.tab == Tab.TAMED) {
			for (Entry entry : entries) {
				if (entry.tamed() != null && entry.tamed().id().equals(this.selectedTamedId)) {
					return entry;
				}
			}
			this.selectedTamedId = entries.get(0).tamed().id();
			this.detailScroll = 0;
			return entries.get(0);
		}
		for (Entry entry : entries) {
			if (entry.formId().equals(this.selectedFormId)) {
				return entry;
			}
		}
		this.selectedFormId = entries.get(0).formId();
		this.detailScroll = 0;
		return entries.get(0);
	}

	private boolean isSelected(Entry entry) {
		if (this.tab == Tab.TAMED) {
			return entry.tamed() != null && entry.tamed().id().equals(this.selectedTamedId);
		}
		return entry.formId().equals(this.selectedFormId);
	}

	private int pageCount(int size) {
		return Math.max(1, (size + this.cardsPerPage - 1) / this.cardsPerPage);
	}

	private int discoveredCount(JournalData data) {
		int discovered = 0;
		for (MythlingProfile profile : MythlingCatalog.profiles()) {
			if (data.isDiscovered(profile.formId())) {
				discovered++;
			}
		}
		return discovered;
	}

	private LivingEntity previewEntity(String formId) {
		if (this.previews.containsKey(formId)) {
			return this.previews.get(formId);
		}
		LivingEntity preview = null;
		if (this.minecraft != null && this.minecraft.level != null) {
			Optional<EntityType<? extends MythlingEntity>> type = MythlingCatalog.entityType(formId);
			if (type.isPresent()) {
				MythlingEntity created = type.get().create(this.minecraft.level, EntitySpawnReason.MOB_SUMMONED);
				if (created != null) {
					// A detached preview is never added to a level, so nothing assigns it an
					// entity ID. Entity#getId() throws while the ID is still 0, and both the
					// renderer and vanilla's item-model resolver read it. Vanilla gives
					// display-only entities -1 for the same reason.
					created.setId(-1);
					created.setNoGravity(true);
					created.setNoAi(true);
					created.setSilent(true);
					created.setPermanentlyInvulnerable(true);
					if (this.minecraft.player != null) {
						// Render at the player's position so the preview picks up sensible light.
						created.setPos(this.minecraft.player.getX(), this.minecraft.player.getY(), this.minecraft.player.getZ());
					}
					created.setYRot(0.0F);
					created.setYBodyRot(0.0F);
					created.setYHeadRot(0.0F);
					created.setXRot(0.0F);
					created.tickCount = 20;
					preview = created;
				}
			}
		}
		this.previews.put(formId, preview);
		return preview;
	}

	private Component status(TamedMythling mythling) {
		if (mythling.evolving()) {
			return Component.translatable("screen.mythweave.journal.status.evolving");
		}
		return Component.translatable(mythling.sitting()
				? "screen.mythweave.journal.status.sitting"
				: "screen.mythweave.journal.status.following");
	}

	private Component elements(MythlingProfile profile) {
		List<String> keys = profile.elementKeys();
		if (keys.isEmpty()) {
			return Component.empty();
		}
		if (keys.size() == 1) {
			return Component.translatable(keys.get(0));
		}
		MutableComponent result = Component.empty();
		for (int i = 0; i < keys.size(); i++) {
			if (i > 0) {
				result.append(Component.translatable(i == keys.size() - 1
						? "screen.mythweave.journal.element.and"
						: "screen.mythweave.journal.element.comma"));
			}
			result.append(Component.translatable(keys.get(i)));
		}
		return result;
	}

	private Component traits(MythlingProfile profile) {
		List<Component> names = new ArrayList<>(profile.abilityKeys().size());
		for (String key : profile.abilityKeys()) {
			names.add(Component.translatable(key));
		}
		return Component.translatable("screen.mythweave.journal.traits", join(names));
	}

	private Component join(List<Component> parts) {
		MutableComponent result = Component.empty();
		for (int i = 0; i < parts.size(); i++) {
			if (i > 0) {
				result.append(Component.literal(", "));
			}
			result.append(parts.get(i));
		}
		return result;
	}

	private String stat(float value) {
		if (Math.abs(value - Math.round(value)) < 0.05F) {
			return Integer.toString(Math.round(value));
		}
		return String.format(Locale.ROOT, "%.1f", value);
	}

	private String ellipsize(String text, int maxWidth) {
		if (this.font.width(text) <= maxWidth) {
			return text;
		}
		String ellipsis = "...";
		int width = maxWidth - this.font.width(ellipsis);
		return this.font.plainSubstrByWidth(text, Math.max(0, width)) + ellipsis;
	}

	// ------------------------------------------------------------------
	// Text helpers
	// ------------------------------------------------------------------

	private int drawWrapped(GuiGraphicsExtractor graphics, Component text,
			int x, int y, int width, int colour, int maxLines) {
		List<FormattedCharSequence> lines = this.font.split(text, width);
		int count = Math.min(lines.size(), maxLines);
		for (int i = 0; i < count; i++) {
			graphics.text(this.font, lines.get(i), x, y, colour);
			y += this.font.lineHeight;
		}
		return y;
	}

	private void drawCenteredWrapped(GuiGraphicsExtractor graphics, Component text,
			int centerX, int y, int width, int colour) {
		for (FormattedCharSequence line : this.font.split(text, width)) {
			graphics.centeredText(this.font, line, centerX, y, colour);
			y += this.font.lineHeight;
		}
	}

	private int drawBar(GuiGraphicsExtractor graphics, int x, int y, int width, float ratio, int colour) {
		graphics.fill(x, y, x + width, y + 5, BAR_BG);
		graphics.outline(x, y, width, 5, BAR_BORDER);
		int filled = (int) ((width - 2) * Mth.clamp(ratio, 0.0F, 1.0F));
		if (filled > 0) {
			graphics.fill(x + 1, y + 1, x + 1 + filled, y + 4, colour);
		}
		return y + 5;
	}

	// ------------------------------------------------------------------
	// Input
	// ------------------------------------------------------------------

	@Override
	public boolean keyPressed(KeyEvent event) {
		// The screen owns the toggle: vanilla does not deliver keybind clicks while a
		// screen is open. If the player is typing in the search field, let the text win.
		if ((this.searchBox == null || !this.searchBox.isFocused())
				&& MythweaveClient.isJournalKey(event)) {
			this.onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
			double mouseX = event.x();
			double mouseY = event.y();
			if (this.overClose(mouseX, mouseY)) {
				this.onClose();
				return true;
			}
			if (this.isInside(mouseX, mouseY, this.tabTamedX, this.tabY, this.tabTamedWidth, this.tabHeight)) {
				this.switchTab(Tab.TAMED);
				return true;
			}
			if (this.isInside(mouseX, mouseY, this.tabMythlingsX, this.tabY, this.tabMythlingsWidth, this.tabHeight)) {
				this.switchTab(Tab.MYTHLINGS);
				return true;
			}

			JournalData data = this.data();
			this.sanitizeFilters(data);
			if (this.isInside(mouseX, mouseY, this.listX, this.rarityFilterY, LIST_WIDTH, FILTER_HEIGHT)) {
				this.rarityFilter = this.nextFilter(this.rarityOptions(data), this.rarityFilter);
				this.page = 0;
				return true;
			}
			if (this.isInside(mouseX, mouseY, this.listX, this.elementFilterY, LIST_WIDTH, FILTER_HEIGHT)) {
				this.elementFilter = this.nextFilter(this.elementOptions(data), this.elementFilter);
				this.page = 0;
				return true;
			}
			List<Entry> entries = this.entries(data);
			int pages = this.pageCount(entries.size());
			if (this.isInside(mouseX, mouseY, this.listX, this.pageY, PAGE_BUTTON, PAGE_BUTTON) && this.page > 0) {
				this.page--;
				return true;
			}
			if (this.isInside(mouseX, mouseY, this.listX + LIST_WIDTH - PAGE_BUTTON, this.pageY,
					PAGE_BUTTON, PAGE_BUTTON) && this.page < pages - 1) {
				this.page++;
				return true;
			}

			int index = this.cardIndexAt(mouseX, mouseY, entries.size());
			if (index >= 0) {
				Entry entry = entries.get(index);
				if (entry.tamed() != null) {
					if (!entry.tamed().id().equals(this.selectedTamedId)) {
						this.detailScroll = 0;
					}
					this.selectedTamedId = entry.tamed().id();
				} else {
					if (!entry.formId().equals(this.selectedFormId)) {
						this.detailScroll = 0;
					}
					this.selectedFormId = entry.formId();
				}
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (this.isInside(mouseX, mouseY, this.listX, this.listY, LIST_WIDTH, this.contentBottom - this.listY)) {
			int pages = this.pageCount(this.entries(this.data()).size());
			if (scrollY < 0 && this.page < pages - 1) {
				this.page++;
				return true;
			}
			if (scrollY > 0 && this.page > 0) {
				this.page--;
				return true;
			}
		}
		if (this.isInside(mouseX, mouseY, this.detailX, this.detailY, this.detailWidth, this.detailHeight)) {
			int maxScroll = this.maxDetailScroll();
			if (maxScroll > 0) {
				int next = Mth.clamp(this.detailScroll + (int) Math.round(-scrollY * 12), 0, maxScroll);
				if (next != this.detailScroll) {
					this.detailScroll = next;
					return true;
				}
			}
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	private void switchTab(Tab newTab) {
		if (this.tab != newTab) {
			this.tab = newTab;
			this.page = 0;
			this.selectedTamedId = null;
			this.selectedFormId = null;
			this.detailScroll = 0;
			this.detailContentHeight = 0;
		}
	}

	private int cardIndexAt(double mouseX, double mouseY, int size) {
		if (mouseX < this.listX || mouseX >= this.listX + LIST_WIDTH) {
			return -1;
		}
		int start = this.page * this.cardsPerPage;
		int end = Math.min(size, start + this.cardsPerPage);
		for (int i = start; i < end; i++) {
			int cardY = this.listY + (i - start) * (CARD_HEIGHT + CARD_GAP);
			if (mouseY >= cardY && mouseY < cardY + CARD_HEIGHT) {
				return i;
			}
		}
		return -1;
	}

	private boolean overClose(double mouseX, double mouseY) {
		return this.isInside(mouseX, mouseY, this.closeX - 2, this.closeY - 2,
				this.closeSize + 4, this.closeSize + 4);
	}

	private boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	// ------------------------------------------------------------------

	private enum Tab {
		TAMED,
		MYTHLINGS
	}

	private record Entry(String formId, TamedMythling tamed, boolean discovered) {
		static Entry tamed(TamedMythling mythling) {
			return new Entry(mythling.formId(), mythling, true);
		}

		static Entry catalog(String formId, boolean discovered) {
			return new Entry(formId, null, discovered);
		}

		String displayName() {
			if (this.tamed != null && !this.tamed.name().isEmpty()) {
				return this.tamed.name();
			}
			return EvolutionLine.formName(this.formId).getString();
		}
	}
}
