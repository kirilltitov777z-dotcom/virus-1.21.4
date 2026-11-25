package cc.silk.module.modules.misc;

import cc.silk.event.impl.player.TickEvent;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.module.setting.BooleanSetting;
import cc.silk.module.setting.NumberSetting;
import cc.silk.utils.notification.Notification;
import cc.silk.utils.notification.NotificationManager;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.text.Text;

/** Countdown and alert for the Spooky Time (Halloween) event. */
public final class SpookyTime extends Module {

    private final NumberSetting interval = new NumberSetting("Interval (min)", 2, 180, 15, 1);
    private final NumberSetting duration = new NumberSetting("Duration (s)", 5, 600, 90, 1);
    private final BooleanSetting notify = new BooleanSetting("Notify", true);
    private final BooleanSetting chat = new BooleanSetting("Chat Message", false);

    private long nextEvent;

    public SpookyTime() {
        super("Spooky Time", "Announces the Spooky Time event and keeps a countdown", Category.MISC);
        addSettings(interval, duration, notify, chat);
    }

    @Override
    public void onEnable() {
        nextEvent = System.currentTimeMillis() + (long) interval.getValue() * 60000L;
    }

    @EventHandler
    private void onTick(TickEvent event) {
        if (isNull()) return;
        if (System.currentTimeMillis() < nextEvent) return;

        long activeUntil = System.currentTimeMillis() + (long) duration.getValue() * 1000L;
        if (notify.getValue()) {
            NotificationManager.getInstance().getNotifications().add(new Notification(
                    "Spooky Time", "Event active for " + (int) duration.getValue() + "s",
                    Notification.NotificationType.MODULE_ENABLED, 3000));
        }
        if (chat.getValue()) {
            mc.player.sendMessage(Text.literal("[Spooky Time] Event active!"), false);
        }
        nextEvent = activeUntil + (long) interval.getValue() * 60000L;
    }

    public long getRemaining() {
        return Math.max(0, nextEvent - System.currentTimeMillis());
    }
}
