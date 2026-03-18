package net.thanachot.choLib.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.util.ActionResult;
import net.thanachot.choLib.api.ShiftHand;
import net.thanachot.choLib.api.ShiftRegistrationType;

/**
 * Event fired when an item is registered for shift activation.
 * Can be used for logging, validation, or modifying registrations.
 */
public interface ShiftItemRegisterEvent {
    Event<ShiftItemRegisterEvent> EVENT = EventFactory.createArrayBacked(
        ShiftItemRegisterEvent.class,
        callbacks -> (registrant, type, hand) -> {
            for (ShiftItemRegisterEvent callback : callbacks) {
                ActionResult result = callback.onRegister(registrant, type, hand);
                if (result != ActionResult.PASS) {
                    return result;
                }
            }
            return ActionResult.PASS;
        }
    );

    /**
     * Called when an item is registered for shift activation.
     *
     * @param registrant The item, tag, class, or predicate being registered
     * @param type The type of registration
     * @param hand The hand requirement
     * @return ActionResult.SUCCESS to allow, ActionResult.FAIL to deny registration
     */
    ActionResult onRegister(Object registrant, ShiftRegistrationType type, ShiftHand hand);
}
