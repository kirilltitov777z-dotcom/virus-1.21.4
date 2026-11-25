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
 * "Virus" click GUI: navigation sidebar, module cards with favourites,
 * inline settings, keybind capture and RU/EN interface language.
 */
public final class VirusGui extends Screen {

    private final MinecraftClient mc = MinecraftClient.getInstance();

    private static final Color WINDOW = new Color(20, 20, 23, 255);
    private static final Color SIDEBAR = new Color(14, 14, 16, 255);
    private static final Color CARD = new Color(28, 28, 33, 255);
    private static final Color CARD_HOVER = new Color(35, 35, 42, 255);
    private static final Color FIELD = new Color(24, 24, 29, 255);
    private static final Color BORDER = new Color(48, 48, 56, 255);
    private static final Color TEXT = new Color(232, 232, 238, 255);
    private static final Color MUTED = new Color(138, 138, 150, 255);
    private static final Color ACCENT = new Color(76, 141, 255, 255);
    private static final Color TRACK = new Color(62, 62, 70, 255);
    private static final Color WHITE = new Color(255, 255, 255, 255);
    private static final Color RED = new Color(236, 92, 92, 255);

    private static final int SIDEBAR_W = 168;
    private static final int HEADER_H = 42;
    private static final int CARD_H = 34;
    private static final int CARD_GAP = 8;
    private static final int SETTING_H = 20;

    private static final Set<String> FAVOURITES = new HashSet<>();
    private static final Map<String, Boolean> EXPANDED = new HashMap<>();

    private final List<NavItem> nav = new ArrayList<>();
    private final List<CardRect> cards = new ArrayList<>();
    private String selectedId = "combat";
    private String search = "";
    private boolean searchFocused;
    private boolean twoColumns = true;
    private float scroll;
    private int maxScroll;
    private Module listeningModule;
    private NumberSetting draggedSlider;
    private int sliderX, sliderW;

    private int winX, winY, winW, winH;
    private int contentX, contentY, contentW, contentH;
    private int searchX, searchW, langX, gridX, closeX, allX;

    public VirusGui() {
        super(Text.literal("Virus"));
        nav.add(new NavItem("combat", Category.COMBAT));
        nav.add(new NavItem("movement", Category.MOVEMENT));
        nav.add(new NavItem("player", Category.PLAYER));
        nav.add(new NavItem("visual", Category.RENDER));
        nav.add(new NavItem("utilities", Category.MISC));
        nav.add(new NavItem(null, null));
        nav.add(new NavItem("favorites", null));
        nav.add(new NavItem("friends", null));
        nav.add(new NavItem("configs", Category.CONFIG));
        nav.add(new NavItem("interface", Category.CLIENT));
        nav.add(new NavItem("macros", null));

        EXPANDED.putIfAbsent("Kill Aura", true);
        EXPANDED.putIfAbsent("Aim Assist", true);
    }

    private void layout() {
        winW = Math.min(width - 40, 860);
        winH = Math.min(height - 40, 580);
        winX = Math.max(20, (width - winW) / 2 - 70);
        winY = Math.max(20, (height - winH) / 2);
        if (winX + winW > width - 12) winX = Math.max(12, width - winW - 12);

        contentX = winX + SIDEBAR_W + 12;
        contentY = winY + HEADER_H + 10;
        contentW = winW - SIDEBAR_W - 24;
        contentH = winH - HEADER_H - 20;

        searchW = 150;
        searchX = winX + winW - searchW - 150;
        langX = winX + winW - 112;
        gridX = winX + winW - 76;
        closeX = winX + winW - 38;
        allX = winX + SIDEBAR_W + 230;
    }

