package cc.silk.module.modules.combat;

import cc.silk.event.impl.player.TickEvent;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.module.setting.BooleanSetting;
import cc.silk.module.setting.ModeSetting;
import cc.silk.module.setting.NumberSetting;
import cc.silk.module.modules.misc.Teams;
import cc.silk.utils.friend.FriendManager;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * KillAura ported from VenusFree's combat logic onto Fabric 1.21.4.
 * Targets are filtered and sorted before rotation, rotation is GCD aligned,
 * and attacks are validated with a fresh ray trace instead of the vanilla
 * crosshair target.
 */
public final class KillAura extends Module {

    private final ModeSetting mode = new ModeSetting("Type", "Smooth", "Smooth", "Instant");
    private final ModeSetting priority = new ModeSetting("Priority", "Distance", "Distance", "Health", "FOV");
    private final NumberSetting range = new NumberSetting("Attack Range", 3.0, 6.0, 3.3, 0.1);
    private final NumberSetting aimRange = new NumberSetting("Aim Range", 3.0, 40.0, 15.0, 0.5);
    private final NumberSetting aps = new NumberSetting("Attacks per second", 1.0, 20.0, 10.0, 0.5);
    private final NumberSetting rotationSpeed = new NumberSetting("Rotation Speed", 1.0, 180.0, 70.0, 1.0);
    private final NumberSetting fov = new NumberSetting("FOV", 10.0, 180.0, 120.0, 1.0);

    private final BooleanSetting targetPlayers = new BooleanSetting("Players", true);
    private final BooleanSetting targetMobs = new BooleanSetting("Mobs", false);
    private final BooleanSetting targetAnimals = new BooleanSetting("Animals", false);
    private final BooleanSetting targetInvisible = new BooleanSetting("Invisible", true);
    private final BooleanSetting throughWalls = new BooleanSetting("Through Walls", false);
    private final BooleanSetting onlyCrits = new BooleanSetting("Only Crits", false);
    private final BooleanSetting focusTarget = new BooleanSetting("Focus Target", true);
    private final BooleanSetting breakShield = new BooleanSetting("Break Shield", true);
    private final BooleanSetting movementCorrection = new BooleanSetting("Movement Correction", false);

    private LivingEntity target;
    private long lastAttack;

    public KillAura() {
        super("Kill Aura", "Automatically tracks and attacks nearby targets", Category.COMBAT);
        addSettings(mode, priority, range, aimRange, aps, rotationSpeed, fov, targetPlayers, targetMobs,
                targetAnimals, targetInvisible, throughWalls, onlyCrits, focusTarget, breakShield,
                movementCorrection);
    }

    @EventHandler
    private void onTick(TickEvent event) {
        if (isNull() || mc.currentScreen != null) return;

        if (target == null || !isValid(target) || !focusTarget.getValue()) {
            target = findTarget();
        }
        if (target == null) return;

        updateRotation();
        tryAttack();
    }

