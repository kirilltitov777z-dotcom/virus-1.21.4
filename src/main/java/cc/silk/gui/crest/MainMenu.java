package cc.silk.gui.crest;

import cc.silk.SilkClient;
import cc.silk.gui.components.UIRenderer;
import cc.silk.gui.virus.Lang;
import cc.silk.utils.render.nanovg.NanoVGRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.session.Session;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Custom title-screen background, logo and on-screen account switcher. */
public final class MainMenu {

    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final Color PANEL = new Color(20, 14, 34, 225);
    private static final Color ACCENT = new Color(158, 112, 255, 255);
    private static final Color TEXT = new Color(235, 228, 255, 255);
    private static final Color MUTED = new Color(160, 145, 190, 255);

    private static int background = -1;
    private static int logo = -1;
    private static boolean panelOpen;
    private static boolean lastPressed;
    private static List<String> accounts;
    private static File accountsFile;

    private MainMenu() {
    }

    private static void initImages() {
        if (background >= 0) return;
        NanoVGRenderer.init();
        background = NanoVGRenderer.loadImage("assets/silk/textures/gui/title_bg.png");
        logo = NanoVGRenderer.loadImage("assets/silk/textures/logo.png");
    }

    public static void drawBackground(int width, int height) {
        try {
            initImages();
            NanoVGRenderer.beginFrame();
            NanoVGRenderer.drawImage(background, 0, 0, width, height, Color.WHITE);
            NanoVGRenderer.drawImage(logo, width / 2f - 58, 8, 116, 116, Color.WHITE);
            NanoVGRenderer.endFrame();
        } catch (Exception ignored) {
        }
    }

    public static void drawOverlay(DrawContext context, int mouseX, int mouseY) {
        boolean pressed = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT)
                == GLFW.GLFW_PRESS;
        if (pressed && !lastPressed) {
            mouseClicked(mouseX, mouseY, 0);
        }
        lastPressed = pressed;

        int labelWidth = 152;
        UIRenderer.renderRoundedRect(context, 14, 14, labelWidth, 24, 5, PANEL);
        context.drawBorder(14, 14, labelWidth, 24, new Color(90, 60, 150, 220).getRGB());
        String label = (Lang.russian() ? "Аккаунт: " : "Account: ") + currentName();
        while (mc.textRenderer.getWidth(label) > labelWidth - 12 && label.length() > 6) {
            label = label.substring(0, label.length() - 2);
        }
        context.drawText(mc.textRenderer, Text.literal(label), 21, 21, TEXT.getRGB(), false);

        if (!panelOpen) return;
        List<String> list = getAccounts();
        int h = 26 + list.size() * 20 + 4;
        UIRenderer.renderRoundedRect(context, 14, 44, 176, h, 6, PANEL);
        context.drawBorder(14, 44, 176, h, new Color(90, 60, 150, 220).getRGB());
        for (int i = 0; i < list.size(); i++) {
            String name = list.get(i);
            int y = 50 + i * 20;
            if (mouseX >= 18 && mouseX <= 186 && mouseY >= y && mouseY <= y + 18) {
                context.fill(18, y, 186, y + 18, new Color(60, 40, 100, 200).getRGB());
            }
            context.drawText(mc.textRenderer, Text.literal(name), 24, y + 5,
                    name.equals(currentName()) ? ACCENT.getRGB() : MUTED.getRGB(), false);
        }
    }

    public static boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        int mx = (int) mouseX;
        int my = (int) mouseY;

        if (mx >= 14 && mx <= 166 && my >= 14 && my <= 38) {
            panelOpen = !panelOpen;
            return true;
        }
        if (!panelOpen) return false;
        List<String> list = getAccounts();
        for (int i = 0; i < list.size(); i++) {
            int y = 50 + i * 20;
            if (mx >= 18 && mx <= 186 && my >= y && my <= y + 18) {
                switchAccount(list.get(i));
                panelOpen = false;
                return true;
            }
        }
        panelOpen = false;
        return false;
    }

    private static String currentName() {
        try {
            return mc.getSession().getUsername();
        } catch (Exception ignored) {
            return "Player";
        }
    }

    private static void switchAccount(String name) {
        try {
            Session session = mc.getSession();
            String current = session.getUsername();
            for (Field field : Session.class.getDeclaredFields()) {
                if (field.getType() != String.class) continue;
                field.setAccessible(true);
                Object value = field.get(session);
                if (current.equals(value)) {
                    field.set(session, name);
                    break;
                }
            }
            saveAccounts();
        } catch (Exception ignored) {
        }
    }

    private static List<String> getAccounts() {
        if (accounts != null) return accounts;
        accounts = new ArrayList<>();
        try {
            if (SilkClient.mc != null && SilkClient.mc.runDirectory != null) {
                accountsFile = new File(SilkClient.mc.runDirectory, "virus/accounts.txt");
                if (accountsFile.isFile()) {
                    for (String line : Files.readAllLines(accountsFile.toPath(), StandardCharsets.UTF_8)) {
                        if (!line.trim().isEmpty()) accounts.add(line.trim());
                    }
                }
            }
        } catch (Exception ignored) {
        }
        if (accounts.isEmpty()) {
            accounts.addAll(Arrays.asList("Ginger", "Crest", "Player", "Steve", "Alex"));
        }
        return accounts;
    }

    private static void saveAccounts() {
        try {
            if (accountsFile == null) return;
            File parent = accountsFile.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            Files.write(accountsFile.toPath(), accounts, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }
}
