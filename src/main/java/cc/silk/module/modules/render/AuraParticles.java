package cc.silk.module.modules.render;

import cc.silk.event.impl.player.TickEvent;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.module.setting.ModeSetting;
import cc.silk.module.setting.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;

/** Visual module: orbiting ring of particles around the player. */
public final class AuraParticles extends Module {

    private final ModeSetting particle = new ModeSetting("Particle", "Totem", "Totem", "Crit", "Damage");
    private final NumberSetting count = new NumberSetting("Count", 4, 40, 16, 1);
    private final NumberSetting radius = new NumberSetting("Radius", 1, 8, 2, 0.5);
    private final NumberSetting speed = new NumberSetting("Speed", 1, 20, 6, 1);

    private float angle;

    public AuraParticles() {
        super("Aura Particles", "Draws an orbiting particle ring around you", Category.PARTICLES);
        addSettings(particle, count, radius, speed);
    }

    @EventHandler
    private void onTick(TickEvent event) {
        if (isNull() || mc.world == null) return;
        angle += speed.getValueFloat() * 0.02f;
        int n = count.getValueInt();
        ParticleEffect effect = switch (particle.getMode()) {
            case "Crit" -> ParticleTypes.CRIT;
            case "Damage" -> ParticleTypes.DAMAGE_INDICATOR;
            default -> ParticleTypes.TOTEM_OF_UNDYING;
        };
        for (int i = 0; i < n; i++) {
            double a = angle + (Math.PI * 2.0 / n) * i;
            double x = mc.player.getX() + Math.cos(a) * radius.getValue();
            double z = mc.player.getZ() + Math.sin(a) * radius.getValue();
            mc.world.addParticle(effect, x, mc.player.getY() + 0.8, z, 0, 0, 0);
        }
    }
}
