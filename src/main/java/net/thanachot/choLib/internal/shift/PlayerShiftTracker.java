package net.thanachot.choLib.internal.shift;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

public class PlayerShiftTracker {
    private final UUID playerUuid;
    private final Deque<Long> pressTimestamps;
    private long lastActivationTime;
    private boolean isOnCooldown;
    private boolean hasShownBar;
    private int windowTicks;
    private int cooldownTicks;

    public PlayerShiftTracker(UUID playerUuid, int windowTicks, int cooldownTicks) {
        this.playerUuid = playerUuid;
        this.pressTimestamps = new ArrayDeque<>();
        this.lastActivationTime = 0;
        this.isOnCooldown = false;
        this.hasShownBar = false;
        this.windowTicks = windowTicks;
        this.cooldownTicks = cooldownTicks;
    }

    public int recordPress(long currentTick) {
        if (isOnCooldown) {
            if (currentTick - lastActivationTime >= cooldownTicks) {
                isOnCooldown = false;
            } else {
                return 0;
            }
        }

        cleanOldPresses(currentTick);
        pressTimestamps.addLast(currentTick);
        hasShownBar = true;

        return pressTimestamps.size();
    }

    public boolean checkActivation(int maxProgress, long currentTick) {
        cleanOldPresses(currentTick);
        return pressTimestamps.size() >= maxProgress;
    }

    public void activate(long currentTick) {
        lastActivationTime = currentTick;
        isOnCooldown = true;
        pressTimestamps.clear();
        hasShownBar = false;
    }

    public void reset() {
        pressTimestamps.clear();
        lastActivationTime = 0;
        isOnCooldown = false;
        hasShownBar = false;
    }

    public int getPressCount(long currentTick) {
        cleanOldPresses(currentTick);
        return pressTimestamps.size();
    }

    public boolean isOnCooldown(long currentTick) {
        if (isOnCooldown && currentTick - lastActivationTime >= cooldownTicks) {
            isOnCooldown = false;
        }
        return isOnCooldown;
    }

    public boolean hasShownBar() {
        return hasShownBar;
    }

    public boolean isWindowActive(long currentTick) {
        cleanOldPresses(currentTick);
        return !pressTimestamps.isEmpty();
    }

    public void updateWindowTicks(int windowTicks) {
        this.windowTicks = windowTicks;
    }

    public void updateCooldownTicks(int cooldownTicks) {
        this.cooldownTicks = cooldownTicks;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    private void cleanOldPresses(long currentTick) {
        long cutoffTick = currentTick - windowTicks;
        while (!pressTimestamps.isEmpty() && pressTimestamps.peekFirst() < cutoffTick) {
            pressTimestamps.pollFirst();
        }
    }
}
