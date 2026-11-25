package cc.silk.gui.virus;

import cc.silk.SilkClient;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.module.setting.BooleanSetting;
import cc.silk.module.setting.KeybindSetting;
import cc.silk.module.setting.ModeSetting;
import cc.silk.module.setting.NumberSetting;
import cc.silk.module.setting.Setting;
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
 * Click GUI in the "Virus" style: left navigation bar, two columns of module
 * cards with favourite hearts, inline switches and expandable settings.
 */
public final class VirusGui extends Screen {

    private final MinecraftClient mc = MinecraftClient.getInstance();

    private static final Color WINDOW = new Color(20, 20, 23, 255);
    private static final Color SIDEBAR = new Color(14, 14, 16, 255);
    private static final Color HEADER = new Color(22, 22, 26, 255);
    private static final Color CARD = new Color(28, 28, 33, 255);
    private static final Color CARD_HOVER = new Color(34, 34, 40, 255);
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
    private String selectedId = "player";
    private String search = "";
    private boolean searchFocused;
    private boolean twoColumns = true;
    private float scroll;
    private int maxScroll;

    private NumberSetting draggedSlider;
    private int sliderX, sliderY, sliderW;

    private int winX, winY, winW, winH;
    private int contentX, contentY, contentW, contentH;

    public VirusGui() {
        super(Text.literal("Virus"));
        nav.add(new NavItem("Бой", "combat", Category.COMBAT));
        nav.add(new NavItem("Движение", "movement", Category.MOVEMENT));
        nav.add(new NavItem("Игрок", "player", Category.PLAYER));
        nav.add(new NavItem("Визуалы", "visual", Category.RENDER));
        nav.add(new NavItem("Утилиты", "misc", Category.MISC));
        nav.add(new NavItem("Разное", null, null));
        nav.add(new NavItem("Избранное", "favorites", null));
        nav.add(new NavItem("Друзья", "friends", null));
        nav.add(new NavItem("Конфиги", "config", Category.CONFIG));
        nav.add(new NavItem("Интерфейс", "interface", Category.CLIENT));
        nav.add(new NavItem("Макросы", "macros", null));

        EXPANDED.putIfAbsent("Ping Spoof", true);
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
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        layout();

        context.fill(0, 0, width, height, 0x9008080B);
        round(context, winX, winY, winW, winH, 10, WINDOW);
        round(context, winX, winY, SIDEBAR_W, winH, 10, SIDEBAR);
        context.fill(winX + SIDEBAR_W - 10, winY, winX + SIDEBAR_W, winY + winH, SIDEBAR.getRGB());

        drawSidebar(context, mouseX, mouseY);
        drawHeader(context, mouseX, mouseY);
        drawModules(context, mouseX, mouseY);
    }

    private void drawSidebar(DrawContext context, int mouseX, int mouseY) {
        text(context, "Virus", winX + 22, winY + 22, TEXT, true);

        int y = winY + 62;
        for (NavItem item : nav) {
            if (item.id == null) {
                text(context, item.label, winX + 22, y + 2, MUTED, false);
                y += 26;
                continue;
            }
            boolean active = selectedId.equals(item.id);
            if (active) {
                round(context, winX + 10, y - 3, SIDEBAR_W - 20, 22, 5, new Color(38, 38, 45, 255));
                context.fill(winX + 10, y - 3, 3, 22, ACCENT.getRGB());
            }
            Color color = active ? TEXT : new Color(150, 150, 160, 255);
            text(context, item.label, winX + 24, y + 2, color, active);
            y += 26;
        }

        int footY = winY + winH - 48;
        context.fill(winX + 12, footY - 8, winX + SIDEBAR_W - 12, footY - 7, BORDER.getRGB());
        circle(context, winX + 26, footY + 8, 9, new Color(56, 56, 66, 255));
        text(context, "Администратор", winX + 42, footY + 2, TEXT, false);
        text(context, "1337", winX + 42, footY + 14, MUTED, false);
    }

