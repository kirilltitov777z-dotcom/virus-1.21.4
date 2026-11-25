package cc.silk.module.modules.render;

import cc.silk.event.impl.player.TickEvent;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.module.setting.NumberSetting;
import meteordevelopment.orbit.EventHandler;

/** Visual module: overrides the client field of view. */
public final class CustomFov extends Module {

    private final NumberSetting fov = new NumberSetting("FOV", 30.0, 180.0, 90.0, 1.0);
    private int original = -1;

    public CustomFov() {
        super("Custom FOV", "Changes your field of view and restores it on disable", Category.VISUAL);
        addSettings(fov);
    }

    @Override
    public void onEnable() {
        if (original < 0) original = mc.options.getFov().getValue();
    }

    @EventHandler
    private void onTick(TickEvent event) {
        if (isNull()) return;
        mc.options.getFov().setValue((int) fov.getValue());
    }

    @Override
    public void onDisable() {
        if (original >= 0) mc.options.getFov().setValue(original);
        original = -1;
    }
}
