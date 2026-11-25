package cc.silk.gui.virus;

import cc.silk.SilkClient;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.module.setting.BooleanSetting;
import cc.silk.module.setting.KeybindSetting;
import cc.silk.module.setting.ModeSetting;
import cc.silk.module.setting.NumberSetting;
import cc.silk.module.setting.Setting;
import cc.silk.module.setting.StringSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * "Virus" click GUI: compact translucent window, category rail, search with
 * quick filters, three-column card grid, inline settings, keybind capture,
 * per-module descriptions and RU/EN interface language.
 */
public final class VirusGui extends Screen {

    private final MinecraftClient mc = MinecraftClient.getInstance();

    private static final Color WINDOW = new Color(18, 18, 21, 235);
    private static final Color SIDEBAR = new Color(12, 12, 15, 235);
    private static final Color FIELD = new Color(30, 30, 36, 255);
    private static final Color CARD = new Color(26, 26, 31, 220);
    private static final Color CARD_HOVER = new Color(34, 34, 41, 235);
    private static final Color BORDER = new Color(52, 52, 60, 255);
    private static final Color TEXT = new Color(228, 228, 234, 255);
    private static final Color MUTED = new Color(132, 132, 144, 255);
    private static final Color ACCENT = new Color(96, 152, 255, 255);
    private static final Color TRACK = new Color(66, 66, 74, 255);
    private static final Color WHITE = new Color(255, 255, 255, 255);
    private static final Color RED = new Color(236, 92, 92, 255);

    private static final int RAIL_W = 120;
    private static final int HEADER_H = 38;
    private static final int CARD_H = 24;
    private static final int CARD_GAP = 6;
    private static final int SETTING_H = 17;

    private static final Set<String> FAVOURITES = new HashSet<>();
    private static final Map<String, Boolean> EXPANDED = new HashMap<>();

    private final List<NavItem> nav = new ArrayList<>();
    private final List<CardRect> cards = new ArrayList<>();
    private String selectedId = "movement";
    private String search = "";
    private boolean searchFocused;
    private float scroll;
    private int maxScroll;
    private Module listeningModule;
    private NumberSetting draggedSlider;
    private int sliderX, sliderW;
    private Module hoveredModule;
    private int hoveredX, hoveredY;

    private int winX, winY, winW, winH;
    private int contentX, contentY, contentW, contentH;
    private int searchX, searchY, searchW, searchH;
    private int iconFriendsX, iconTargetX, iconStarX;
    private int langX;

    public VirusGui() {
        super(Text.literal("Virus"));
        nav.add(new NavItem("all", null));
        nav.add(new NavItem("combat", Category.COMBAT));
        nav.add(new NavItem("movement", Category.MOVEMENT));
        nav.add(new NavItem("player", Category.PLAYER));
        nav.add(new NavItem("renderer", Category.RENDER));
        nav.add(new NavItem("visual", Category.VISUAL));
        nav.add(new NavItem("particles", Category.PARTICLES));
        nav.add(new NavItem("client", Category.CLIENT));

        EXPANDED.putIfAbsent("Kill Aura", true);
        EXPANDED.putIfAbsent("Aim Assist", true);
        EXPANDED.putIfAbsent("Trigger Bot", true);
    }

    private void layout() {
        winW = Math.min(width - 60, 640);
        winH = Math.min(height - 60, 380);
        winX = Math.max(20, (width - winW) / 2);
        winY = Math.max(20, (height - winH) / 2);

        searchX = winX + RAIL_W + 8;
        searchY = winY + 7;
        searchW = winW - RAIL_W - 16;
        searchH = 24;
        iconStarX = winX + winW - 24;
        iconTargetX = iconStarX - 24;
        iconFriendsX = iconTargetX - 24;

        contentX = winX + RAIL_W + 8;
        contentY = winY + HEADER_H + 6;
        contentW = winW - RAIL_W - 16;
        contentH = winH - HEADER_H - 12;
        langX = winX + 12;
    }

    private List<Module> modulesFor(String id) {
        List<Module> result = new ArrayList<>();
        List<Module> all = SilkClient.INSTANCE.getModuleManager().getModules();
        if ("all".equals(id)) return new ArrayList<>(all);
        if ("favorites".equals(id)) {
            for (Module module : all) {
                if (FAVOURITES.contains(module.getName())) result.add(module);
            }
            return result;
        }
        if ("friends".equals(id)) {
            for (Module module : all) {
                if (module.getModuleCategory() == Category.MISC) result.add(module);
            }
            return result;
        }
        for (NavItem item : nav) {
            if (id.equals(item.id) && item.category != null) {
                for (Module module : all) {
                    if (module.getModuleCategory() == item.category) result.add(module);
                }
                break;
            }
        }
        return result;
    }

