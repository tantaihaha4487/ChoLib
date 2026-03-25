package net.thanachot.choLib.internal.shift;

import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

/**
 * Tracks shift presses for a single player.
 * Maintains a sliding window of press timestamps.
 */
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

    /**
     * Record a shift press.
     *
     * @param currentTick Current server tick
     * @return Current press count in window, or 0 if on cooldown
     */
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

    /**
     * Check if the player has reached the activation threshold.
     *
     * @param maxProgress Required number of presses
     * @param currentTick Current server tick
     * @return true if threshold reached
     */
    public boolean checkActivation(int maxProgress, long currentTick) {
        cleanOldPresses(currentTick);
        return pressTimestamps.size() >= maxProgress;
    }

    /**
     * Mark ability as activated, starting cooldown.
     *
     * @param currentTick Current server tick
     */
    public void activate(long currentTick) {
        lastActivationTime = currentTick;
        isOnCooldown = true;
        pressTimestamps.clear();
        hasShownBar = false;
    }

    /**
     * Reset the tracker (clear all presses and cooldown).
     */
    public void reset() {
        pressTimestamps.clear();
        lastActivationTime = 0;
        isOnCooldown = false;
        hasShownBar = false;
    }

    /**
     * Clean presses older than the window.
     *
     * @param currentTick Current server tick
     */
    private void cleanOldPresses(long currentTick) {
        long cutoffTick = currentTick - windowTicks;
        while (!pressTimestamps.isEmpty() && pressTimestamps.peekFirst() < cutoffTick) {
            pressTimestamps.pollFirst();
        }
    }

    /**
     * Get current press count.
     *
     * @param currentTick Current server tick
     * @return Number of presses in window
     */
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
}
