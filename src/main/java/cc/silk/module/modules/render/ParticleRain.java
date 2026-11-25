package cc.silk.module.modules.render;

import cc.silk.event.impl.player.TickEvent;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.module.setting.ModeSetting;
import cc.silk.module.setting.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;

/** Visual module with particle shapes and colour presets. */
public final class ParticleRain extends Module {

    private final ModeSetting shape = new ModeSetting("Shape", "Rain", "Rain", "Heart", "Star", "Dollar", "Spiral");
    private final ModeSetting color = new ModeSetting("Color", "Gold", "Red", "Gold", "Purple", "White", "Cyan");
    private final NumberSetting amount = new NumberSetting("Amount", 1, 40, 10, 1);
    private final NumberSetting radius = new NumberSetting("Radius", 1, 30, 8, 1);
    private final NumberSetting height = new NumberSetting("Height", 0, 20, 3, 1);

    private float phase;

    public ParticleRain() {
        super("Particle Rain", "Decorative particle field: rain, heart, star, dollar or spiral", Category.PARTICLES);
        addSettings(shape, color, amount, radius, height);
    }

    @EventHandler
    private void onTick(TickEvent event) {
        if (isNull() || mc.world == null) return;
        phase += 0.04f;
        int count = amount.getValueInt();
        double r = radius.getValue();
        double h = height.getValue();
        ParticleEffect effect = effect();
        for (int i = 0; i < count; i++) {
            double[] p = point(shape.getMode(), i, count, r, h);
            mc.world.addParticle(effect,
                    mc.player.getX() + p[0], mc.player.getY() + p[1], mc.player.getZ() + p[2], 0, 0, 0);
        }
    }

    private double[] point(String type, int index, int count, double r, double h) {
        double t = (phase + (Math.PI * 2.0 * index) / Math.max(1, count));
        return switch (type) {
            case "Heart" -> new double[] {
                    Math.pow(Math.sin(t), 3) * r,
                    (13 * Math.cos(t) - 5 * Math.cos(2 * t) - 2 * Math.cos(3 * t) - Math.cos(4 * t)) / 16.0 * r + h,
                    0
            };
            case "Star" -> new double[] {
                    Math.cos(t) * r * (index % 2 == 0 ? 1 : 0.45),
                    Math.sin(t) * r * (index % 2 == 0 ? 1 : 0.45) + h * 0.5,
                    0
            };
            case "Dollar" -> new double[] {
                    Math.sin(t * 0.5) * r * 0.6,
                    (index / (double) Math.max(1, count)) * h - h * 0.5,
                    Math.cos(t * 0.5) * r * 0.15
            };
            case "Spiral" -> new double[] {
                    Math.cos(t + index * 0.3) * r * (index / (double) Math.max(1, count)),
                    (index / (double) Math.max(1, count)) * h,
                    Math.sin(t + index * 0.3) * r * (index / (double) Math.max(1, count))
            };
            default -> new double[] {
                    (Math.random() - 0.5) * r, Math.random() * h, (Math.random() - 0.5) * r
            };
        };
    }

    private ParticleEffect effect() {
        return switch (color.getMode()) {
            case "Red" -> ParticleTypes.DAMAGE_INDICATOR;
            case "Purple" -> ParticleTypes.PORTAL;
            case "White" -> ParticleTypes.END_ROD;
            case "Cyan" -> ParticleTypes.CRIT;
            default -> ParticleTypes.TOTEM_OF_UNDYING;
        };
    }
}
