package net.thanachot.choLib.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.thanachot.choLib.api.ShiftDeactivationReason;

/**
 * Event fired when shift ability is deactivated.
 * Can be used for cleanup, removing effects, etc.
 */
public interface ShiftDeactivationEvent {
    Event<ShiftDeactivationEvent> EVENT = EventFactory.createArrayBacked(
        ShiftDeactivationEvent.class,
        callbacks -> (player, stack, hand, reason) -> {
            for (ShiftDeactivationEvent callback : callbacks) {
                ActionResult result = callback.onDeactivate(player, stack, hand, reason);
                if (result != ActionResult.PASS) {
                    return result;
                }
            }
            return ActionResult.PASS;
        }
    );

    /**
     * Called when shift ability is deactivated.
     *
     * @param player The player whose ability is deactivated
     * @param stack The item stack (may be empty if item was dropped)
     * @param hand The hand that held the item
     * @param reason Why the ability was deactivated
     * @return ActionResult.SUCCESS to consume, ActionResult.PASS to allow other handlers
     */
    ActionResult onDeactivate(PlayerEntity player, ItemStack stack, Hand hand, ShiftDeactivationReason reason);
}
