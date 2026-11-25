package cc.silk.module.modules.render;

import cc.silk.event.impl.player.TickEvent;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.module.setting.ModeSetting;
import cc.silk.module.setting.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;

/** Visual module: orbiting shapes made of particles. */
public final class AuraParticles extends Module {

    private final ModeSetting shape = new ModeSetting("Shape", "Heart", "Heart", "Star", "Dollar", "Ring");
    private final ModeSetting color = new ModeSetting("Color", "Cyan", "Red", "Gold", "Purple", "White", "Cyan");
    private final NumberSetting count = new NumberSetting("Count", 4, 60, 20, 1);
    private final NumberSetting radius = new NumberSetting("Radius", 1, 8, 2, 0.5);
    private final NumberSetting speed = new NumberSetting("Speed", 1, 20, 6, 1);

    private float angle;

    public AuraParticles() {
        super("Aura Particles", "Orbiting heart, star, dollar or ring shaped particles", Category.PARTICLES);
        addSettings(shape, color, count, radius, speed);
    }

    @EventHandler
    private void onTick(TickEvent event) {
        if (isNull() || mc.world == null) return;
        angle += speed.getValueFloat() * 0.02f;
        int n = count.getValueInt();
        double r = radius.getValue();
        ParticleEffect effect = switch (color.getMode()) {
            case "Red" -> ParticleTypes.DAMAGE_INDICATOR;
            case "Gold" -> ParticleTypes.TOTEM_OF_UNDYING;
            case "Purple" -> ParticleTypes.PORTAL;
            case "White" -> ParticleTypes.END_ROD;
            default -> ParticleTypes.CRIT;
        };
        for (int i = 0; i < n; i++) {
            double a = angle + (Math.PI * 2.0 / n) * i;
            double x = Math.cos(a);
            double z = Math.sin(a);
            double py = 0.8;
            switch (shape.getMode()) {
                case "Heart" -> {
                    double s = Math.pow(Math.sin(a), 3);
                    x = s;
                    py = 1.0 + (13 * Math.cos(a) - 5 * Math.cos(2 * a) - 2 * Math.cos(3 * a)
                            - Math.cos(4 * a)) / 16.0;
                    z = 0;
                }
                case "Star" -> {
                    double k = i % 2 == 0 ? 1.0 : 0.45;
                    x *= k;
                    z *= k;
                }
                case "Dollar" -> {
                    x = Math.sin(a * 0.5) * 0.8;
                    z = Math.cos(a * 0.5) * 0.2;
                    py = (i / (double) n) * 1.6;
                }
                default -> {
                }
            }
            mc.world.addParticle(effect,
                    mc.player.getX() + x * r, mc.player.getY() + py, mc.player.getZ() + z * r, 0, 0, 0);
        }
    }
}
