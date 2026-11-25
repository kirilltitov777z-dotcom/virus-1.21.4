package cc.silk.gui.crest;

import cc.silk.SilkClient;
import cc.silk.gui.components.UIRenderer;
import cc.silk.gui.virus.Lang;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.session.Session;
import net.minecraft.text.Text;

import java.awt.Color;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Account switcher on the vanilla title screen.
 * Important: no NanoVG / texture loading here — that crashed on Android launchers.
 */
public final class MainMenu {

    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final Color PANEL = new Color(24, 12, 26, 235);
    private static final Color ACCENT = new Color(255, 105, 170, 255);
    private static final Color TEXT = new Color(255, 236, 246, 255);
    private static final Color MUTED = new Color(205, 160, 185, 255);

    private static boolean panelOpen;
    private static boolean lastPressed;
    private static List<String> accounts;
    private static File accountsFile;

    private MainMenu() {
    }

    public static void drawOverlay(DrawContext context, int mouseX, int mouseY) {
        boolean pressed = isPressed();
        if (pressed && !lastPressed) mouseClicked(mouseX, mouseY);
        lastPressed = pressed;

        int labelWidth = 152;
        UIRenderer.renderRoundedRect(context, 14, 14, labelWidth, 24, 5, PANEL);
        context.drawBorder(14, 14, labelWidth, 24, new Color(255, 120, 180, 220).getRGB());
        String label = (Lang.russian() ? "Аккаунт: " : "Account: ") + currentName();
        while (mc.textRenderer.getWidth(label) > labelWidth - 12 && label.length() > 6) {
            label = label.substring(0, label.length() - 2);
        }
        context.drawText(mc.textRenderer, Text.literal(label), 21, 21, TEXT.getRGB(), false);

        if (!panelOpen) return;
        List<String> list = getAccounts();
        int h = 26 + list.size() * 20 + 4;
        UIRenderer.renderRoundedRect(context, 14, 44, 176, h, 6, PANEL);
        context.drawBorder(14, 44, 176, h, new Color(255, 120, 180, 220).getRGB());
        for (int i = 0; i < list.size(); i++) {
            String name = list.get(i);
            int y = 50 + i * 20;
            if (mouseX >= 18 && mouseX <= 186 && mouseY >= y && mouseY <= y + 18) {
                context.fill(18, y, 186, y + 18, new Color(90, 35, 70, 200).getRGB());
            }
            context.drawText(mc.textRenderer, Text.literal(name), 24, y + 5,
                    name.equals(currentName()) ? ACCENT.getRGB() : MUTED.getRGB(), false);
        }
    }

    private static boolean isPressed() {
        try {
            return org.lwjgl.glfw.GLFW.glfwGetMouseButton(
                    mc.getWindow().getHandle(), org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT)
                    == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void mouseClicked(int mx, int my) {
        if (mx >= 14 && mx <= 166 && my >= 14 && my <= 38) {
            panelOpen = !panelOpen;
            return;
        }
        if (!panelOpen) return;
        List<String> list = getAccounts();
        for (int i = 0; i < list.size(); i++) {
            int y = 50 + i * 20;
            if (mx >= 18 && mx <= 186 && my >= y && my <= y + 18) {
                switchAccount(list.get(i));
                panelOpen = false;
                return;
            }
        }
        panelOpen = false;
    }

    private static String currentName() {
        try {
            return mc.getSession().getUsername();
        } catch (Throwable ignored) {
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
                if (current.equals(field.get(session))) {
                    field.set(session, name);
                    break;
                }
            }
            saveAccounts();
        } catch (Throwable ignored) {
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
        } catch (Throwable ignored) {
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
        } catch (Throwable ignored) {
        }
    }
}
