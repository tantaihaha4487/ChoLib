package net.thanachot.choLib.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;

/**
 * Event fired on each shift press before activation threshold is reached.
 * Can be cancelled to stop the shift sequence.
 */
public interface ShiftProgressEvent {
    Event<ShiftProgressEvent> EVENT = EventFactory.createArrayBacked(
        ShiftProgressEvent.class,
        callbacks -> (player, currentProgress, maxProgress, percentage) -> {
            for (ShiftProgressEvent callback : callbacks) {
                ActionResult result = callback.onProgress(player, currentProgress, maxProgress, percentage);
                if (result != ActionResult.PASS) {
                    return result;
                }
            }
            return ActionResult.PASS;
        }
    );

    /**
     * Called on each shift press during progress toward activation.
     *
     * @param player The player pressing shift
     * @param currentProgress Current number of presses in window
     * @param maxProgress Required presses for activation
     * @param percentage Progress percentage (0-100)
     * @return ActionResult.FAIL to cancel the sequence, ActionResult.PASS to continue
     */
    ActionResult onProgress(PlayerEntity player, int currentProgress, int maxProgress, int percentage);
}
