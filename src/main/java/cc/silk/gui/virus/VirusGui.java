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

/** Compact pink click GUI: 5 tabs, centered panel, gradient-style module buttons. */
public final class VirusGui extends Screen {

    private final MinecraftClient mc = MinecraftClient.getInstance();

    private static final Color PANEL = new Color(26, 12, 26, 242);
    private static final Color HEADER = new Color(48, 16, 44, 255);
    private static final Color PINK = new Color(255, 92, 160, 255);
    private static final Color PINK_LIGHT = new Color(255, 150, 200, 255);
    private static final Color PINK_DARK = new Color(120, 40, 85, 255);
    private static final Color WHITE = new Color(255, 245, 250, 255);
    private static final Color MUTED = new Color(230, 170, 205, 255);
    private static final Color TRACK = new Color(90, 40, 70, 255);

    private static final int TAB_H = 24;
    private static final int ROW_H = 22;
    private static final int SET_H = 18;
    private static final String[] TABS = {"combat", "movement", "player", "visual", "misc"};

    private static final Set<String> FAVOURITES = new HashSet<>();
    private static final Map<String, Boolean> EXPANDED = new HashMap<>();

    private final List<Card> cards = new ArrayList<>();
    private int tab;
    private String search = "";
    private boolean searchFocused;
    private float scroll;
    private int maxScroll;
    private Module listening;
    private NumberSetting dragged;
    private int sliderX, sliderW;
    private Module hovered;
    private int hoverX, hoverY;

    private int panelX, panelY, panelW, panelH;
    private int listX, listY, listW, listH;
    private int searchX, searchY, searchW;
    private int langX, langY;

    public VirusGui() {
        super(Text.literal("Virus"));
        EXPANDED.putIfAbsent("Kill Aura", true);
        EXPANDED.putIfAbsent("Aim Assist", true);
        EXPANDED.putIfAbsent("Kill Effect", true);
    }

    private void layout() {
        panelW = Math.min(width - 40, 280);
        panelH = Math.min(height - 40, 470);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        listX = panelX + 8;
        listY = panelY + TAB_H + 30 + 20;
        listW = panelW - 16;
        listH = panelH - TAB_H - 30 - 28;
        searchX = panelX + 8;
        searchY = panelY + TAB_H + 4;
        searchW = panelW - 16;
        langX = panelX + 8;
        langY = panelY + panelH - 20;
    }

    private List<Module> modulesFor() {
        List<Module> result = new ArrayList<>();
        List<Module> all = SilkClient.INSTANCE.getModuleManager().getModules();
        for (Module module : all) {
            Category c = module.getModuleCategory();
            boolean add = switch (tab) {
                case 0 -> c == Category.COMBAT;
                case 1 -> c == Category.MOVEMENT;
                case 2 -> c == Category.PLAYER || c == Category.FARM;
                case 3 -> c == Category.RENDER || c == Category.VISUAL || c == Category.PARTICLES;
                default -> c == Category.MISC || c == Category.CLIENT || c == Category.CONFIG;
            };
            if (add) result.add(module);
        }
        if (!search.isEmpty()) {
            String q = search.toLowerCase(Locale.ROOT);
            result.removeIf(m -> !m.getName().toLowerCase(Locale.ROOT).contains(q)
                    && !Lang.module(m.getName()).toLowerCase(Locale.ROOT).contains(q));
        }
        return result;
    }

    private void computeCards() {
        cards.clear();
        List<Module> modules = modulesFor();
        int y = listY - (int) scroll;
        for (Module module : modules) {
            boolean open = EXPANDED.getOrDefault(module.getName(), false);
            int h = ROW_H + (open ? 2 + module.getSettings().size() * SET_H : 0);
            cards.add(new Card(module, listX, y, listW, h, open));
            y += h + 4;
        }
        maxScroll = Math.max(0, y + (int) scroll - listH - listY);
        scroll = Math.max(0, Math.min(maxScroll, scroll));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        layout();
        computeCards();

        context.fill(0, 0, width, height, 0x980C0612);
        round(context, panelX, panelY, panelW, panelH, 10, PANEL);
        context.drawBorder(panelX, panelY, panelW, panelH, PINK.getRGB());

        int tabW = panelW / TABS.length;
        for (int i = 0; i < TABS.length; i++) {
            int x = panelX + i * tabW;
            boolean active = i == tab;
            round(context, x + 1, panelY + 1, tabW - 2, TAB_H - 2, 4,
                    active ? PINK : new Color(60, 28, 52, 255));
            String label = Lang.ui(TABS[i]);
            while (mc.textRenderer.getWidth(label) > tabW - 6 && label.length() > 3) {
                label = label.substring(0, label.length() - 2);
            }
            context.drawText(mc.textRenderer, Text.literal(label),
                    x + (tabW - mc.textRenderer.getWidth(label)) / 2, panelY + 8,
                    active ? new Color(30, 8, 24, 255).getRGB() : MUTED.getRGB(), false);
        }

        round(context, searchX, searchY, searchW, 18, 4, new Color(48, 20, 42, 255));
        String shown = search.isEmpty() ? Lang.ui("hint") : search;
        while (mc.textRenderer.getWidth(shown) > searchW - 12 && shown.length() > 4) {
            shown = shown.substring(0, shown.length() - 2);
        }
        context.drawText(mc.textRenderer, Text.literal(shown), searchX + 6, searchY + 5,
                search.isEmpty() ? MUTED.getRGB() : WHITE.getRGB(), false);

        hovered = null;
        for (Card card : cards) {
            if (card.y + card.h < listY || card.y > listY + listH) continue;
            drawCard(context, card, mouseX, mouseY);
        }
        round(context, langX, langY, 30, 16, 4, new Color(70, 30, 60, 255));
        context.drawText(mc.textRenderer, Text.literal(Lang.russian() ? "RU" : "EN"),
                langX + 9, langY + 4, PINK_LIGHT.getRGB(), false);

        if (hovered != null) drawTooltip(context, hovered, hoverX, hoverY);
    }

