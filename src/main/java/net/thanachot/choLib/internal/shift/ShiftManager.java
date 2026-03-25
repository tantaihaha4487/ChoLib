package net.thanachot.choLib.internal.shift;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
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

public class ShiftManager {
    private static ShiftManager instance;

    private int maxProgress = 10;
    private int windowTicks = 60;
    private int cooldownTicks = 40;

    private final Map<ShiftHand, Map<Item, ShiftActivationHandler>> itemHandlers;
    private final Map<ShiftHand, Map<TagKey<Item>, ShiftActivationHandler>> tagHandlers;
    private final Map<ShiftHand, Map<Class<? extends Item>, ShiftActivationHandler>> classHandlers;
    private final Map<ShiftHand, List<PredicateEntry>> predicateHandlers;

    private final Map<UUID, PlayerShiftTracker> playerTrackers;
    private final Map<UUID, ActiveAbility> activeAbilities;

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
        itemHandlers = new EnumMap<>(ShiftHand.class);
        tagHandlers = new EnumMap<>(ShiftHand.class);
        classHandlers = new EnumMap<>(ShiftHand.class);
        predicateHandlers = new EnumMap<>(ShiftHand.class);
        playerTrackers = new HashMap<>();
        activeAbilities = new HashMap<>();

        for (ShiftHand hand : ShiftHand.values()) {
            itemHandlers.put(hand, new HashMap<>());
            tagHandlers.put(hand, new HashMap<>());
            classHandlers.put(hand, new HashMap<>());
            predicateHandlers.put(hand, new ArrayList<>());
        }
    }

    public static ShiftManager getInstance() {
        if (instance == null) instance = new ShiftManager();
        return instance;
    }

    public void setServer(MinecraftServer server) {
        this.server = server;
    }

    private long getCurrentTick() {
        return server != null ? server.getTicks() : 0;
    }

    // Registration

    public void registerItem(Item item, ShiftHand hand, ShiftActivationHandler handler) {
        if (ShiftItemRegisterEvent.EVENT.invoker().onRegister(item, ShiftRegistrationType.ITEM, hand) == ActionResult.FAIL) return;
        itemHandlers.get(hand).put(item, handler);
    }

    public void registerTag(TagKey<Item> tag, ShiftHand hand, ShiftActivationHandler handler) {
        if (ShiftItemRegisterEvent.EVENT.invoker().onRegister(tag, ShiftRegistrationType.TAG, hand) == ActionResult.FAIL) return;
        tagHandlers.get(hand).put(tag, handler);
    }

    public void registerItemClass(Class<? extends Item> itemClass, ShiftHand hand, ShiftActivationHandler handler) {
        if (ShiftItemRegisterEvent.EVENT.invoker().onRegister(itemClass, ShiftRegistrationType.CLASS, hand) == ActionResult.FAIL) return;
        classHandlers.get(hand).put(itemClass, handler);
    }

    public void registerPredicate(Predicate<ItemStack> predicate, ShiftHand hand, ShiftActivationHandler handler) {
        if (ShiftItemRegisterEvent.EVENT.invoker().onRegister(predicate, ShiftRegistrationType.PREDICATE, hand) == ActionResult.FAIL) return;
        predicateHandlers.get(hand).add(new PredicateEntry(predicate, handler));
    }

    // Configuration

    public void setMaxProgress(int maxProgress) {
        this.maxProgress = maxProgress;
    }

    public int getMaxProgress() {
        return maxProgress;
    }

    public void setWindowTicks(int ticks) {
        this.windowTicks = ticks;
        playerTrackers.values().forEach(t -> t.updateWindowTicks(ticks));
    }

    public void setCooldownTicks(int ticks) {
        this.cooldownTicks = ticks;
        playerTrackers.values().forEach(t -> t.updateCooldownTicks(ticks));
    }

    // Core Logic

    public void handleSneak(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        long currentTick = getCurrentTick();

        PlayerShiftTracker tracker = playerTrackers.computeIfAbsent(uuid, u -> new PlayerShiftTracker(u, windowTicks, cooldownTicks));
        HandResult result = findHandler(player);
        if (result == null) return;

        int pressCount = tracker.recordPress(currentTick);
        if (pressCount == 0) return;

        if (pressCount >= maxProgress) {
            if (ShiftActivationEvent.EVENT.invoker().onActivate(player, result.stack, result.hand) == ActionResult.FAIL) return;

            if (result.handler.activate(player, result.stack, result.hand) == ActionResult.SUCCESS) {
                tracker.activate(currentTick);
                activeAbilities.put(uuid, new ActiveAbility(result.stack, result.hand));
                ActionBarHelper.sendActivationSuccess(player);

                System.out.println("[ChoLib] " + result.stack.getName().getString() + " shift ability activated for " + player.getName().getString());
            }
        } else {
            int percentage = (pressCount * 100) / maxProgress;
            if (ShiftProgressEvent.EVENT.invoker().onProgress(player, pressCount, maxProgress, percentage) == ActionResult.FAIL) return;
            ActionBarHelper.sendProgressBar(player, pressCount, maxProgress, percentage);
        }
    }

    public void checkItemSwap(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        ActiveAbility active = activeAbilities.get(uuid);
        if (active == null) return;

        ItemStack currentMain = player.getMainHandStack();
        ItemStack currentOff = player.getOffHandStack();
        boolean stillHolding;

        if (active.hand == Hand.MAIN_HAND) {
            stillHolding = ItemStack.areEqual(currentMain, active.stack);
        } else {
            stillHolding = ItemStack.areEqual(currentOff, active.stack);
        }

        if (!stillHolding) deactivate(uuid, ShiftDeactivationReason.ITEM_SWAP);
    }

    private HandResult findHandler(ServerPlayerEntity player) {
        ItemStack mainHand = player.getMainHandStack();
        if (!mainHand.isEmpty()) {
            ShiftActivationHandler handler = findHandlerForStack(mainHand, ShiftHand.MAIN_HAND);
            if (handler != null) return new HandResult(handler, mainHand, Hand.MAIN_HAND);
        }

        ItemStack offHand = player.getOffHandStack();
        if (!offHand.isEmpty()) {
            ShiftActivationHandler handler = findHandlerForStack(offHand, ShiftHand.OFF_HAND);
            if (handler != null) return new HandResult(handler, offHand, Hand.OFF_HAND);
        }

        return null;
    }

    private ShiftActivationHandler findHandlerForStack(ItemStack stack, ShiftHand hand) {
        Item item = stack.getItem();

        ShiftActivationHandler handler = itemHandlers.get(hand).get(item);
        if (handler != null) return handler;

        for (Map.Entry<TagKey<Item>, ShiftActivationHandler> entry : tagHandlers.get(hand).entrySet()) {
            if (stack.isIn(entry.getKey())) return entry.getValue();
        }

        for (Map.Entry<Class<? extends Item>, ShiftActivationHandler> entry : classHandlers.get(hand).entrySet()) {
            if (entry.getKey().isInstance(item)) return entry.getValue();
        }

        for (PredicateEntry entry : predicateHandlers.get(hand)) {
            if (entry.predicate.test(stack)) return entry.handler;
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

    // Deactivation

    public void deactivate(UUID uuid) {
        deactivate(uuid, ShiftDeactivationReason.MANUAL);
    }

    public void deactivate(UUID uuid, ShiftDeactivationReason reason) {
        ActiveAbility active = activeAbilities.remove(uuid);
        PlayerShiftTracker tracker = playerTrackers.get(uuid);
        if (tracker != null) tracker.reset();

        if (active != null && server != null) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
            if (player != null) ShiftDeactivationEvent.EVENT.invoker().onDeactivate(player, active.stack, active.hand, reason);
        }
    }

    public void deactivateAll() {
        for (UUID uuid : new ArrayList<>(activeAbilities.keySet())) {
            deactivate(uuid, ShiftDeactivationReason.MANUAL);
        }
    }

    public boolean isActive(UUID uuid) {
        return activeAbilities.containsKey(uuid);
    }

    // Cleanup

    public void onServerTick() {
        long currentTick = getCurrentTick();

        playerTrackers.entrySet().removeIf(entry -> {
            PlayerShiftTracker tracker = entry.getValue();
            UUID uuid = entry.getKey();

            if (tracker.hasShownBar() && !tracker.isWindowActive(currentTick)) {
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
                if (player != null) ActionBarHelper.sendActionBar(player, Text.literal(""));
            }

            return !activeAbilities.containsKey(uuid) && tracker.getPressCount(currentTick) == 0 && !tracker.isOnCooldown(currentTick);
        });
    }
}
