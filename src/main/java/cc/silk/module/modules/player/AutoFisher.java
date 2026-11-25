package cc.silk.module.modules.player;

import cc.silk.event.impl.player.TickEvent;
import cc.silk.mixin.MinecraftClientAccessor;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.module.setting.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.FishingRodItem;
import net.minecraft.util.math.Box;

import java.util.List;

/** Auto fishing: casts, detects a bite and reels in. */
public final class AutoFisher extends Module {

    private final NumberSetting castDelay = new NumberSetting("Cast Delay (MS)", 200, 5000, 900, 50);
    private final NumberSetting biteTimeout = new NumberSetting("Reel Delay (S)", 1, 30, 8, 1);

    private long lastCast;
    private long castTime;

    public AutoFisher() {
        super("Auto Fisher", "Automatically casts and reels the fishing rod", Category.FARM);
        addSettings(castDelay, biteTimeout);
    }

    @EventHandler
    private void onTick(TickEvent event) {
        if (isNull() || mc.player.getMainHandStack().isEmpty()) return;
        if (!(mc.player.getMainHandStack().getItem() instanceof FishingRodItem)) return;

        List<FishingBobberEntity> bobbers = mc.world.getEntitiesByClass(FishingBobberEntity.class,
                new Box(mc.player.getBlockPos()).expand(40),
                bobber -> bobber.getPlayerOwner() == mc.player);
        FishingBobberEntity bobber = bobbers.isEmpty() ? null : bobbers.get(0);

        if (bobber == null) {
            if (System.currentTimeMillis() - lastCast < castDelay.getValue()) return;
            use();
            lastCast = System.currentTimeMillis();
            castTime = lastCast;
            return;
        }

        boolean bitten = bobber.getHookedEntity() != null;
        long inWater = System.currentTimeMillis() - castTime;
        if (bitten || inWater > biteTimeout.getValue() * 1000L) {
            use();
            lastCast = System.currentTimeMillis();
            castTime = lastCast;
        }
    }

    private void use() {
        ((MinecraftClientAccessor) mc).setItemUseCooldown(0);
        ((MinecraftClientAccessor) mc).invokeDoItemUse();
    }
}