    private List<Module> filteredModules() {
        List<Module> modules = modulesFor(selectedId);
        if (!search.isEmpty()) {
            String query = search.toLowerCase(Locale.ROOT);
            modules.removeIf(m -> !m.getName().toLowerCase(Locale.ROOT).contains(query)
                    && !Lang.module(m.getName()).toLowerCase(Locale.ROOT).contains(query));
        }
        return modules;
    }

    private void computeCards() {
        cards.clear();
        List<Module> modules = filteredModules();
        int cols = 3;
        int colW = (contentW - CARD_GAP * (cols - 1)) / cols;
        int[] columnY = new int[cols];
        for (int i = 0; i < modules.size(); i++) {
            Module module = modules.get(i);
            int col = i % cols;
            boolean expanded = EXPANDED.getOrDefault(module.getName(), false);
            int x = contentX + col * (colW + CARD_GAP);
            int y = contentY + columnY[col] - (int) scroll;
            int h = cardHeight(module, expanded);
            cards.add(new CardRect(module, x, y, colW, h, expanded));
            columnY[col] += h + CARD_GAP;
        }
        int tallest = 0;
        for (int y : columnY) tallest = Math.max(tallest, y);
        maxScroll = Math.max(0, tallest - contentH);
        scroll = Math.max(0, Math.min(maxScroll, scroll));
    }

    private int cardHeight(Module module, boolean expanded) {
        if (!expanded) return CARD_H;
        return CARD_H + 4 + module.getSettings().size() * SETTING_H;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        layout();
        computeCards();

        context.fill(0, 0, width, height, 0x8C08080B);
        round(context, winX, winY, winW, winH, 8, WINDOW);
        round(context, winX, winY, RAIL_W, winH, 8, SIDEBAR);
        context.fill(winX + RAIL_W - 6, winY, winX + RAIL_W, winY + winH, SIDEBAR.getRGB());

        drawRail(context);
        drawTopBar(context);

        hoveredModule = null;
        for (CardRect card : cards) {
            if (card.y + card.h < contentY || card.y > contentY + contentH) continue;
            drawCard(context, card, mouseX, mouseY);
        }
        if (hoveredModule != null) {
            drawTooltip(context, hoveredModule, hoveredX, hoveredY);
        }
    }

    private void drawRail(DrawContext context) {
        text(context, "Virus", winX + 14, winY + 10, TEXT, true);
        int y = winY + 42;
        for (NavItem item : nav) {
            boolean active = selectedId.equals(item.id);
            if (active) {
                round(context, winX + 8, y - 3, RAIL_W - 16, 20, 4, new Color(42, 42, 50, 255));
                context.fill(winX + 8, y - 3, 2, 20, ACCENT.getRGB());
            }
            text(context, Lang.ui(item.id), winX + 16, y, active ? TEXT : MUTED, active);
            y += 24;
        }
        round(context, langX, winY + winH - 28, 30, 18, 4, new Color(40, 40, 48, 255));
        text(context, Lang.russian() ? "RU" : "EN", langX + 8, winY + winH - 26, ACCENT, true);
    }

    private void drawTopBar(DrawContext context) {
        round(context, searchX, searchY, searchW, searchH, 5, FIELD);
        if (searchFocused) context.fill(searchX, searchY, searchX + searchW, searchY + 1, ACCENT.getRGB());
        String shown = search.isEmpty() ? Lang.ui("hint") : search;
        text(context, shown, searchX + 8, searchY + 7, search.isEmpty() ? MUTED : TEXT, false);
        drawSearchIcon(context, searchX + searchW - 62, searchY + 8);
        drawPeopleIcon(context, iconFriendsX, searchY + 12);
        drawCrosshairIcon(context, iconTargetX, searchY + 12);
        drawStarIcon(context, iconStarX, searchY + 12, "favorites".equals(selectedId));
        context.fill(searchX, searchY + searchH + 2, winX + winW - 8, searchY + searchH + 3, BORDER.getRGB());
    }

