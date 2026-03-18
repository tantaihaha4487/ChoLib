package net.thanachot.choLib.internal.shift;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.thanachot.choLib.api.ShiftActivationHandler;
import net.thanachot.choLib.api.ShiftDeactivationReason;
import net.thanachot.choLib.api.ShiftHand;
import net.thanachot.choLib.api.ShiftRegistrationType;
import net.thanachot.choLib.api.event.ShiftActivationEvent;
import net.thanachot.choLib.api.event.ShiftDeactivationEvent;
import net.thanachot.choLib.api.event.ShiftItemRegisterEvent;
import net.thanachot.choLib.api.event.ShiftProgressEvent;
import net.thanachot.choLib.internal.util.ActionBarHelper;

import java.util.*;
import java.util.function.Predicate;

/**
 * Core manager for shift activation system.
 * Handles registration, tracking, and activation logic.
 */
public class ShiftManager {
    private static ShiftManager instance;

    // Configuration
    private int maxProgress = 10;
    private int windowTicks = 60;  // 3 seconds
    private int cooldownTicks = 40; // 2 seconds

    // Registrations
    private final Map<ShiftHand, Map<Item, ShiftActivationHandler>> itemHandlers;
    private final Map<ShiftHand, Map<TagKey<Item>, ShiftActivationHandler>> tagHandlers;
    private final Map<ShiftHand, Map<Class<? extends Item>, ShiftActivationHandler>> classHandlers;
    private final Map<ShiftHand, List<PredicateEntry>> predicateHandlers;

    // Player tracking
    private final Map<UUID, PlayerShiftTracker> playerTrackers;
    private final Map<UUID, ActiveAbility> activeAbilities;

    // Server reference for tick counting
    private MinecraftServer server;

    private static class PredicateEntry {
        final Predicate<ItemStack> predicate;
        final ShiftActivationHandler handler;

        PredicateEntry(Predicate<ItemStack> predicate, ShiftActivationHandler handler) {
            this.predicate = predicate;
            this.handler = handler;
        }
    }

    private static class ActiveAbility {
        final ItemStack stack;
        final Hand hand;

        ActiveAbility(ItemStack stack, Hand hand) {
            this.stack = stack;
            this.hand = hand;
        }
    }

    private ShiftManager() {
        this.itemHandlers = new EnumMap<>(ShiftHand.class);
        this.tagHandlers = new EnumMap<>(ShiftHand.class);
        this.classHandlers = new EnumMap<>(ShiftHand.class);
        this.predicateHandlers = new EnumMap<>(ShiftHand.class);
        this.playerTrackers = new HashMap<>();
        this.activeAbilities = new HashMap<>();

        for (ShiftHand hand : ShiftHand.values()) {
            itemHandlers.put(hand, new HashMap<>());
            tagHandlers.put(hand, new HashMap<>());
            classHandlers.put(hand, new HashMap<>());
            predicateHandlers.put(hand, new ArrayList<>());
        }
    }

    public static ShiftManager getInstance() {
        if (instance == null) {
            instance = new ShiftManager();
        }
        return instance;
    }

    public void setServer(MinecraftServer server) {
        this.server = server;
    }

    private long getCurrentTick() {
        return server != null ? server.getTicks() : 0;
    }

    // ==================== Registration Methods ====================

    public void registerItem(Item item, ShiftHand hand, ShiftActivationHandler handler) {
        ActionResult result = ShiftItemRegisterEvent.EVENT.invoker().onRegister(item, ShiftRegistrationType.ITEM, hand);
        if (result == ActionResult.FAIL) {
            return;
        }
        itemHandlers.get(hand).put(item, handler);
    }

    public void registerTag(TagKey<Item> tag, ShiftHand hand, ShiftActivationHandler handler) {
        ActionResult result = ShiftItemRegisterEvent.EVENT.invoker().onRegister(tag, ShiftRegistrationType.TAG, hand);
        if (result == ActionResult.FAIL) {
            return;
        }
        tagHandlers.get(hand).put(tag, handler);
    }

