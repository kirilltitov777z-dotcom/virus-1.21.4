package cc.silk.module.modules.combat;

import cc.silk.event.impl.render.Render3DEvent;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.module.modules.misc.Teams;
import cc.silk.module.setting.BooleanSetting;
import cc.silk.module.setting.ModeSetting;
import cc.silk.module.setting.NumberSetting;
import cc.silk.utils.friend.FriendManager;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.SwordItem;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AimAssist extends Module {

    private final NumberSetting maxYawSpeed = new NumberSetting("Max Yaw Speed", 0.1, 5.0, 2.0, 0.1);
    private final NumberSetting minYawSpeed = new NumberSetting("Min Yaw Speed", 0.1, 5.0, 2.0, 0.1);

    private final NumberSetting minPitchSpeed = new NumberSetting("Min Pitch Speed", 0.1, 5.0, 2.0, 0.1);
    private final NumberSetting maxPitchSpeed = new NumberSetting("Max Pitch Speed", 0.1, 5.0, 2.0, 0.1);

    private final NumberSetting fov = new NumberSetting("FOV", 10.0, 180.0, 90.0, 1.0);
    private final NumberSetting range = new NumberSetting("Range", 1.0, 10.0, 4.5, 0.1);
    private final NumberSetting smoothing = new NumberSetting("Smoothing", 1.0, 20.0, 10.0, 0.5);
    private final NumberSetting pitchThreshold = new NumberSetting("Pitch Threshold", 0.0, 90.0, 60.0, 1.0);

    private final BooleanSetting targetPlayers = new BooleanSetting("Target Players", true);
    private final BooleanSetting targetMobs = new BooleanSetting("Target Mobs", false);
    private final BooleanSetting weaponsOnly = new BooleanSetting("Weapons Only", false);
    private final BooleanSetting throughWalls = new BooleanSetting("Through Walls", false);
    private final BooleanSetting disableOnTarget = new BooleanSetting("Disable on target", false);
    private final BooleanSetting ignoreBlocks = new BooleanSetting("Ignore Blocks", true);
    private final BooleanSetting targetLock = new BooleanSetting("Target Lock", true);
    private final BooleanSetting gcd = new BooleanSetting("GCD", true);
    private final ModeSetting priority = new ModeSetting("Priority", "Distance", "Distance", "Health");

    private final BooleanSetting antiDetect = new BooleanSetting("Anti-Detect", true);
    private final BooleanSetting onlyGround = new BooleanSetting("Only on Ground", false);
    private final BooleanSetting resetOnDeath = new BooleanSetting("Reset on Death", true);
    private final NumberSetting switchDelay = new NumberSetting("Switch Delay (MS)", 0.0, 500.0, 80.0, 10.0);

    private Entity currentTarget = null;
    private long lastSwitch;
    private long lastUpdateTime = 0;
    private float currentBaseSpeed = 10f;
    private float nextBaseSpeed = 10f;
    private long lastSpeedChangeTime = 0;

    public AimAssist() {
        super("Aim Assist", "Gives you assistance on your aim", Category.COMBAT);
        addSettings(
                maxYawSpeed, minYawSpeed, maxPitchSpeed, minPitchSpeed, fov, range, smoothing, pitchThreshold,
                targetPlayers, targetMobs, weaponsOnly, throughWalls, disableOnTarget, ignoreBlocks,
                targetLock, gcd, priority, antiDetect, onlyGround, resetOnDeath, switchDelay);
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (isNull())
            return;
        if (maxPitchSpeed.getValueFloat() <= minPitchSpeed.getValueFloat()) {
            maxPitchSpeed.setValue(minPitchSpeed.getValue() + 0.1);
        }
        if (maxYawSpeed.getValueFloat() <= minYawSpeed.getValueFloat()) {
            maxYawSpeed.setValue(minYawSpeed.getValue() + 0.1);
        }
        if (weaponsOnly.getValue() && !isHoldingWeapon())
            return;

        if (mc.currentScreen != null)
            return;

        if (onlyGround.getValue() && !mc.player.isOnGround())
            return;

        if (resetOnDeath.getValue() && !mc.player.isAlive()) {
            currentTarget = null;
            return;
        }

        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.BLOCK
                && mc.options.attackKey.isPressed()) {
            return;
        }

        if (mc.player.getPitch() > pitchThreshold.getValueFloat()) {
            return;
        }

        if (ignoreBlocks.getValue() && mc.crosshairTarget != null
                && mc.crosshairTarget.getType() == HitResult.Type.BLOCK) {
            return;
        }

        Entity found = findBestTarget();
        if (antiDetect.getValue() && found != currentTarget) {
            if (System.currentTimeMillis() - lastSwitch < switchDelay.getValue()) {
                return;
            }
            lastSwitch = System.currentTimeMillis();
        }
        currentTarget = found;

        if (mc.targetedEntity == currentTarget && disableOnTarget.getValue()) {
            return;
        }

        if (currentTarget != null) {
            if (!throughWalls.getValue() && !mc.player.canSee(currentTarget))
                return;

            Vec3d chestPos = getChestPosition(currentTarget);
            float[] rotation = calculateRotation(chestPos);
            applySmoothAiming(rotation[0], rotation[1]);
        }
    }

    private Entity findBestTarget() {
        if (isNull())
            return null;

        if (targetLock.getValue() && currentTarget != null && isValidTarget(currentTarget)
                && mc.player.distanceTo(currentTarget) <= range.getValue()) {
            return currentTarget;
        }

        List<Entity> candidates = new ArrayList<>();
        for (Entity entity : mc.world.getEntities()) {
            if (!isValidTarget(entity))
                continue;
            if (mc.player.distanceTo(entity) > range.getValue())
                continue;

            float[] rotation = calculateRotation(getChestPosition(entity));
            if (getFOVDistance(rotation[0], rotation[1]) <= fov.getValue() / 2.0) {
                candidates.add(entity);
            }
        }
        if (candidates.isEmpty())
            return null;

        if (priority.isMode("Health")) {
            candidates.sort(Comparator
                    .comparingDouble((Entity e) -> ((LivingEntity) e).getHealth())
                    .thenComparingDouble(e -> mc.player.distanceTo(e)));
        } else {
            candidates.sort(Comparator
                    .comparingDouble((Entity e) -> mc.player.distanceTo(e))
                    .thenComparingDouble(e -> {
                        float[] r = calculateRotation(getChestPosition(e));
                        return getFOVDistance(r[0], r[1]);
                    }));
        }
        return candidates.get(0);
    }

    private boolean isValidTarget(Entity entity) {
        if (entity == null || entity == mc.player || !(entity instanceof LivingEntity livingEntity))
            return false;
        if (!livingEntity.isAlive() || livingEntity.isDead())
            return false;
        if (Teams.isTeammate(entity))
            return false;
        if (entity instanceof PlayerEntity player && FriendManager.isFriend(player.getUuid()))
            return false;

        return entity instanceof PlayerEntity ? targetPlayers.getValue() : targetMobs.getValue();
    }

    private Vec3d getChestPosition(Entity entity) {
        return new Vec3d(entity.getX(), entity.getEyeY(), entity.getZ());
    }

    private float[] calculateRotation(Vec3d target) {
        Vec3d diff = target.subtract(mc.player.getEyePos());
        double distance = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        float yaw = (float) Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0f;
        float pitch = (float) -Math.toDegrees(Math.atan2(diff.y, distance));
        return new float[] { MathHelper.wrapDegrees(yaw), MathHelper.clamp(pitch, -89.0f, 89.0f) };
    }

    private double getFOVDistance(float targetYaw, float targetPitch) {
        float yawDiff = MathHelper.wrapDegrees(targetYaw - mc.player.getYaw());
        float pitchDiff = targetPitch - mc.player.getPitch();
        return Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);
    }

    private void applySmoothAiming(float targetYaw, float targetPitch) {
        long currentTime = System.currentTimeMillis();

        if (lastUpdateTime == 0) {
            lastUpdateTime = currentTime;
            return;
        }

        float deltaTime = (currentTime - lastUpdateTime) / 1000.0f;
        lastUpdateTime = currentTime;

        if (deltaTime < 0.001f || deltaTime > 0.1f)
            return;
        deltaTime *= randomFloat(0.9f, 1.1f);

        updateBaseSpeed();

        float currentYaw = mc.player.getYaw();
        float currentPitch = mc.player.getPitch();

        float yawDiff = MathHelper.wrapDegrees(targetYaw - currentYaw);
        float pitchDiff = targetPitch - currentPitch;

        float distance = (float) Math.hypot(yawDiff, pitchDiff);
        if (distance < 0.3f)
            return;

        float t = Math.min(distance / 10f, 1f);
        float eased = easeOutCubic(t);

        float lerpFactor = eased * (smoothing.getValueFloat() / 10f) * deltaTime;
        float newYaw = MathHelper.lerp(lerpFactor, currentYaw, currentYaw + yawDiff);
        float newPitch = MathHelper.lerp(lerpFactor, currentPitch, currentPitch + pitchDiff);

        if (gcd.getValue()) {
            float step = (float) (mc.options.getMouseSensitivity().getValue() * 0.6 / 100.0);
            if (step > 0.0001f) {
                newYaw -= (newYaw - currentYaw) % step;
                newPitch -= (newPitch - currentPitch) % step;
            }
        }

        mc.player.setYaw(newYaw);
        mc.player.setPitch(MathHelper.clamp(newPitch, -89f, 89f));
    }

    private float easeOutCubic(float t) {
        return 1f - (float) Math.pow(1f - t, 3);
    }

    private void updateBaseSpeed() {
        long now = System.currentTimeMillis();
        if (now - lastSpeedChangeTime > 200) {
            lastSpeedChangeTime = now;
            float change = randomFloat(-5f, 5f);
            nextBaseSpeed += change;
            nextBaseSpeed = MathHelper.clamp(nextBaseSpeed, 8f, 100f);
        }

        currentBaseSpeed = (currentBaseSpeed * 0.9f) + (nextBaseSpeed * 0.1f);
    }

    private float randomFloat(float min, float max) {
        return min + (float) Math.random() * (max - min);
    }

    private boolean isHoldingWeapon() {
        if (mc.player == null)
            return false;
        if (mc.player.getMainHandStack().isEmpty())
            return false;
        Item heldItem = mc.player.getMainHandStack().getItem();
        return heldItem instanceof SwordItem || heldItem instanceof AxeItem;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        lastUpdateTime = System.currentTimeMillis();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        currentTarget = null;
    }
}
