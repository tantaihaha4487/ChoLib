package net.thanachot.choLib.internal.shift;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShiftListener {

    private static boolean initialized = false;
    private static final Map<UUID, ItemStack> lastMainHand = new HashMap<>();
    private static final Map<UUID, ItemStack> lastOffHand = new HashMap<>();
    private static final Map<UUID, Boolean> wasSneaking = new HashMap<>();
    private static final Map<UUID, Long> lastSneakTick = new HashMap<>();
    private static long currentServerTick = 0;

    public static void init() {
        if (initialized) return;
        initialized = true;

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ShiftManager.getInstance().setServer(server);
            currentServerTick = server.getTicks();

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                checkSneakState(player);
                checkItemSwap(player);
            }
            ShiftManager.getInstance().onServerTick();
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID uuid = handler.player.getUuid();
            lastMainHand.remove(uuid);
            lastOffHand.remove(uuid);
            wasSneaking.remove(uuid);
            lastSneakTick.remove(uuid);
        });
    }

    private static void checkSneakState(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        boolean isSneaking = player.isSneaking();
        boolean wasSneak = wasSneaking.getOrDefault(uuid, false);

        if (isSneaking && !wasSneak) {
            ShiftManager.getInstance().handleSneak(player);
        }
        wasSneaking.put(uuid, isSneaking);
    }

    private static boolean canRecordSneak(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        Long lastTick = lastSneakTick.get(uuid);

        if (lastTick == null || lastTick < currentServerTick) {
            lastSneakTick.put(uuid, currentServerTick);
            return true;
        }
        return false;
    }

    private static void checkItemSwap(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        ItemStack currentMain = player.getMainHandStack();
        ItemStack currentOff = player.getOffHandStack();

        ItemStack lastMain = lastMainHand.get(uuid);
        ItemStack lastOff = lastOffHand.get(uuid);

        if (lastMain == null || !ItemStack.areEqual(lastMain, currentMain) ||
            lastOff == null || !ItemStack.areEqual(lastOff, currentOff)) {
            ShiftManager.getInstance().checkItemSwap(player);
        }

        lastMainHand.put(uuid, currentMain.copy());
        lastOffHand.put(uuid, currentOff.copy());
    }
}