    public void registerItemClass(Class<? extends Item> itemClass, ShiftHand hand, ShiftActivationHandler handler) {
        ActionResult result = ShiftItemRegisterEvent.EVENT.invoker().onRegister(itemClass, ShiftRegistrationType.CLASS, hand);
        if (result == ActionResult.FAIL) {
            return;
        }
        classHandlers.get(hand).put(itemClass, handler);
    }

    public void registerPredicate(Predicate<ItemStack> predicate, ShiftHand hand, ShiftActivationHandler handler) {
        ActionResult result = ShiftItemRegisterEvent.EVENT.invoker().onRegister(predicate, ShiftRegistrationType.PREDICATE, hand);
        if (result == ActionResult.FAIL) {
            return;
        }
        predicateHandlers.get(hand).add(new PredicateEntry(predicate, handler));
    }

    // ==================== Configuration ====================

    public void setMaxProgress(int maxProgress) {
        this.maxProgress = maxProgress;
    }

    public int getMaxProgress() {
        return maxProgress;
    }

    public void setWindowTicks(int ticks) {
        this.windowTicks = ticks;
        for (PlayerShiftTracker tracker : playerTrackers.values()) {
            tracker.updateWindowTicks(ticks);
        }
    }

    public void setCooldownTicks(int ticks) {
        this.cooldownTicks = ticks;
        for (PlayerShiftTracker tracker : playerTrackers.values()) {
            tracker.updateCooldownTicks(ticks);
        }
    }

    // ==================== Core Logic ====================

    /**
     * Called when player presses shift.
     */
    public void handleSneak(ServerPlayerEntity player) {
        UUID playerUuid = player.getUuid();
        long currentTick = getCurrentTick();

        // Get or create tracker
        PlayerShiftTracker tracker = playerTrackers.computeIfAbsent(
            playerUuid,
            uuid -> new PlayerShiftTracker(uuid, windowTicks, cooldownTicks)
        );

        // Check held items and find handler
        HandResult result = findHandler(player);
        if (result == null) {
            return; // No registered item found
        }

        // Record press
        int pressCount = tracker.recordPress(currentTick);
        if (pressCount == 0) {
            return; // On cooldown
        }

        // Check for activation
        if (tracker.checkActivation(maxProgress, currentTick)) {
            // Fire activation event
            ActionResult activationResult = ShiftActivationEvent.EVENT.invoker().onActivate(
                player, result.stack, result.hand
            );

            if (activationResult != ActionResult.FAIL) {
                // Execute handler
                ActionResult handlerResult = result.handler.activate(player, result.stack, result.hand);

                if (handlerResult == ActionResult.SUCCESS) {
                    tracker.activate(currentTick);
                    activeAbilities.put(playerUuid, new ActiveAbility(result.stack, result.hand));
                }
            }
        } else {
            // Fire progress event
            int percentage = (pressCount * 100) / maxProgress;
            ActionResult progressResult = ShiftProgressEvent.EVENT.invoker().onProgress(
                player, pressCount, maxProgress, percentage
            );

            if (progressResult != ActionResult.FAIL) {
                // Show progress bar
                ActionBarHelper.sendProgressBar(player, pressCount, maxProgress, percentage);
            }
        }
    }

    /**
     * Check if player swapped items and deactivate if necessary.
     */
    public void checkItemSwap(ServerPlayerEntity player) {
        UUID playerUuid = player.getUuid();
        ActiveAbility active = activeAbilities.get(playerUuid);

        if (active != null) {
            ItemStack currentMain = player.getMainHandStack();
            ItemStack currentOff = player.getOffHandStack();

            boolean stillHolding = false;

            // Check if still holding the same item in the same hand
            if (active.hand == Hand.MAIN_HAND) {
                stillHolding = ItemStack.areEqual(currentMain, active.stack);
            } else {
                stillHolding = ItemStack.areEqual(currentOff, active.stack);
            }

            if (!stillHolding) {
                deactivate(playerUuid, ShiftDeactivationReason.ITEM_SWAP);
            }
        }
    }