    private void drawCard(DrawContext context, Card card, int mouseX, int mouseY) {
        Module module = card.module;
        boolean over = mouseX >= card.x && mouseX <= card.x + card.w
                && mouseY >= card.y && mouseY <= card.y + ROW_H;
        Color bg = module.isEnabled()
                ? (over ? PINK_LIGHT : PINK)
                : new Color(70, 28, 55, 235);
        round(context, card.x, card.y, card.w, card.h, 5, bg);
        if (module.isEnabled()) {
            round(context, card.x, card.y, card.w, Math.min(8, card.h), 5, PINK_LIGHT);
        }

        String name = Lang.module(module.getName());
        while (mc.textRenderer.getWidth(name) > card.w - 40 && name.length() > 4) {
            name = name.substring(0, name.length() - 2);
        }
        context.drawText(mc.textRenderer, Text.literal(name), card.x + 7, card.y + 6,
                module.isEnabled() ? new Color(30, 8, 24, 255).getRGB() : WHITE.getRGB(), false);
        star(context, card.x + card.w - 18, card.y + 10, FAVOURITES.contains(module.getName()));
        if (over) {
            hovered = module;
            hoverX = mouseX;
            hoverY = mouseY;
        }
        if (!card.open) return;

        int sy = card.y + ROW_H + 2;
        for (Setting setting : module.getSettings()) {
            drawSetting(context, setting, card.x + 6, sy, card.w - 12);
            sy += SET_H;
        }
    }

    private void drawSetting(DrawContext context, Setting setting, int x, int y, int w) {
        String label = Lang.setting(setting.getName());
        if (setting instanceof BooleanSetting boolSetting) {
            context.drawText(mc.textRenderer, Text.literal(label), x, y + 2, MUTED.getRGB(), false);
            checkbox(context, x + w - 13, y + 1, boolSetting.getValue());
        } else if (setting instanceof NumberSetting number) {
            context.drawText(mc.textRenderer, Text.literal(label), x, y, MUTED.getRGB(), false);
            String value = number.getValue() == Math.floor(number.getValue())
                    ? Integer.toString((int) number.getValue())
                    : String.format("%.1f", number.getValue());
            context.drawText(mc.textRenderer, Text.literal(value),
                    x + w - (int) mc.textRenderer.getWidth(value), y, WHITE.getRGB(), false);
            double n = (number.getValue() - number.getMin()) / (number.getMax() - number.getMin());
            int px = x + (int) (w * n);
            context.fill(x, y + 11, x + w, y + 13, TRACK.getRGB());
            context.fill(x, y + 11, px, y + 13, PINK.getRGB());
            circle(context, px, y + 12, 2, WHITE);
        } else if (setting instanceof ModeSetting mode) {
            context.drawText(mc.textRenderer, Text.literal(label), x, y + 2, MUTED.getRGB(), false);
            String value = "< " + Lang.setting(mode.getMode()) + " >";
            context.drawText(mc.textRenderer, Text.literal(value),
                    x + w - (int) mc.textRenderer.getWidth(value), y + 2, PINK_LIGHT.getRGB(), false);
        } else if (setting instanceof KeybindSetting keybind) {
            context.drawText(mc.textRenderer, Text.literal(label), x, y + 2, MUTED.getRGB(), false);
            String value = keybind.isListening() ? "..." : keyName(keybind.getKeyCode());
            context.drawText(mc.textRenderer, Text.literal(value),
                    x + w - (int) mc.textRenderer.getWidth(value), y + 2, PINK_LIGHT.getRGB(), false);
        } else if (setting instanceof StringSetting stringSetting) {
            context.drawText(mc.textRenderer, Text.literal(label + ": " + stringSetting.getValue()),
                    x, y + 2, MUTED.getRGB(), false);
        } else {
            context.drawText(mc.textRenderer, Text.literal(label), x, y + 2, MUTED.getRGB(), false);
        }
    }

