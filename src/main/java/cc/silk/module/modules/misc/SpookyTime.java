package cc.silk.module.modules.misc;

import cc.silk.SilkClient;
import cc.silk.event.impl.player.TickEvent;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.module.modules.combat.AimAssist;
import cc.silk.module.modules.combat.KillAura;
import cc.silk.module.modules.combat.TriggerBot;
import cc.silk.module.setting.BooleanSetting;
import cc.silk.module.setting.ModeSetting;
import cc.silk.module.setting.NumberSetting;
import cc.silk.utils.notification.Notification;
import cc.silk.utils.notification.NotificationManager;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.text.Text;

/**
 * SpookyTime server profile. Stricter anti-flag limits for combat modules plus
 * an event countdown.
 */
public final class SpookyTime extends Module {

    private final ModeSetting profile = new ModeSetting("Profile", "Safe", "Safe", "Balanced", "Aggressive");
    private final NumberSetting maxCps = new NumberSetting("Max CPS", 1.0, 20.0, 6.0, 0.5);
    private final NumberSetting rotation = new NumberSetting("Max Rotation", 10.0, 180.0, 55.0, 5.0);
    private final BooleanSetting enforce = new BooleanSetting("Enforce Limits", true);
    private final BooleanSetting notify = new BooleanSetting("Notify", true);
    private final BooleanSetting chat = new BooleanSetting("Chat Message", false);
    private final NumberSetting interval = new NumberSetting("Event Interval (min)", 2, 180, 15, 1);

    private long nextEvent;
    private int ticks;

    public SpookyTime() {
        super("Spooky Time", "SpookyTime server bypass profile with event timer", Category.MISC);
        addSettings(profile, maxCps, rotation, enforce, notify, chat, interval);
    }

    @Override
    public void onEnable() {
        nextEvent = System.currentTimeMillis() + (long) interval.getValue() * 60000L;
        apply();
    }

    @EventHandler
    private void onTick(TickEvent event) {
        if (isNull()) return;
        if (enforce.getValue() && ++ticks % 20 == 0) apply();

        if (System.currentTimeMillis() < nextEvent) return;
        if (notify.getValue()) {
            NotificationManager.getInstance().getNotifications().add(new Notification(
                    "SpookyTime", "Spooky Time event active", Notification.NotificationType.MODULE_ENABLED, 3000));
        }
        if (chat.getValue()) mc.player.sendMessage(Text.literal("[SpookyTime] Event active!"), false);
        nextEvent = System.currentTimeMillis() + (long) interval.getValue() * 60000L;
    }

    private void apply() {
        boolean strict = profile.isMode("Safe");
        SilkClient.INSTANCE.getModuleManager().getModule(KillAura.class)
                .ifPresent(m -> m.applyServerBypass(maxCps.getValue(), rotation.getValue(), strict));
        SilkClient.INSTANCE.getModuleManager().getModule(AimAssist.class)
                .ifPresent(m -> m.applyServerBypass(strict));
        SilkClient.INSTANCE.getModuleManager().getModule(TriggerBot.class)
                .ifPresent(m -> m.applyServerBypass(1000.0 / Math.max(1, maxCps.getValue()), strict));
    }

    public long getRemaining() {
        return Math.max(0, nextEvent - System.currentTimeMillis());
    }
}
