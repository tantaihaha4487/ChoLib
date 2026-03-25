package net.thanachot.choLib.api;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.TagKey;
import net.thanachot.choLib.internal.shift.ShiftManager;

import java.util.function.Predicate;
import java.util.UUID;

public class ChoLibAPI {

    // Per-mod registration

    public static void registerItem(String modId, Item item, ShiftHand hand, ShiftActivationHandler handler) {
        ShiftManager.getInstance().registerItem(modId, item, hand, handler);
    }

    public static void registerTag(String modId, TagKey<Item> tag, ShiftHand hand, ShiftActivationHandler handler) {
        ShiftManager.getInstance().registerTag(modId, tag, hand, handler);
    }

    public static void registerItemClass(String modId, Class<? extends Item> itemClass, ShiftHand hand, ShiftActivationHandler handler) {
        ShiftManager.getInstance().registerItemClass(modId, itemClass, hand, handler);
    }

    public static void registerPredicate(String modId, Predicate<ItemStack> predicate, ShiftHand hand, ShiftActivationHandler handler) {
        ShiftManager.getInstance().registerPredicate(modId, predicate, hand, handler);
    }

    // Per-mod configuration

    public static void setMaxProgress(String modId, int maxProgress) {
        ShiftManager.getInstance().setMaxProgress(modId, maxProgress);
    }

    public static int getMaxProgress(String modId) {
        return ShiftManager.getInstance().getMaxProgress(modId);
    }

    public static void setWindowTicks(String modId, int ticks) {
        ShiftManager.getInstance().setWindowTicks(modId, ticks);
    }

    public static int getWindowTicks(String modId) {
        return ShiftManager.getInstance().getWindowTicks(modId);
    }

    public static void setCooldownTicks(String modId, int ticks) {
        ShiftManager.getInstance().setCooldownTicks(modId, ticks);
    }

    public static int getCooldownTicks(String modId) {
        return ShiftManager.getInstance().getCooldownTicks(modId);
    }

    // Progress bar provider

    public static void setProgressBarProvider(String modId, ProgressBarProvider provider) {
        ShiftManager.getInstance().setProgressBarProvider(modId, provider);
    }

    public static ProgressBarProvider getProgressBarProvider(String modId) {
        return ShiftManager.getInstance().getProgressBarProvider(modId);
    }

    public static void setProgressBarProvider(ProgressBarProvider provider) {
        ShiftManager.getInstance().setProgressBarProvider(provider);
    }

    public static ProgressBarProvider getProgressBarProvider() {
        return ShiftManager.getInstance().getProgressBarProvider();
    }

    // Per-mod state

    public static void deactivate(String modId, UUID playerUuid) {
        ShiftManager.getInstance().deactivate(modId, playerUuid);
    }

    public static void deactivateAll(String modId) {
        ShiftManager.getInstance().deactivateAll(modId);
    }

    public static boolean isActive(String modId, UUID playerUuid) {
        return ShiftManager.getInstance().isActive(modId, playerUuid);
    }

    // Backwards-compatible global methods

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

    public static int getWindowTicks() {
        return ShiftManager.getInstance().getWindowTicks();
    }

    public static void setCooldownTicks(int ticks) {
        ShiftManager.getInstance().setCooldownTicks(ticks);
    }

    public static int getCooldownTicks() {
        return ShiftManager.getInstance().getCooldownTicks();
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