    private List<Module> modulesFor(String id) {
        List<Module> result = new ArrayList<>();
        List<Module> all = SilkClient.INSTANCE.getModuleManager().getModules();
        if ("favorites".equals(id)) {
            for (Module module : all) {
                if (FAVOURITES.contains(module.getName())) result.add(module);
            }
            return result;
        }
        if ("friends".equals(id) || "macros".equals(id)) {
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

    /** Cards have variable height; each column stacks independently. */
    private void computeCards() {
        cards.clear();
        List<Module> modules = filteredModules();
        int cols = twoColumns ? 2 : 1;
        int colW = (contentW - CARD_GAP * (cols - 1)) / cols;
        int[] columnY = new int[cols];
        for (int i = 0; i < modules.size(); i++) {
            int col = twoColumns ? i % 2 : 0;
            Module module = modules.get(i);
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
        return CARD_H + 6 + module.getSettings().size() * SETTING_H;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        layout();
        computeCards();

        context.fill(0, 0, width, height, 0x9008080B);
        round(context, winX, winY, winW, winH, 10, WINDOW);
        round(context, winX, winY, SIDEBAR_W, winH, 10, SIDEBAR);
        context.fill(winX + SIDEBAR_W - 10, winY, winX + SIDEBAR_W, winY + winH, SIDEBAR.getRGB());

        drawSidebar(context);
        drawHeader(context);
        for (CardRect card : cards) {
            if (card.y + card.h < contentY || card.y > contentY + contentH) continue;
            drawCard(context, card, mouseX, mouseY);
        }
    }

    private void drawSidebar(DrawContext context) {
        text(context, "Virus", winX + 22, winY + 22, TEXT, true);

        int y = winY + 62;
        for (NavItem item : nav) {
            if (item.id == null) {
                text(context, Lang.ui("misc"), winX + 22, y + 2, MUTED, false);
                y += 26;
                continue;
            }
            boolean active = selectedId.equals(item.id);
            if (active) {
                round(context, winX + 10, y - 3, SIDEBAR_W - 20, 22, 5, new Color(38, 38, 45, 255));
                context.fill(winX + 10, y - 3, 3, 22, ACCENT.getRGB());
            }
            text(context, Lang.ui(item.id), winX + 24, y + 2, active ? TEXT : MUTED, active);
            y += 26;
        }

        int footY = winY + winH - 48;
        context.fill(winX + 12, footY - 8, winX + SIDEBAR_W - 12, footY - 7, BORDER.getRGB());
        circle(context, winX + 26, footY + 8, 9, new Color(56, 56, 66, 255));
        text(context, Lang.ui("admin"), winX + 42, footY + 2, TEXT, false);
        text(context, "1337", winX + 42, footY + 14, MUTED, false);
    }

    private void drawHeader(DrawContext context) {
        int x = winX + SIDEBAR_W;
        context.fill(x, winY + HEADER_H - 1, winX + winW, winY + HEADER_H, BORDER.getRGB());
        text(context, Lang.ui(selectedId), x + 16, winY + 15, TEXT, true);

        text(context, Lang.ui("enableAll"), allX - 92, winY + 16, MUTED, false);
        drawToggle(context, allX, winY + 14, areAllEnabled());

        round(context, searchX, winY + 10, searchW, 22, 5, FIELD);
        if (searchFocused) context.fill(searchX, winY + 10, searchX + searchW, winY + 11, ACCENT.getRGB());
        String shown = search.isEmpty() ? Lang.ui("search") : search;
        text(context, shown, searchX + 8, winY + 16, search.isEmpty() ? MUTED : TEXT, false);
        drawSearchIcon(context, searchX + searchW - 18, winY + 16);

        String lang = Lang.russian() ? "RU" : "EN";
        round(context, langX, winY + 11, 30, 20, 5, new Color(38, 38, 46, 255));
        text(context, lang, langX + 9, winY + 16, ACCENT, true);
        drawGridIcon(context, gridX + 4, winY + 16);
        drawCloseIcon(context, closeX + 4, winY + 16);
    }

    private void drawCard(DrawContext context, CardRect card, int mouseX, int mouseY) {
        Module module = card.module;
        boolean hovered = hover(mouseX, mouseY, card.x, card.y, card.w, CARD_H);
        round(context, card.x, card.y, card.w, card.h, 7, hovered ? CARD_HOVER : CARD);
        if (module.isEnabled()) {
            context.fill(card.x, card.y + 8, card.x + 2, card.y + CARD_H - 8, ACCENT.getRGB());
        }

        heart(context, card.x + 16, card.y + 16,
                FAVOURITES.contains(module.getName()) ? RED : new Color(110, 110, 120, 255));

        String name = Lang.module(module.getName());
        int available = card.w - 70;
        while (mc.textRenderer.getWidth(name) > available && name.length() > 4) {
            name = name.substring(0, name.length() - 2);
        }
        text(context, name, card.x + 32, card.y + 12,
                module.isEnabled() ? TEXT : new Color(200, 200, 208, 255), true);
        drawChevron(context, card.x + card.w - 40, card.y + 15, card.expanded);
        drawToggle(context, card.x + card.w - 26, card.y + 10, module.isEnabled());

        if (!card.expanded) return;

        int sy = card.y + CARD_H + 4;
        for (Setting setting : module.getSettings()) {
            drawSetting(context, setting, card.x + 10, sy, card.w - 20);
            sy += SETTING_H;
        }
    }

    private void drawSetting(DrawContext context, Setting setting, int x, int y, int w) {
        String label = Lang.setting(setting.getName());
        if (setting instanceof BooleanSetting boolSetting) {
            text(context, label, x, y + 4, MUTED, false);
            drawCheckbox(context, x + w - 16, y + 2, boolSetting.getValue());
        } else if (setting instanceof NumberSetting number) {
            text(context, label, x, y + 2, MUTED, false);
            String value = format(number.getValue());
            text(context, value, x + w - (int) mc.textRenderer.getWidth(value), y + 2, TEXT, false);
            double norm = (number.getValue() - number.getMin()) / (number.getMax() - number.getMin());
            int px = x + (int) (w * norm);
            context.fill(x, y + 13, x + w, y + 16, TRACK.getRGB());
            context.fill(x, y + 13, px, y + 16, ACCENT.getRGB());
            circle(context, px, y + 14, 3, WHITE);
        } else if (setting instanceof ModeSetting mode) {
            text(context, label, x, y + 4, MUTED, false);
            String value = "< " + Lang.setting(mode.getMode()) + " >";
            text(context, value, x + w - (int) mc.textRenderer.getWidth(value), y + 4, ACCENT, false);
        } else if (setting instanceof KeybindSetting keybind) {
            text(context, label, x, y + 4, MUTED, false);
            String value = keybind.isListening() ? Lang.ui("listening") : keyName(keybind.getKeyCode());
            text(context, value, x + w - (int) mc.textRenderer.getWidth(value), y + 4, ACCENT, false);
        } else if (setting instanceof StringSetting stringSetting) {
            text(context, label + ": " + stringSetting.getValue(), x, y + 4, MUTED, false);
        } else {
            text(context, label, x, y + 4, MUTED, false);
        }
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
        if (key <= 0) return Lang.ui("listening");
        String name = GLFW.glfwGetKeyName(key, 0);
        return name == null || name.isEmpty() ? "KEY" : name.toUpperCase(Locale.ROOT);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        layout();
        computeCards();
        int mx = (int) mouseX;
        int my = (int) mouseY;

        if (hover(mx, my, closeX, winY + 8, 28, 28)) {
            close();
            return true;
        }
        if (hover(mx, my, gridX, winY + 8, 28, 28)) {
            twoColumns = !twoColumns;
            scroll = 0;
            return true;
        }
        if (hover(mx, my, langX, winY + 11, 30, 20)) {
            Lang.toggle();
            return true;
        }
        searchFocused = hover(mx, my, searchX, winY + 10, searchW, 22);

        if (hover(mx, my, allX, winY + 14, 26, 14)) {
            boolean enable = !areAllEnabled();
            for (Module module : filteredModules()) module.setEnabled(enable);
            return true;
        }

        int navY = winY + 62;
        for (NavItem item : nav) {
            if (item.id != null && hover(mx, my, winX + 10, navY - 3, SIDEBAR_W - 20, 22)) {
                selectedId = item.id;
                scroll = 0;
                return true;
            }
            navY += 26;
        }

        if (mx < contentX || mx > contentX + contentW || my < contentY || my > contentY + contentH) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        for (CardRect card : cards) {
            if (mx < card.x || mx > card.x + card.w || my < card.y || my > card.y + card.h) continue;

            if (hover(mx, my, card.x + 8, card.y + 8, 16, 16)) {
                if (!FAVOURITES.add(card.module.getName())) FAVOURITES.remove(card.module.getName());
                return true;
            }
            if (hover(mx, my, card.x + card.w - 26, card.y + 8, 22, 18)) {
                card.module.toggle();
                return true;
            }
            if (hover(mx, my, card.x + card.w - 46, card.y + 8, 16, 18)) {
                EXPANDED.put(card.module.getName(), !card.expanded);
                return true;
            }
            if (my <= card.y + CARD_H) {
                card.module.toggle();
                return true;
            }

            int sy = card.y + CARD_H + 4;
            int sx = card.x + 10;
            int sw = card.w - 20;
            for (Setting setting : card.module.getSettings()) {
                if (setting instanceof BooleanSetting boolSetting) {
                    if (hover(mx, my, sx + sw - 18, sy, 18, 16)) {
                        boolSetting.toggle();
                        return true;
                    }
                } else if (setting instanceof NumberSetting number) {
                    if (hover(mx, my, sx, sy + 10, sw, 10)) {
                        draggedSlider = number;
                        sliderX = sx;
                        sliderW = sw;
                        applySlider(mx);
                        return true;
                    }
                } else if (setting instanceof ModeSetting mode) {
                    if (hover(mx, my, sx, sy, sw, 16)) {
                        mode.cycle();
                        return true;
                    }
                } else if (setting instanceof KeybindSetting keybind) {
                    if (hover(mx, my, sx, sy, sw, 16)) {
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
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
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
        scroll -= verticalAmount * 24f;
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

    private static void heart(DrawContext c, int x, int y, Color color) {
        circle(c, x + 3, y + 3, 2, color);
        circle(c, x + 7, y + 3, 2, color);
        for (int i = 0; i < 6; i++) {
            c.fill(x + 1 + i / 2, y + 4 + i, x + 9 - i / 2, y + 5 + i, color.getRGB());
        }
    }

    private static void drawSearchIcon(DrawContext c, int x, int y) {
        circle(c, x, y - 1, 3, MUTED);
        c.fill(x + 3, y + 2, x + 4, y + 7, MUTED.getRGB());
    }

    private static void drawGridIcon(DrawContext c, int x, int y) {
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 2; col++) {
                c.fill(x + col * 5, y + row * 5, x + col * 5 + 4, y + row * 5 + 4, MUTED.getRGB());
            }
        }
    }

    private static void drawCloseIcon(DrawContext c, int x, int y) {
        for (int i = -4; i <= 4; i++) {
            c.fill(x + i, y + i, x + i + 1, y + i + 1, MUTED.getRGB());
            c.fill(x + i, y - i, x + i + 1, y - i + 1, MUTED.getRGB());
        }
    }

    private static void drawChevron(DrawContext c, int x, int y, boolean up) {
        for (int i = 0; i < 4; i++) {
            int width = 7 - i;
            int yy = up ? y + i : y + 3 - i;
            c.fill(x + i, yy, x + i + width, yy + 1, MUTED.getRGB());
        }
    }

    private static void drawToggle(DrawContext c, int x, int y, boolean enabled) {
        round(c, x, y, 26, 14, 7, enabled ? ACCENT : TRACK);
        circle(c, enabled ? x + 19 : x + 7, y + 7, 5, WHITE);
    }

    private static void drawCheckbox(DrawContext c, int x, int y, boolean enabled) {
        if (enabled) {
            round(c, x, y, 14, 14, 3, ACCENT);
            c.fill(x + 3, y + 7, x + 6, y + 10, WHITE.getRGB());
            c.fill(x + 5, y + 5, x + 11, y + 7, WHITE.getRGB());
        } else {
            round(c, x, y, 14, 14, 3, FIELD);
            c.drawBorder(x, y, 14, 14, BORDER.getRGB());
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
