package net.thanachot.choLib.internal.shift;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Fabric event listener for shift activation system.
 * Listens for sneak events, item swaps, and server ticks.
 */
public class ShiftListener {

    private static boolean initialized = false;

    // Track last held items to detect swaps
    private static final Map<UUID, ItemStack> lastMainHand = new HashMap<>();
    private static final Map<UUID, ItemStack> lastOffHand = new HashMap<>();

    // Track previous sneak state to detect transitions (not sneaking → sneaking)
    private static final Map<UUID, Boolean> wasSneaking = new HashMap<>();

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        // Listen for server start to set server reference
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ShiftManager.getInstance().setServer(server);
        });

        // Listen for player tick to detect sneaking and item swaps
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            currentServerTick = server.getTicks();
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                checkSneakState(player);
                checkItemSwap(player);
            }

            // Run cleanup
            ShiftManager.getInstance().onServerTick();
        });

        // Clean up on disconnect
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID playerUuid = handler.player.getUuid();
            lastMainHand.remove(playerUuid);
            lastOffHand.remove(playerUuid);
            wasSneaking.remove(playerUuid);
            lastSneakTick.remove(playerUuid);
        });
    }

    private static void checkSneakState(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        boolean currentlySneaking = player.isSneaking();
        boolean previouslySneaking = wasSneaking.getOrDefault(uuid, false);

        // Only count shift on transition: not sneaking → sneaking
        // This prevents long-press from counting as multiple presses
        if (currentlySneaking && !previouslySneaking) {
            ShiftManager.getInstance().handleSneak(player);
        }

        // Update the tracking state
        wasSneaking.put(uuid, currentlySneaking);
    }

    private static final Map<UUID, Long> lastSneakTick = new HashMap<>();
    private static long currentServerTick = 0;

    private static boolean canRecordSneak(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        long currentTick = currentServerTick;
        Long lastTick = lastSneakTick.get(uuid);

        // Only record if this is a new sneak (not same tick)
        if (lastTick == null || lastTick < currentTick) {
            lastSneakTick.put(uuid, currentTick);
            return true;
        }

        return false;
    }

    private static void checkItemSwap(ServerPlayerEntity player) {
        UUID playerUuid = player.getUuid();

        ItemStack currentMain = player.getMainHandStack();
        ItemStack currentOff = player.getOffHandStack();

        ItemStack lastMain = lastMainHand.get(playerUuid);
        ItemStack lastOff = lastOffHand.get(playerUuid);

        // Check if main hand changed
        if (lastMain == null || !ItemStack.areEqual(lastMain, currentMain) ||
            lastOff == null || !ItemStack.areEqual(lastOff, currentOff)) {
            ShiftManager.getInstance().checkItemSwap(player);
        }

        // Update tracked items
        lastMainHand.put(playerUuid, currentMain.copy());
        lastOffHand.put(playerUuid, currentOff.copy());
    }
}
