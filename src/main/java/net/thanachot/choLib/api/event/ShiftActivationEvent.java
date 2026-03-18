package net.thanachot.choLib.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

/**
 * Event fired when shift ability threshold is reached and ability activates.
 * Called before the registered handler executes.
 */
public interface ShiftActivationEvent {
    Event<ShiftActivationEvent> EVENT = EventFactory.createArrayBacked(
        ShiftActivationEvent.class,
        callbacks -> (player, stack, hand) -> {
            for (ShiftActivationEvent callback : callbacks) {
                ActionResult result = callback.onActivate(player, stack, hand);
                if (result != ActionResult.PASS) {
                    return result;
                }
            }
            return ActionResult.PASS;
        }
    );

    /**
     * Called when shift ability is activated.
     *
     * @param player The player activating the ability
     * @param stack The item stack being held
     * @param hand The hand holding the item
     * @return ActionResult.SUCCESS to consume, ActionResult.PASS to allow other handlers
     */
    ActionResult onActivate(PlayerEntity player, ItemStack stack, Hand hand);
}
