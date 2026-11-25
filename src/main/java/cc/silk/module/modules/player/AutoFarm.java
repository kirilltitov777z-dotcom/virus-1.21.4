package cc.silk.module.modules.player;

import cc.silk.event.impl.player.TickEvent;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.module.setting.BooleanSetting;
import cc.silk.module.setting.ModeSetting;
import cc.silk.module.setting.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/** Farming module: breaks nearby crops/blocks and optionally replants them. */
public final class AutoFarm extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Crops", "Crops", "All");
    private final NumberSetting range = new NumberSetting("Range", 1.0, 6.0, 4.0, 0.5);
    private final NumberSetting delay = new NumberSetting("Delay (MS)", 50, 1000, 220, 10);
    private final BooleanSetting replant = new BooleanSetting("Replant", true);

    private long lastAction;

    public AutoFarm() {
        super("Auto Farm", "Automatically harvests crops and replants seeds", Category.FARM);
        addSettings(mode, delay, range, replant);
    }

    @EventHandler
    private void onTick(TickEvent event) {
        if (isNull() || mc.interactionManager == null) return;
        if (System.currentTimeMillis() - lastAction < delay.getValue()) return;

        int r = (int) Math.ceil(range.getValue());
        BlockPos origin = mc.player.getBlockPos();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos pos = origin.add(dx, dy, dz);
                    if (dx * dx + dy * dy + dz * dz > range.getValue() * range.getValue()) continue;
                    BlockState state = mc.world.getBlockState(pos);
                    if (state.isAir()) continue;
                    if (mode.isMode("Crops") && !(state.getBlock() instanceof CropBlock crop && crop.isMature(state))) {
                        continue;
                    }
                    mc.interactionManager.attackBlock(pos, Direction.UP);
                    mc.player.swingHand(Hand.MAIN_HAND);
                    if (replant.getValue()) {
                        Vec3d hitVec = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
                                new BlockHitResult(hitVec, Direction.UP, pos, false));
                    }
                    lastAction = System.currentTimeMillis();
                    return;
                }
            }
        }
    }
}
