package net.thanachot.choLib.api;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

@FunctionalInterface
public interface ShiftActivationHandler {
    ActionResult activate(PlayerEntity player, ItemStack stack, Hand hand);
}
