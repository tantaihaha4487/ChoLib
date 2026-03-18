package net.thanachot.choLib.api;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

/**
 * Functional interface for shift ability activation handlers.
 * Called when a player successfully activates an item's shift ability.
 */
@FunctionalInterface
public interface ShiftActivationHandler {
    /**
     * Called when shift ability is activated.
     *
     * @param player The player who activated the ability
     * @param stack The item stack being held
     * @param hand The hand holding the item
     * @return ActionResult.SUCCESS to consume the event, ActionResult.PASS to allow other handlers
     */
    ActionResult activate(PlayerEntity player, ItemStack stack, Hand hand);
}
