package cc.silk.module.modules.render;

import cc.silk.event.impl.player.TickEvent;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.module.setting.ModeSetting;
import cc.silk.module.setting.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.particle.ParticleTypes;

/** Visual module: spawns a soft particle field around the local player. */
public final class ParticleRain extends Module {

    private final ModeSetting particle = new ModeSetting("Particle", "Crit", "Crit", "Damage", "Totem");
    private final NumberSetting amount = new NumberSetting("Amount", 1, 30, 8, 1);
    private final NumberSetting radius = new NumberSetting("Radius", 1, 30, 8, 1);
    private final NumberSetting height = new NumberSetting("Height", 0, 20, 3, 1);

    public ParticleRain() {
        super("Particle Rain", "Spawns decorative particles around you", Category.PARTICLES);
        addSettings(particle, amount, radius, height);
    }

    @EventHandler
    private void onTick(TickEvent event) {
        if (isNull() || mc.world == null) return;
        int count = amount.getValueInt();
        double r = radius.getValue();
        double h = height.getValue();
        for (int i = 0; i < count; i++) {
            double x = mc.player.getX() + (Math.random() - 0.5) * r;
            double y = mc.player.getY() + Math.random() * h;
            double z = mc.player.getZ() + (Math.random() - 0.5) * r;
            mc.world.addParticle(effect(), x, y, z, 0, -0.02, 0);
        }
    }

    private net.minecraft.particle.ParticleEffect effect() {
        return switch (particle.getMode()) {
            case "Damage" -> ParticleTypes.DAMAGE_INDICATOR;
            case "Totem" -> ParticleTypes.TOTEM_OF_UNDYING;
            default -> ParticleTypes.CRIT;
        };
    }
}
