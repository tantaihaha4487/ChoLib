package net.thanachot.choLib.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

public interface ShiftActivationEvent {
    Event<ShiftActivationEvent> EVENT = EventFactory.createArrayBacked(
        ShiftActivationEvent.class,
        callbacks -> (player, stack, hand) -> {
            for (ShiftActivationEvent callback : callbacks) {
                ActionResult result = callback.onActivate(player, stack, hand);
                if (result != ActionResult.PASS) return result;
            }
            return ActionResult.PASS;
        }
    );

    ActionResult onActivate(PlayerEntity player, ItemStack stack, Hand hand);
}
