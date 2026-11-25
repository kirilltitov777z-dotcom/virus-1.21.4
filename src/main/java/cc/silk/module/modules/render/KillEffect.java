package cc.silk.module.modules.render;

import cc.silk.event.impl.network.PacketEvent;
import cc.silk.event.impl.player.TickEvent;
import cc.silk.event.types.TransferOrder;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.module.setting.ModeSetting;
import cc.silk.module.setting.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Kill animations: explosion, "shahed" drone strike and vanish. */
public final class KillEffect extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Shahed", "None", "Explosion", "Shahed", "Vanish");
    private final NumberSetting particles = new NumberSetting("Particles", 8, 80, 28, 2);
    private final NumberSetting radius = new NumberSetting("Radius", 0.5, 6.0, 2.0, 0.25);

    private final List<Strike> strikes = new ArrayList<>();

    public KillEffect() {
        super("Kill Effect", "Animations on kill: explosion, drone strike or vanish", Category.VISUAL);
        addSettings(mode, particles, radius);
    }

    @EventHandler
    private void onPacket(PacketEvent event) {
        if (isNull() || event.getOrder() != TransferOrder.RECEIVE) return;
        if (mode.isMode("None")) return;
        if (!(event.getPacket() instanceof EntityStatusS2CPacket packet)) return;
        if (packet.getStatus() != 35) return;

        Entity entity = packet.getEntity(mc.world);
        if (entity == null) return;
        strikes.add(new Strike(entity.getX(), entity.getY(), entity.getZ(), 14));
    }

    @EventHandler
    private void onTick(TickEvent event) {
        if (isNull() || mc.world == null) return;
        Iterator<Strike> iterator = strikes.iterator();
        while (iterator.hasNext()) {
            Strike strike = iterator.next();
            strike.ticks--;
            if (strike.ticks <= 0) {
                burst(strike);
                iterator.remove();
                continue;
            }
            if (mode.isMode("Shahed")) {
                double progress = 1.0 - strike.ticks / 14.0;
                double y = strike.y + 18.0 * (1.0 - progress);
                for (int i = 0; i < 3; i++) {
                    mc.world.addParticle(ParticleTypes.TOTEM_OF_UNDYING,
                            strike.x + (Math.random() - 0.5) * 0.4, y,
                            strike.z + (Math.random() - 0.5) * 0.4, 0, -0.4, 0);
                }
            } else if (mode.isMode("Vanish")) {
                double a = strike.ticks * 0.5;
                mc.world.addParticle(ParticleTypes.DAMAGE_INDICATOR,
                        strike.x + Math.cos(a) * radius.getValue() * 0.4,
                        strike.y + 0.8,
                        strike.z + Math.sin(a) * radius.getValue() * 0.4, 0, 0.02, 0);
            }
        }
    }

    private void burst(Strike strike) {
        int count = particles.getValueInt();
        double r = radius.getValue();
        for (int i = 0; i < count; i++) {
            double angle = Math.random() * Math.PI * 2;
            double speed = 0.05 + Math.random() * 0.25;
            mc.world.addParticle(ParticleTypes.CRIT,
                    strike.x + Math.cos(angle) * r * Math.random(),
                    strike.y + 0.5 + Math.random() * r,
                    strike.z + Math.sin(angle) * r * Math.random(),
                    Math.cos(angle) * speed, 0.1, Math.sin(angle) * speed);
        }
        if (mode.isMode("Shahed")) {
            for (int i = 0; i < count / 2; i++) {
                double angle = Math.random() * Math.PI * 2;
                mc.world.addParticle(ParticleTypes.TOTEM_OF_UNDYING,
                        strike.x, strike.y + 0.5, strike.z,
                        Math.cos(angle) * 0.3, 0.25, Math.sin(angle) * 0.3);
            }
        }
    }

    @Override
    public void onDisable() {
        strikes.clear();
    }

    private static final class Strike {
        final double x, y, z;
        int ticks;

        Strike(double x, double y, double z, int ticks) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.ticks = ticks;
        }
    }
}
