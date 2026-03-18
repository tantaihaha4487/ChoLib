package net.thanachot.choLib.internal.util;

import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Helper class for displaying progress on the action bar.
 */
public class ActionBarHelper {

    private static final int BAR_LENGTH = 10;
    private static final char FILLED_CHAR = '█';
    private static final char EMPTY_CHAR = '░';

    /**
     * Send a progress bar to the player's action bar.
     *
     * @param player The player to send to
     * @param current Current progress
     * @param max Maximum progress
     * @param percentage Percentage (0-100)
     */
    public static void sendProgressBar(ServerPlayerEntity player, int current, int max, int percentage) {
        int filledBars = (current * BAR_LENGTH) / max;
        int emptyBars = BAR_LENGTH - filledBars;

        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < filledBars; i++) {
            bar.append(FILLED_CHAR);
        }
        for (int i = 0; i < emptyBars; i++) {
            bar.append(EMPTY_CHAR);
        }

        MutableText message = Text.literal(bar.toString())
            .formatted(Formatting.GREEN)
            .append(Text.literal(" " + percentage + "%")
                .formatted(Formatting.WHITE));

        sendActionBar(player, message);
    }

    /**
     * Send a text message to the player's action bar.
     *
     * @param player The player to send to
     * @param message The message to send
     */
    public static void sendActionBar(ServerPlayerEntity player, Text message) {
        player.networkHandler.sendPacket(new OverlayMessageS2CPacket(message));
    }

    /**
     * Send activation success message.
     *
     * @param player The player to send to
     */
    public static void sendActivationSuccess(ServerPlayerEntity player) {
        Text message = Text.literal("✓ ACTIVATED!")
            .formatted(Formatting.GREEN, Formatting.BOLD);
        sendActionBar(player, message);
    }

    /**
     * Send cooldown message.
     *
     * @param player The player to send to
     */
    public static void sendCooldownMessage(ServerPlayerEntity player) {
        Text message = Text.literal("Cooldown...")
            .formatted(Formatting.RED);
        sendActionBar(player, message);
    }
}