    private void drawCard(DrawContext context, CardRect card, int mouseX, int mouseY) {
        Module module = card.module;
        boolean headerHover = hover(mouseX, mouseY, card.x, card.y, card.w, CARD_H);
        round(context, card.x, card.y, card.w, card.h, 5,
                headerHover ? CARD_HOVER : CARD);
        if (module.isEnabled()) context.fill(card.x, card.y + 4, card.x + 1, card.y + CARD_H - 4, ACCENT.getRGB());

        String name = Lang.module(module.getName());
        while (mc.textRenderer.getWidth(name) > card.w - 32 && name.length() > 4) {
            name = name.substring(0, name.length() - 2);
        }
        text(context, name, card.x + 6, card.y + 6, module.isEnabled() ? TEXT : MUTED, true);
        drawCheck(context, card.x + card.w - 34, card.y + 12, module.isEnabled());
        drawStarIcon(context, card.x + card.w - 16, card.y + 12, FAVOURITES.contains(module.getName()));

        if (headerHover) {
            hoveredModule = module;
            hoveredX = mouseX;
            hoveredY = mouseY;
        }
        if (!card.expanded) return;

        int sy = card.y + CARD_H + 2;
        for (Setting setting : module.getSettings()) {
            drawSetting(context, setting, card.x + 6, sy, card.w - 12);
            sy += SETTING_H;
        }
    }

    private void drawSetting(DrawContext context, Setting setting, int x, int y, int w) {
        String label = Lang.setting(setting.getName());
        if (setting instanceof BooleanSetting boolSetting) {
            text(context, label, x, y + 2, MUTED, false);
            drawCheckbox(context, x + w - 13, y + 1, boolSetting.getValue());
        } else if (setting instanceof NumberSetting number) {
            text(context, label, x, y, MUTED, false);
            String value = format(number.getValue());
            text(context, value, x + w - (int) mc.textRenderer.getWidth(value), y, TEXT, false);
            double norm = (number.getValue() - number.getMin()) / (number.getMax() - number.getMin());
            int px = x + (int) (w * norm);
            context.fill(x, y + 11, x + w, y + 13, TRACK.getRGB());
            context.fill(x, y + 11, px, y + 13, ACCENT.getRGB());
            circle(context, px, y + 12, 2, WHITE);
        } else if (setting instanceof ModeSetting mode) {
            text(context, label, x, y + 2, MUTED, false);
            String value = "< " + Lang.setting(mode.getMode()) + " >";
            text(context, value, x + w - (int) mc.textRenderer.getWidth(value), y + 2, ACCENT, false);
        } else if (setting instanceof KeybindSetting keybind) {
            text(context, label, x, y + 2, MUTED, false);
            String value = keybind.isListening() ? "..." : keyName(keybind.getKeyCode());
            text(context, value, x + w - (int) mc.textRenderer.getWidth(value), y + 2, ACCENT, false);
        } else if (setting instanceof StringSetting stringSetting) {
            text(context, label + ": " + stringSetting.getValue(), x, y + 2, MUTED, false);
        } else {
            text(context, label, x, y + 2, MUTED, false);
        }
    }

    private void drawTooltip(DrawContext context, Module module, int mouseX, int mouseY) {
        String description = Lang.setting(module.getDescription());
        if (description == null || description.isEmpty()) return;
        int tw = Math.min((int) mc.textRenderer.getWidth(description) + 12, 220);
        int tx = Math.min(mouseX + 8, width - tw - 10);
        int ty = Math.min(mouseY + 8, height - 22);
        round(context, tx, ty, tw, 18, 4, new Color(16, 16, 20, 240));
        context.drawBorder(tx, ty, tw, 18, BORDER.getRGB());
        String value = description;
        while (mc.textRenderer.getWidth(value) > tw - 10 && value.length() > 4) {
            value = value.substring(0, value.length() - 2);
        }
        text(context, value, tx + 5, ty + 5, TEXT, false);
    }

    private boolean areAllEnabled() {
        List<Module> modules = filteredModules();
        if (modules.isEmpty()) return false;
        for (Module module : modules) {
            if (!module.isEnabled()) return false;
        }
        return true;
    }

    private static String format(double value) {
        return value == Math.floor(value) ? Integer.toString((int) value) : String.format("%.1f", value);
    }

