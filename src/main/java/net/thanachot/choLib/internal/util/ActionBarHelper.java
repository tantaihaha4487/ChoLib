package net.thanachot.choLib.internal.util;

import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ActionBarHelper {

    private static final int BAR_LENGTH = 10;
    private static final String GREEN = Formatting.GREEN.toString();
    private static final String BLACK = Formatting.BLACK.toString();

    public static void sendProgressBar(ServerPlayerEntity player, int current, int max, int percentage) {
        String bar = buildProgressBar(percentage);
        sendActionBar(player, Text.literal(bar));
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
        sendActionBar(player, Text.literal("Cooldown...").formatted(Formatting.RED));
    }

    public static Text buildProgressBarAsText(ServerPlayerEntity player, int current, int max) {
        int percentage = (max == 0) ? 0 : (current * 100) / max;
        String bar = buildProgressBar(percentage);
        return Text.literal(bar);
    }

    private static String buildProgressBar(int percentage) {
        int rounded = Math.round(percentage);
        int filled = rounded / 10;

        if (rounded == 0) return GREEN + "╞" + BLACK + "══════════" + GREEN + "╡ 0%";
        if (rounded == 100) return GREEN + "╞" + GREEN + "══════════" + GREEN + "╡ 100%";

        StringBuilder bar = new StringBuilder();
        bar.append(GREEN).append("╞");

        for (int i = 0; i < BAR_LENGTH; i++) {
            if (i < filled) {
                bar.append(GREEN).append("═");
            } else if (i == filled) {
                bar.append(GREEN).append("▰");
            } else {
                bar.append(BLACK).append("═");
            }
        }

        bar.append(GREEN).append("╡ ").append(rounded).append("%");
        return bar.toString();
    }
}
