package net.thanachot.choLib.api;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.TagKey;
import net.thanachot.choLib.internal.shift.ShiftManager;

import java.util.function.Predicate;
import java.util.UUID;

public class ChoLibAPI {

    public static void registerItem(Item item, ShiftHand hand, ShiftActivationHandler handler) {
        ShiftManager.getInstance().registerItem(item, hand, handler);
    }

    public static void registerTag(TagKey<Item> tag, ShiftHand hand, ShiftActivationHandler handler) {
        ShiftManager.getInstance().registerTag(tag, hand, handler);
    }

    public static void registerItemClass(Class<? extends Item> itemClass, ShiftHand hand, ShiftActivationHandler handler) {
        ShiftManager.getInstance().registerItemClass(itemClass, hand, handler);
    }

    public static void registerPredicate(Predicate<ItemStack> predicate, ShiftHand hand, ShiftActivationHandler handler) {
        ShiftManager.getInstance().registerPredicate(predicate, hand, handler);
    }

    public static void setMaxProgress(int maxProgress) {
        ShiftManager.getInstance().setMaxProgress(maxProgress);
    }

    public static int getMaxProgress() {
        return ShiftManager.getInstance().getMaxProgress();
    }

    public static void setWindowTicks(int ticks) {
        ShiftManager.getInstance().setWindowTicks(ticks);
    }

    public static void setCooldownTicks(int ticks) {
        ShiftManager.getInstance().setCooldownTicks(ticks);
    }

    public static void deactivate(UUID playerUuid) {
        ShiftManager.getInstance().deactivate(playerUuid);
    }

    public static void deactivateAll() {
        ShiftManager.getInstance().deactivateAll();
    }

    public static boolean isActive(UUID playerUuid) {
        return ShiftManager.getInstance().isActive(playerUuid);
    }
}
