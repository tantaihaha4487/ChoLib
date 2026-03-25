package net.thanachot.choLib.internal.util;

import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ActionBarHelper {

    private static final int BAR_LENGTH = 10;
    private static final String FILLED_BAR = Formatting.GREEN.toString();
    private static final String EMPTY_BAR = Formatting.BLACK.toString();
    private static final String TRANSITION_CHAR = Formatting.GREEN.toString();

    public static void sendProgressBar(ServerPlayerEntity player, int current, int max, int percentage) {
        String bar = buildProgressBar(current, max, percentage);
        MutableText message = Text.literal(bar);
        sendActionBar(player, message);
    }

    public static void sendActionBar(ServerPlayerEntity player, Text message) {
        player.networkHandler.sendPacket(new OverlayMessageS2CPacket(message));
    }

    public static void sendActivationSuccess(ServerPlayerEntity player) {
        MutableText message = Text.literal("");
        message.append(Text.literal("(").formatted(Formatting.GOLD, Formatting.BOLD));
        message.append(Text.literal("i").formatted(Formatting.RED, Formatting.BOLD));
        message.append(Text.literal(")").formatted(Formatting.GOLD, Formatting.BOLD));
        message.append(Text.literal(" "));
        message.append(Text.literal("ACTIVATED!").formatted(Formatting.GREEN, Formatting.BOLD));
        sendActionBar(player, message);
    }

    public static void sendCooldownMessage(ServerPlayerEntity player) {
        Text message = Text.literal("Cooldown...")
            .formatted(Formatting.RED);
        sendActionBar(player, message);
    }

    private static String buildProgressBar(int current, int max, int percentage) {
        int rounded = Math.round(percentage);
        int greenBars = rounded / 10;

        if (rounded == 0) {
            return FILLED_BAR + "╞" + EMPTY_BAR + "══════════" + FILLED_BAR + "╡ 0%";
        }
        if (rounded == 100) {
            return FILLED_BAR + "╞" + FILLED_BAR + "══════════" + FILLED_BAR + "╡ 100%";
        }

        StringBuilder bar = new StringBuilder();
        bar.append(FILLED_BAR);
        bar.append("╞");

        for (int i = 0; i < BAR_LENGTH; i++) {
            if (i < greenBars) {
                bar.append(FILLED_BAR).append("═");
            } else if (i == greenBars) {
                bar.append(TRANSITION_CHAR).append("▰");
            } else {
                bar.append(EMPTY_BAR).append("═");
            }
        }

        bar.append(FILLED_BAR).append("╡ ");
        bar.append(rounded);
        bar.append("%");

        return bar.toString();
    }
}
