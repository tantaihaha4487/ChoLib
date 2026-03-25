package net.thanachot.choLib.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.thanachot.choLib.api.ShiftDeactivationReason;

public interface ShiftDeactivationEvent {
    Event<ShiftDeactivationEvent> EVENT = EventFactory.createArrayBacked(
        ShiftDeactivationEvent.class,
        callbacks -> (player, stack, hand, reason) -> {
            for (ShiftDeactivationEvent callback : callbacks) {
                ActionResult result = callback.onDeactivate(player, stack, hand, reason);
                if (result != ActionResult.PASS) return result;
            }
            return ActionResult.PASS;
        }
    );

    ActionResult onDeactivate(PlayerEntity player, ItemStack stack, Hand hand, ShiftDeactivationReason reason);
}