    private static String keyName(int key) {
        if (key <= 0) return "...";
        String name = GLFW.glfwGetKeyName(key, 0);
        return name == null || name.isEmpty() ? "KEY" : name.toUpperCase(Locale.ROOT);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        layout();
        computeCards();
        int mx = (int) mouseX;
        int my = (int) mouseY;

        if (hover(mx, my, winX + winW - 30, winY + 8, 24, 24)) {
            close();
            return true;
        }
        if (hover(mx, my, langX, winY + winH - 28, 30, 18)) {
            Lang.toggle();
            return true;
        }
        if (hover(mx, my, iconStarX - 12, searchY, 24, searchH)) {
            selectedId = "favorites";
            scroll = 0;
            return true;
        }
        if (hover(mx, my, iconTargetX - 12, searchY, 24, searchH)) {
            selectedId = "combat";
            scroll = 0;
            return true;
        }
        if (hover(mx, my, iconFriendsX - 12, searchY, 24, searchH)) {
            selectedId = "friends";
            scroll = 0;
            return true;
        }
        searchFocused = hover(mx, my, searchX, searchY, searchW, searchH);

        int navY = winY + 42;
        for (NavItem item : nav) {
            if (hover(mx, my, winX + 8, navY - 3, RAIL_W - 16, 20)) {
                selectedId = item.id;
                scroll = 0;
                return true;
            }
            navY += 24;
        }

        if (mx < contentX || mx > contentX + contentW || my < contentY || my > contentY + contentH) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        for (CardRect card : cards) {
            if (mx < card.x || mx > card.x + card.w || my < card.y || my > card.y + card.h) continue;

            if (hover(mx, my, card.x + card.w - 22, card.y + 5, 18, 16)) {
                String id = card.module.getName();
                if (!FAVOURITES.add(id)) FAVOURITES.remove(id);
                return true;
            }
            if (hover(mx, my, card.x + card.w - 40, card.y + 5, 16, 16)) {
                card.module.toggle();
                return true;
            }
            if (my <= card.y + CARD_H) {
                EXPANDED.put(card.module.getName(), !card.expanded);
                return true;
            }

            int sy = card.y + CARD_H + 2;
            int sx = card.x + 6;
            int sw = card.w - 12;
            for (Setting setting : card.module.getSettings()) {
                if (setting instanceof BooleanSetting boolSetting) {
                    if (hover(mx, my, sx + sw - 15, sy, 15, 14)) {
                        boolSetting.toggle();
                        return true;
                    }
                } else if (setting instanceof NumberSetting number) {
                    if (hover(mx, my, sx, sy + 8, sw, 8)) {
                        draggedSlider = number;
                        sliderX = sx;
                        sliderW = sw;
                        applySlider(mx);
                        return true;
                    }
                } else if (setting instanceof ModeSetting mode) {
                    if (hover(mx, my, sx, sy, sw, 14)) {
                        mode.cycle();
                        return true;
                    }
                } else if (setting instanceof KeybindSetting keybind) {
                    if (hover(mx, my, sx, sy, sw, 14)) {
                        for (Module other : SilkClient.INSTANCE.getModuleManager().getModules()) {
                            for (Setting s : other.getSettings()) {
                                if (s instanceof KeybindSetting k) k.setListening(false);
                            }
                        }
                        keybind.setListening(true);
                        listeningModule = card.module;
                        return true;
                    }
                }
                sy += SETTING_H;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggedSlider != null) {
            applySlider((int) mouseX);
            return true;
        }
        scroll -= deltaY * 0.6f;
        if (scroll < 0) scroll = 0;
        if (maxScroll > 0) scroll = Math.min(maxScroll, scroll);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggedSlider = null;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void applySlider(int mouseX) {
        if (draggedSlider == null || sliderW <= 0) return;
        double norm = (mouseX - sliderX) / (double) sliderW;
        draggedSlider.setValue(draggedSlider.getMin() + norm * (draggedSlider.getMax() - draggedSlider.getMin()));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll -= verticalAmount * 22f;
        if (scroll < 0) scroll = 0;
        if (maxScroll > 0) scroll = Math.min(maxScroll, scroll);
        return true;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searchFocused) {
            if (chr == '\b') {
                if (!search.isEmpty()) search = search.substring(0, search.length() - 1);
            } else if (chr >= ' ') {
                search += chr;
            }
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (listeningModule != null) {
            int value = keyCode == GLFW.GLFW_KEY_ESCAPE ? -1 : keyCode;
            for (Setting setting : listeningModule.getSettings()) {
                if (setting instanceof KeybindSetting keybind) {
                    keybind.setKeyCode(value);
                    keybind.setListening(false);
                }
            }
            listeningModule = null;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        if (searchFocused && keyCode == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
            search = search.substring(0, search.length() - 1);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        searchFocused = false;
        draggedSlider = null;
        listeningModule = null;
        if (mc != null) mc.setScreen(null);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private static boolean hover(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static void round(DrawContext c, int x, int y, int w, int h, int r, Color color) {
        cc.silk.gui.components.UIRenderer.renderRoundedRect(c, x, y, w, h, r, color);
    }

    private static void circle(DrawContext c, int cx, int cy, int r, Color color) {
        for (int dy = -r; dy <= r; dy++) {
            int dx = (int) Math.sqrt(r * r - dy * dy);
            c.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, color.getRGB());
        }
    }

    private static void drawCheck(DrawContext c, int x, int y, boolean enabled) {
        c.drawBorder(x, y, 9, 9, enabled ? ACCENT.getRGB() : BORDER.getRGB());
        if (enabled) {
            c.fill(x + 2, y + 5, x + 4, y + 7, ACCENT.getRGB());
            c.fill(x + 4, y + 3, x + 8, y + 5, ACCENT.getRGB());
        }
    }

    private static void drawStarIcon(DrawContext c, int x, int y, boolean active) {
        Color color = active ? new Color(255, 205, 90, 255) : MUTED;
        c.fill(x, y, x + 3, y + 7, color.getRGB());
        c.fill(x + 7, y, x + 10, y + 7, color.getRGB());
        for (int i = 0; i < 5; i++) c.fill(x + i, y + i + 1, x + 10 - i, y + i + 2, color.getRGB());
    }

    private static void drawPeopleIcon(DrawContext c, int x, int y) {
        circle(c, x + 3, y + 2, 2, MUTED);
        circle(c, x + 9, y + 2, 2, MUTED);
        c.fill(x, y + 5, x + 5, y + 10, MUTED.getRGB());
        c.fill(x + 7, y + 5, x + 12, y + 10, MUTED.getRGB());
    }

    private static void drawCrosshairIcon(DrawContext c, int x, int y) {
        c.fill(x + 5, y, x + 7, y + 5, MUTED.getRGB());
        c.fill(x + 5, y + 7, x + 7, y + 12, MUTED.getRGB());
        c.fill(x, y + 5, x + 5, y + 7, MUTED.getRGB());
        c.fill(x + 7, y + 5, x + 12, y + 7, MUTED.getRGB());
    }

    private static void drawSearchIcon(DrawContext c, int x, int y) {
        circle(c, x, y, 3, MUTED);
        c.fill(x + 3, y + 3, x + 4, y + 8, MUTED.getRGB());
    }

    private static void drawToggle(DrawContext c, int x, int y, boolean enabled) {
        round(c, x, y, 26, 14, 7, enabled ? ACCENT : TRACK);
        circle(c, enabled ? x + 19 : x + 7, y + 7, 5, WHITE);
    }

    private static void drawCheckbox(DrawContext c, int x, int y, boolean enabled) {
        if (enabled) {
            round(c, x, y, 12, 12, 3, ACCENT);
            c.fill(x + 2, y + 6, x + 5, y + 9, WHITE.getRGB());
            c.fill(x + 4, y + 4, x + 10, y + 6, WHITE.getRGB());
        } else {
            round(c, x, y, 12, 12, 3, FIELD);
            c.drawBorder(x, y, 12, 12, BORDER.getRGB());
        }
    }

    private void text(DrawContext c, String value, int x, int y, Color color, boolean bold) {
        if (bold) {
            c.drawTextWithShadow(mc.textRenderer, Text.literal(value), x, y, color.getRGB());
        } else {
            c.drawText(mc.textRenderer, Text.literal(value), x, y, color.getRGB(), false);
        }
    }

    private static final class NavItem {
        final String id;
        final Category category;

        NavItem(String id, Category category) {
            this.id = id;
            this.category = category;
        }
    }

    private static final class CardRect {
        final Module module;
        final int x, y, w, h;
        final boolean expanded;

        CardRect(Module module, int x, int y, int w, int h, boolean expanded) {
            this.module = module;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.expanded = expanded;
        }
    }
}