    private void drawHeader(DrawContext context, int mouseX, int mouseY) {
        int x = winX + SIDEBAR_W;
        context.fill(x, winY + HEADER_H - 1, winX + winW, winY + HEADER_H, BORDER.getRGB());
        text(context, titleFor(selectedId), x + 16, winY + 15, TEXT, true);

        int allX = x + 210;
        text(context, "Включить все", allX - 92, winY + 16, MUTED, false);
        boolean allOn = areAllEnabled();
        drawToggle(context, allX, winY + 14, allOn);

        int searchW = 150;
        int searchX = winX + winW - searchW - 76;
        round(context, searchX, winY + 10, searchW, 22, 5, FIELD);
        if (searchFocused) {
            context.fill(searchX, winY + 10, searchX + searchW, winY + 11, ACCENT.getRGB());
        }
        String shown = search.isEmpty() ? "Поиск модулей..." : search;
        Color searchColor = search.isEmpty() ? MUTED : TEXT;
        text(context, shown, searchX + 8, winY + 16, searchColor, false);
        drawSearchIcon(context, searchX + searchW - 18, winY + 16);

        drawGridIcon(context, winX + winW - 54, winY + 16);
        drawCloseIcon(context, winX + winW - 24, winY + 16);
    }

    private void drawModules(DrawContext context, int mouseX, int mouseY) {
        List<Module> modules = modulesFor(selectedId);
        if (search != null && !search.isEmpty()) {
            String q = search.toLowerCase(Locale.ROOT);
            modules.removeIf(m -> !m.getName().toLowerCase(Locale.ROOT).contains(q));
        }

        int cols = twoColumns ? 2 : 1;
        int colW = (contentW - CARD_GAP * (cols - 1)) / cols;
        int rows = (modules.size() + cols - 1) / cols;
        int totalH = rows * (CARD_H + CARD_GAP);
        maxScroll = Math.max(0, totalH - contentH);
        scroll = Math.max(0, Math.min(maxScroll, scroll));

        int index = 0;
        for (Module module : modules) {
            int col = index % cols;
            int row = index / cols;
            int x = contentX + col * (colW + CARD_GAP);
            int y = contentY + row * (CARD_H + CARD_GAP) - (int) scroll;
            boolean expanded = EXPANDED.getOrDefault(module.getName(), false);
            int h = cardHeight(module, expanded);

            if (y + h < contentY || y > contentY + contentH) {
                index++;
                continue;
            }
            drawCard(context, module, x, y, colW, expanded, mouseX, mouseY);
            index++;
        }
    }

    private int cardHeight(Module module, boolean expanded) {
        if (!expanded) return CARD_H;
        int count = 0;
        for (Setting setting : module.getSettings()) {
            if (setting instanceof KeybindSetting) continue;
            count++;
        }
        return CARD_H + 6 + count * SETTING_H;
    }

    private void drawCard(DrawContext context, Module module, int x, int y, int w,
                          boolean expanded, int mouseX, int mouseY) {
        boolean hovered = hover(mouseX, mouseY, x, y, w, CARD_H);
        round(context, x, y, w, cardHeight(module, expanded), 7,
                hovered ? CARD_HOVER : CARD);
        if (module.isEnabled()) {
            context.fill(x, y + 8, x + 2, y + CARD_H - 8, ACCENT.getRGB());
        }

        boolean fav = FAVOURITES.contains(module.getName());
        heart(context, x + 16, y + 16, fav ? RED : new Color(110, 110, 120, 255));

        String name = module.getName();
        int nameX = x + 32;
        int available = w - 32 - 40;
        while (mc.textRenderer.getWidth(name) > available && name.length() > 4) {
            name = name.substring(0, name.length() - 2);
        }
        text(context, name, nameX, y + 12, module.isEnabled() ? TEXT : new Color(200, 200, 208, 255), true);
        drawChevron(context, x + w - 40, y + 15, expanded);
        drawToggle(context, x + w - 26, y + 10, module.isEnabled());

        if (!expanded) return;

        int sy = y + CARD_H + 4;
        for (Setting setting : module.getSettings()) {
            if (setting instanceof KeybindSetting) continue;
            drawSetting(context, module, setting, x + 10, sy, w - 20, mouseX, mouseY);
            sy += SETTING_H;
        }
    }

    private void drawSetting(DrawContext context, Module module, Setting setting,
                             int x, int y, int w, int mouseX, int mouseY) {
        String label = setting.getName();
        if (setting instanceof BooleanSetting boolSetting) {
            text(context, label, x, y + 4, MUTED, false);
            boolean value = boolSetting.getValue();
            drawCheckbox(context, x + w - 16, y + 2, value);
        } else if (setting instanceof NumberSetting number) {
            text(context, label, x, y + 2, MUTED, false);
            String value = format(number.getValue());
            text(context, value, x + w - (int) mc.textRenderer.getWidth(value), y + 2, TEXT, false);
            int trackX = x;
            int trackY = y + 13;
            int trackW = w;
            double norm = (number.getValue() - number.getMin()) / (number.getMax() - number.getMin());
            context.fill(trackX, trackY, trackX + trackW, trackY + 3, TRACK.getRGB());
            context.fill(trackX, trackY, trackX + (int) (trackW * norm), trackY + 3, ACCENT.getRGB());
            circle(context, trackX + (int) (trackW * norm), trackY + 1, 3, WHITE);
        } else if (setting instanceof ModeSetting mode) {
            text(context, label, x, y + 4, MUTED, false);
            String value = "< " + mode.getMode() + " >";
            text(context, value, x + w - (int) mc.textRenderer.getWidth(value), y + 4, ACCENT, false);
        } else {
            text(context, label, x, y + 4, MUTED, false);
        }
    }

