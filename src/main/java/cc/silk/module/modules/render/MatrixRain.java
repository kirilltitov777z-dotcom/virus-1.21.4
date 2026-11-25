package cc.silk.module.modules.render;

import cc.silk.event.impl.player.TickEvent;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.module.setting.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.particle.ParticleTypes;

/** Visual module: falling digital-rain style particle columns. */
public final class MatrixRain extends Module {

    private final NumberSetting columns = new NumberSetting("Columns", 2, 40, 14, 1);
    private final NumberSetting speed = new NumberSetting("Speed", 1, 20, 8, 1);
    private final NumberSetting radius = new NumberSetting("Radius", 2, 30, 12, 1);
    private final NumberSetting height = new NumberSetting("Height", 2, 30, 12, 1);

    private float tick;

    public MatrixRain() {
        super("Matrix Rain", "Matrix-style falling particle columns", Category.PARTICLES);
        addSettings(columns, speed, radius, height);
    }

    @EventHandler
    private void onTick(TickEvent event) {
        if (isNull() || mc.world == null) return;
        tick += speed.getValueFloat() * 0.02f;
        int n = columns.getValueInt();
        for (int i = 0; i < n; i++) {
            double angle = (Math.PI * 2.0 / n) * i;
            double x = mc.player.getX() + Math.cos(angle) * radius.getValue();
            double z = mc.player.getZ() + Math.sin(angle) * radius.getValue();
            double fall = (tick + i * 0.7) % height.getValue();
            mc.world.addParticle(ParticleTypes.END_ROD, x,
                    mc.player.getY() + height.getValue() - fall, z, 0, -0.05, 0);
        }
    }
}
