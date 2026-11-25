package cc.silk.module.modules.player;

import cc.silk.event.impl.network.PacketEvent;
import cc.silk.event.impl.player.TickEvent;
import cc.silk.event.types.TransferOrder;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.module.setting.ModeSetting;
import cc.silk.module.setting.NumberSetting;
import cc.silk.module.setting.StringSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.ProfilelessChatMessageS2CPacket;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * FunTime auction helper. Watches chat for cheap offers and sends /ah commands.
 * Command templates are configurable because server syntax can change.
 */
public final class AutoAuction extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Snipe", "Off", "Snipe", "Sell", "Both");
    private final StringSetting keyword = new StringSetting("Keyword", "pickaxe");
    private final NumberSetting maxPrice = new NumberSetting("Max Price", 1000, 10000000, 100000, 1000);
    private final NumberSetting delay = new NumberSetting("Delay (MS)", 100, 10000, 1200, 100);
    private final StringSetting buyCommand = new StringSetting("Buy Command", "/ah buy {item}");
    private final StringSetting sellCommand = new StringSetting("Sell Command", "/ah sell {item}");

    private final Pattern pricePattern = Pattern.compile("([0-9][0-9.,kK]*)");
    private long lastAction;

    public AutoAuction() {
        super("Auto Auction", "FunTime /ah sniper and seller with chat price parsing", Category.FARM);
        addSettings(mode, keyword, maxPrice, delay, buyCommand, sellCommand);
    }

    @EventHandler
    private void onPacket(PacketEvent event) {
        if (isNull() || event.getOrder() != TransferOrder.RECEIVE) return;
        if (!mode.isMode("Snipe") && !mode.isMode("Both")) return;

        String message = null;
        if (event.getPacket() instanceof ProfilelessChatMessageS2CPacket packet) {
            message = packet.comp_1097().getString();
        } else if (event.getPacket() instanceof ChatMessageS2CPacket packet) {
            message = packet.comp_1103().getString();
        }
        if (message == null) return;

        String key = keyword.getValue().toLowerCase();
        if (!message.toLowerCase().contains(key)) return;
        long price = parsePrice(message);
        if (price <= 0 || price > maxPrice.getValue()) return;
        if (System.currentTimeMillis() - lastAction < delay.getValue()) return;

        String command = buyCommand.getValue().replace("{item}", keyword.getValue());
        mc.player.networkHandler.sendChatCommand(command.startsWith("/") ? command.substring(1) : command);
        lastAction = System.currentTimeMillis();
    }

    @EventHandler
    private void onTick(TickEvent event) {
        if (isNull()) return;
        if (!mode.isMode("Sell") && !mode.isMode("Both")) return;
        if (System.currentTimeMillis() - lastAction < delay.getValue()) return;
        String command = sellCommand.getValue().replace("{item}", keyword.getValue());
        mc.player.networkHandler.sendChatCommand(command.startsWith("/") ? command.substring(1) : command);
        lastAction = System.currentTimeMillis();
    }

    private long parsePrice(String message) {
        Matcher matcher = pricePattern.matcher(message);
        long best = -1;
        while (matcher.find()) {
            String raw = matcher.group(1).replace(",", "").replace(".", "");
            long value;
            try {
                value = Long.parseLong(raw);
            } catch (NumberFormatException ignored) {
                continue;
            }
            if (raw.toLowerCase().endsWith("k")) value *= 1000;
            if (value > best) best = value;
        }
        return best;
    }
}