    private static String format(double value) {
        return value == Math.floor(value) ? Integer.toString((int) value) : String.format("%.1f", value);
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

    private boolean areAllEnabled() {
        List<Module> modules = modulesFor(selectedId);
        if (modules.isEmpty()) return false;
        for (Module module : modules) {
            if (!module.isEnabled()) return false;
        }
        return true;
    }

    private String titleFor(String id) {
        for (NavItem item : nav) {
            if (id.equals(item.id)) return item.label;
        }
        return "Virus";
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        int mx = (int) mouseX;
        int my = (int) mouseY;

        if (hover(mx, my, winX + winW - 40, winY + 8, 24, 24)) {
            close();
            return true;
        }
        if (hover(mx, my, winX + winW - 70, winY + 8, 22, 24)) {
            twoColumns = !twoColumns;
            return true;
        }
        int searchW = 150;
        int searchX = winX + winW - searchW - 76;
        searchFocused = hover(mx, my, searchX, winY + 10, searchW, 22);

        int allX = winX + SIDEBAR_W + 210;
        if (hover(mx, my, allX, winY + 14, 26, 14)) {
            boolean enable = !areAllEnabled();
            for (Module module : modulesFor(selectedId)) {
                module.setEnabled(enable);
            }
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

        List<Module> modules = modulesFor(selectedId);
        if (search != null && !search.isEmpty()) {
            String q = search.toLowerCase(Locale.ROOT);
            modules.removeIf(m -> !m.getName().toLowerCase(Locale.ROOT).contains(q));
        }
        int cols = twoColumns ? 2 : 1;
        int colW = (contentW - CARD_GAP * (cols - 1)) / cols;

        for (int i = 0; i < modules.size(); i++) {
            Module module = modules.get(i);
            int col = i % cols;
            int row = i / cols;
            int x = contentX + col * (colW + CARD_GAP);
            int y = contentY + row * (CARD_H + CARD_GAP) - (int) scroll;
            boolean expanded = EXPANDED.getOrDefault(module.getName(), false);

            if (hover(mx, my, x + 8, y + 8, 16, 16)) {
                if (!FAVOURITES.add(module.getName())) FAVOURITES.remove(module.getName());
                return true;
            }
            if (hover(mx, my, x + colW - 26, y + 8, 22, 18)) {
                module.toggle();
                return true;
            }
            if (hover(mx, my, x + colW - 46, y + 8, 16, 18)) {
                EXPANDED.put(module.getName(), !expanded);
                return true;
            }
            if (hover(mx, my, x, y, colW, CARD_H)) {
                module.toggle();
                return true;
            }
            if (!expanded) continue;

            int sy = y + CARD_H + 4;
            for (Setting setting : module.getSettings()) {
                if (setting instanceof KeybindSetting) continue;
                int sx = x + 10;
                int sw = colW - 20;
                if (setting instanceof BooleanSetting boolSetting) {
                    if (hover(mx, my, sx + sw - 18, sy, 18, 16)) {
                        boolSetting.toggle();
                        return true;
                    }
                } else if (setting instanceof NumberSetting number) {
                    if (hover(mx, my, sx, sy + 10, sw, 10)) {
                        draggedSlider = number;
                        sliderX = sx;
                        sliderY = sy + 13;
                        sliderW = sw;
                        applySlider(mx);
                        return true;
                    }
                } else if (setting instanceof ModeSetting mode) {
                    if (hover(mx, my, sx, sy, sw, 16)) {
                        mode.cycle();
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
        if (draggedSlider == null) return;
        double norm = (mouseX - sliderX) / (double) sliderW;
        double value = draggedSlider.getMin() + norm * (draggedSlider.getMax() - draggedSlider.getMin());
        draggedSlider.setValue(value);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll -= verticalAmount * 24f;
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
        final String label;
        final String id;
        final Category category;

        NavItem(String label, String id, Category category) {
            this.label = label;
            this.id = id;
            this.category = category;
        }
    }
}
