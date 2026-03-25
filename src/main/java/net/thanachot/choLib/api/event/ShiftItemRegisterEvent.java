package net.thanachot.choLib.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.util.ActionResult;
import net.thanachot.choLib.api.ShiftHand;
import net.thanachot.choLib.api.ShiftRegistrationType;

public interface ShiftItemRegisterEvent {
    Event<ShiftItemRegisterEvent> EVENT = EventFactory.createArrayBacked(
        ShiftItemRegisterEvent.class,
        callbacks -> (registrant, type, hand) -> {
            for (ShiftItemRegisterEvent callback : callbacks) {
                ActionResult result = callback.onRegister(registrant, type, hand);
                if (result != ActionResult.PASS) return result;
            }
            return ActionResult.PASS;
        }
    );

    ActionResult onRegister(Object registrant, ShiftRegistrationType type, ShiftHand hand);
}