    private void drawTooltip(DrawContext context, Module module, int mx, int my) {
        String desc = Lang.setting(module.getDescription());
        if (desc == null || desc.isEmpty()) return;
        int tw = Math.min((int) mc.textRenderer.getWidth(desc) + 12, 220);
        int tx = Math.min(mx + 8, width - tw - 10);
        int ty = Math.min(my + 8, height - 22);
        round(context, tx, ty, tw, 18, 4, new Color(24, 8, 22, 245));
        context.drawBorder(tx, ty, tw, 18, PINK.getRGB());
        while (mc.textRenderer.getWidth(desc) > tw - 10 && desc.length() > 4) {
            desc = desc.substring(0, desc.length() - 2);
        }
        context.drawText(mc.textRenderer, Text.literal(desc), tx + 5, ty + 5, WHITE.getRGB(), false);
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

        int tabW = panelW / TABS.length;
        for (int i = 0; i < TABS.length; i++) {
            if (mx >= panelX + i * tabW && mx <= panelX + (i + 1) * tabW
                    && my >= panelY && my <= panelY + TAB_H) {
                tab = i;
                scroll = 0;
                return true;
            }
        }
        searchFocused = mx >= searchX && mx <= searchX + searchW && my >= searchY && my <= searchY + 18;
        if (mx >= langX && mx <= langX + 30 && my >= langY && my <= langY + 16) {
            Lang.toggle();
            return true;
        }

        for (Card card : cards) {
            if (mx < card.x || mx > card.x + card.w || my < card.y || my > card.y + card.h) continue;
            if (mx >= card.x + card.w - 24 && my >= card.y + 4 && my <= card.y + 18) {
                String id = card.module.getName();
                if (!FAVOURITES.add(id)) FAVOURITES.remove(id);
                return true;
            }
            if (my <= card.y + ROW_H) {
                card.module.toggle();
                return true;
            }
            int sy = card.y + ROW_H + 2;
            int sx = card.x + 6;
            int sw = card.w - 12;
            for (Setting setting : card.module.getSettings()) {
                if (setting instanceof BooleanSetting boolSetting) {
                    if (mx >= sx + sw - 15 && mx <= sx + sw && my >= sy && my <= sy + 14) {
                        boolSetting.toggle();
                        return true;
                    }
                } else if (setting instanceof NumberSetting number) {
                    if (mx >= sx && mx <= sx + sw && my >= sy + 8 && my <= sy + 16) {
                        dragged = number;
                        sliderX = sx;
                        sliderW = sw;
                        dragged.setValue(number.getMin() + (mx - sx) / (double) sw
                                * (number.getMax() - number.getMin()));
                        return true;
                    }
                } else if (setting instanceof ModeSetting mode) {
                    if (mx >= sx && mx <= sx + sw && my >= sy && my <= sy + 14) {
                        mode.cycle();
                        return true;
                    }
                } else if (setting instanceof KeybindSetting keybind) {
                    if (mx >= sx && mx <= sx + sw && my >= sy && my <= sy + 14) {
                        keybind.setListening(true);
                        listening = card.module;
                        return true;
                    }
                }
                sy += SET_H;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragged != null) {
            dragged.setValue(dragged.getMin() + (mouseX - sliderX) / (double) sliderW
                    * (dragged.getMax() - dragged.getMin()));
            return true;
        }
        scroll -= deltaY * 0.6f;
        if (scroll < 0) scroll = 0;
        if (maxScroll > 0) scroll = Math.min(maxScroll, scroll);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragged = null;
        return super.mouseReleased(mouseX, mouseY, button);
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
        if (listening != null) {
            int value = keyCode == GLFW.GLFW_KEY_ESCAPE ? -1 : keyCode;
            for (Setting setting : listening.getSettings()) {
                if (setting instanceof KeybindSetting keybind) {
                    keybind.setKeyCode(value);
                    keybind.setListening(false);
                }
            }
            listening = null;
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
        dragged = null;
        listening = null;
        if (mc != null) mc.setScreen(null);
    }

    @Override
    public boolean shouldPause() {
        return false;
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

    private static void star(DrawContext c, int x, int y, boolean active) {
        Color color = active ? new Color(255, 220, 100, 255) : MUTED;
        c.fill(x + 5, y - 3, x + 7, y + 9, color.getRGB());
        c.fill(x, y + 2, x + 12, y + 4, color.getRGB());
        for (int i = 0; i < 4; i++) {
            c.fill(x + i, y - 1 + i, x + 12 - i, y + i + 1, color.getRGB());
        }
    }

    private static void checkbox(DrawContext c, int x, int y, boolean enabled) {
        round(c, x, y, 12, 12, 3, enabled ? PINK : new Color(70, 30, 58, 255));
        if (enabled) {
            c.fill(x + 2, y + 6, x + 5, y + 9, WHITE.getRGB());
            c.fill(x + 4, y + 4, x + 10, y + 6, WHITE.getRGB());
        }
    }

    private static final class Card {
        final Module module;
        final int x, y, w, h;
        final boolean open;

        Card(Module module, int x, int y, int w, int h, boolean open) {
            this.module = module;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.open = open;
        }
    }
}
