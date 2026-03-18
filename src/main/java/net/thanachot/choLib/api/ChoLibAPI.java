package net.thanachot.choLib.api;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.TagKey;
import net.thanachot.choLib.internal.shift.ShiftManager;

import java.util.function.Predicate;

/**
 * Main API class for ChoLib shift mechanism.
 * Provides easy registration methods for shift-activatable items.
 */
public class ChoLibAPI {

    /**
     * Register a specific item for shift activation.
     *
     * @param item The item to register
     * @param hand The hand requirement
     * @param handler The activation handler
     */
    public static void registerItem(Item item, ShiftHand hand, ShiftActivationHandler handler) {
        ShiftManager.getInstance().registerItem(item, hand, handler);
    }

    /**
     * Register an item tag for shift activation.
     * All items in the tag will trigger the same handler.
     *
     * @param tag The item tag to register
     * @param hand The hand requirement
     * @param handler The activation handler
     */
    public static void registerTag(TagKey<Item> tag, ShiftHand hand, ShiftActivationHandler handler) {
        ShiftManager.getInstance().registerTag(tag, hand, handler);
    }

    /**
     * Register a custom item class for shift activation.
     * All instances of this class or subclasses will trigger the handler.
     *
     * @param itemClass The item class to register
     * @param hand The hand requirement
     * @param handler The activation handler
     */
    public static void registerItemClass(Class<? extends Item> itemClass, ShiftHand hand, ShiftActivationHandler handler) {
        ShiftManager.getInstance().registerItemClass(itemClass, hand, handler);
    }

    /**
     * Register a predicate for shift activation.
     * Items matching the predicate will trigger the handler.
     *
     * @param predicate The predicate to test items against
     * @param hand The hand requirement
     * @param handler The activation handler
     */
    public static void registerPredicate(Predicate<ItemStack> predicate, ShiftHand hand, ShiftActivationHandler handler) {
        ShiftManager.getInstance().registerPredicate(predicate, hand, handler);
    }

    /**
     * Set the maximum progress (number of shift presses required for activation).
     *
     * @param maxProgress Number of presses required (default: 10)
     */
    public static void setMaxProgress(int maxProgress) {
        ShiftManager.getInstance().setMaxProgress(maxProgress);
    }

    /**
     * Get the current maximum progress setting.
     *
     * @return Number of presses required for activation
     */
    public static int getMaxProgress() {
        return ShiftManager.getInstance().getMaxProgress();
    }

    /**
     * Set the window duration in ticks.
     *
     * @param ticks Number of ticks (default: 60 = 3 seconds at 20 TPS)
     */
    public static void setWindowTicks(int ticks) {
        ShiftManager.getInstance().setWindowTicks(ticks);
    }

    /**
     * Set the cooldown duration in ticks after activation.
     *
     * @param ticks Number of ticks (default: 40 = 2 seconds at 20 TPS)
     */
    public static void setCooldownTicks(int ticks) {
        ShiftManager.getInstance().setCooldownTicks(ticks);
    }

    /**
     * Manually deactivate shift ability for a player.
     *
     * @param playerUuid The player's UUID
     */
    public static void deactivate(java.util.UUID playerUuid) {
        ShiftManager.getInstance().deactivate(playerUuid);
    }

    /**
     * Deactivate shift ability for all players.
     */
    public static void deactivateAll() {
        ShiftManager.getInstance().deactivateAll();
    }

    /**
     * Check if a player has an active shift ability.
     *
     * @param playerUuid The player's UUID
     * @return true if the player has an active ability
     */
    public static boolean isActive(java.util.UUID playerUuid) {
        return ShiftManager.getInstance().isActive(playerUuid);
    }
}