    /**
     * Find handler for player's held items.
     */
    private HandResult findHandler(ServerPlayerEntity player) {
        ItemStack mainHand = player.getMainHandStack();
        ItemStack offHand = player.getOffHandStack();

        // Check main hand first
        if (!mainHand.isEmpty()) {
            ShiftActivationHandler handler = findHandlerForStack(mainHand, ShiftHand.MAIN_HAND);
            if (handler != null) {
                return new HandResult(handler, mainHand, Hand.MAIN_HAND);
            }
        }

        // Check off hand
        if (!offHand.isEmpty()) {
            ShiftActivationHandler handler = findHandlerForStack(offHand, ShiftHand.OFF_HAND);
            if (handler != null) {
                return new HandResult(handler, offHand, Hand.OFF_HAND);
            }
        }

        return null;
    }

    private ShiftActivationHandler findHandlerForStack(ItemStack stack, ShiftHand hand) {
        Item item = stack.getItem();

        // Check specific items
        ShiftActivationHandler handler = itemHandlers.get(hand).get(item);
        if (handler != null) return handler;

        // Check tags
        for (Map.Entry<TagKey<Item>, ShiftActivationHandler> entry : tagHandlers.get(hand).entrySet()) {
            if (stack.isIn(entry.getKey())) {
                return entry.getValue();
            }
        }

        // Check classes
        for (Map.Entry<Class<? extends Item>, ShiftActivationHandler> entry : classHandlers.get(hand).entrySet()) {
            if (entry.getKey().isInstance(item)) {
                return entry.getValue();
            }
        }

        // Check predicates
        for (PredicateEntry entry : predicateHandlers.get(hand)) {
            if (entry.predicate.test(stack)) {
                return entry.handler;
            }
        }

        return null;
    }

    private static class HandResult {
        final ShiftActivationHandler handler;
        final ItemStack stack;
        final Hand hand;

        HandResult(ShiftActivationHandler handler, ItemStack stack, Hand hand) {
            this.handler = handler;
            this.stack = stack;
            this.hand = hand;
        }
    }

    // ==================== Deactivation ====================

    public void deactivate(UUID playerUuid) {
        deactivate(playerUuid, ShiftDeactivationReason.MANUAL);
    }

    public void deactivate(UUID playerUuid, ShiftDeactivationReason reason) {
        ActiveAbility active = activeAbilities.remove(playerUuid);
        PlayerShiftTracker tracker = playerTrackers.get(playerUuid);

        if (tracker != null) {
            tracker.reset();
        }

        if (active != null && server != null) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerUuid);
            if (player != null) {
                ShiftDeactivationEvent.EVENT.invoker().onDeactivate(player, active.stack, active.hand, reason);
            }
        }
    }

    public void deactivateAll() {
        for (UUID playerUuid : new ArrayList<>(activeAbilities.keySet())) {
            deactivate(playerUuid, ShiftDeactivationReason.MANUAL);
        }
    }

    public boolean isActive(UUID playerUuid) {
        return activeAbilities.containsKey(playerUuid);
    }

    // ==================== Cleanup ====================

    public void onServerTick() {
        long currentTick = getCurrentTick();

        // Clean up expired trackers
        playerTrackers.entrySet().removeIf(entry -> {
            PlayerShiftTracker tracker = entry.getValue();
            // Remove if no recent activity and not active
            return !activeAbilities.containsKey(entry.getKey()) &&
                   tracker.getPressCount(currentTick) == 0 &&
                   !tracker.isOnCooldown(currentTick);
        });
    }
}
