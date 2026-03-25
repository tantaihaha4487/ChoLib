package net.thanachot.choLib.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;

public interface ShiftProgressEvent {
    Event<ShiftProgressEvent> EVENT = EventFactory.createArrayBacked(
        ShiftProgressEvent.class,
        callbacks -> (player, currentProgress, maxProgress, percentage) -> {
            for (ShiftProgressEvent callback : callbacks) {
                ActionResult result = callback.onProgress(player, currentProgress, maxProgress, percentage);
                if (result != ActionResult.PASS) return result;
            }
            return ActionResult.PASS;
        }
    );

    ActionResult onProgress(PlayerEntity player, int currentProgress, int maxProgress, int percentage);
}