    private LivingEntity findTarget() {
        List<LivingEntity> candidates = new ArrayList<>();
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof LivingEntity living && isValid(living)) {
                candidates.add(living);
            }
        }
        if (candidates.isEmpty()) return null;
        if (candidates.size() == 1) return candidates.get(0);

        String p = priority.getMode();
        candidates.sort(Comparator
                .comparingDouble((LivingEntity e) -> mc.player.distanceTo(e))
                .thenComparingDouble(e -> e.getHealth())
                .thenComparingDouble((LivingEntity e) -> angleTo(e)));

        if ("Health".equals(p)) {
            candidates.sort(Comparator.comparingDouble(LivingEntity::getHealth));
        } else if ("FOV".equals(p)) {
            candidates.sort(Comparator.comparingDouble(this::angleTo));
        }
        return candidates.get(0);
    }

    private boolean isValid(LivingEntity entity) {
        if (entity == null || entity == mc.player || entity == mc.cameraEntity) return false;
        if (!entity.isAlive() || entity.isDead()) return false;
        if (mc.player.distanceTo(entity) > aimRange.getValue()) return false;
        if (!targetInvisible.getValue() && entity.isInvisible()) return false;
        if (!throughWalls.getValue() && !mc.player.canSee(entity)) return false;
        if (entity instanceof PlayerEntity player) {
            if (!targetPlayers.getValue()) return false;
            if (FriendManager.isFriend(player.getUuid())) return false;
            if (Teams.isTeammate(entity)) return false;
        } else if (entity instanceof HostileEntity) {
            if (!targetMobs.getValue()) return false;
        } else if (entity instanceof AnimalEntity) {
            if (!targetAnimals.getValue()) return false;
        } else {
            return false;
        }
        return angleTo(entity) <= fov.getValue() / 2.0;
    }

    private double angleTo(LivingEntity entity) {
        float[] rotation = rotationTo(entity);
        float yawDiff = MathHelper.wrapDegrees(rotation[0] - mc.player.getYaw());
        float pitchDiff = rotation[1] - mc.player.getPitch();
        return Math.hypot(yawDiff, pitchDiff);
    }

    private float[] rotationTo(LivingEntity entity) {
        Vec3d target = new Vec3d(entity.getX(), entity.getY() + entity.getHeight() * 0.5, entity.getZ());
        Vec3d diff = target.subtract(mc.player.getEyePos());
        double horizontal = Math.hypot(diff.x, diff.z);
        float yaw = (float) Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0f;
        float pitch = (float) -Math.toDegrees(Math.atan2(diff.y, horizontal));
        return new float[] { MathHelper.wrapDegrees(yaw), MathHelper.clamp(pitch, -89.0f, 89.0f) };
    }

    private void updateRotation() {
        float[] desired = rotationTo(target);
        float currentYaw = mc.player.getYaw();
        float currentPitch = mc.player.getPitch();
        float yawDiff = MathHelper.wrapDegrees(desired[0] - currentYaw);
        float pitchDiff = desired[1] - currentPitch;

        float step = rotationSpeed.getValueFloat();
        float newYaw;
        float newPitch;
        if (mode.isMode("Instant")) {
            newYaw = currentYaw + yawDiff;
            newPitch = currentPitch + pitchDiff;
        } else {
            newYaw = currentYaw + MathHelper.clamp(yawDiff, -step, step);
            newPitch = currentPitch + MathHelper.clamp(pitchDiff, -step, step);
        }
        newYaw = MathHelper.wrapDegrees(newYaw);
        newPitch = MathHelper.clamp(newPitch, -89.0f, 89.0f);

        float gcd = (float) (mc.options.getMouseSensitivity().getValue() * 0.6 / 100.0);
        if (gcd > 0.0001f && Math.abs(newYaw - currentYaw) > 0.0001f) {
            newYaw -= (newYaw - currentYaw) % gcd;
            newPitch -= (newPitch - currentPitch) % gcd;
        }

        mc.player.setYaw(newYaw);
        mc.player.setPitch(newPitch);
        if (movementCorrection.getValue()) {
            mc.player.setBodyYaw(newYaw);
        }
    }

    private void tryAttack() {
        if (onlyCrits.getValue() && (mc.player.isOnGround() || mc.player.fallDistance <= 0.0f)) return;

        double distance = mc.player.distanceTo(target);
        if (distance > range.getValue()) return;

        HitResult hit = mc.player.raycast(range.getValue() + 0.5, 1.0f, false);
        boolean visible = hit instanceof EntityHitResult entityHit && entityHit.getEntity() == target;
        if (!visible && !throughWalls.getValue()) return;

        if (breakShield.getValue() && target instanceof PlayerEntity player && player.isBlocking()) {
            // The vanilla attack packet already disables a shield when hit with an axe.
        }

        long now = System.currentTimeMillis();
        long interval = (long) (1000.0 / aps.getValue());
        if (now - lastAttack < interval) return;
        if (mc.player.getAttackCooldownProgress(1.0f) < 0.85f) return;

        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
        lastAttack = now;
    }

    /** Called by server bypass profiles to keep the module inside safe limits. */
    public void applyServerBypass(double maxAps, double maxRotation, boolean strict) {
        aps.setValue(Math.min(aps.getValue(), maxAps));
        rotationSpeed.setValue(Math.min(rotationSpeed.getValue(), maxRotation));
        if (strict) {
            throughWalls.setValue(false);
            onlyCrits.setValue(false);
        }
    }

    @Override
    public void onDisable() {
        target = null;
    }
}
