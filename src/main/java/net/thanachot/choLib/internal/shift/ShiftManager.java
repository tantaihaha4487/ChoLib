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
import net.thanachot.choLib.api.ProgressBarProvider;
import java.util.function.Predicate;

public class ShiftManager {
    private static final String GLOBAL_MOD_ID = "global";
    private static ShiftManager instance;

    private final Map<String, ModSettings> modSettings;
    private final Map<ShiftHand, Map<Item, String>> itemModIds;
    private final Map<ShiftHand, Map<TagKey<Item>, String>> tagModIds;
    private final Map<ShiftHand, Map<Class<? extends Item>, String>> classModIds;
    private final Map<ShiftHand, List<PredicateModEntry>> predicateHandlers;

    private final Map<ModPlayerKey, PlayerShiftTracker> playerTrackers;
    private final Map<ModPlayerKey, ActiveAbility> activeAbilities;
    private final Map<ShiftActivationHandler, String> handlerModIds;
    private ProgressBarProvider defaultProgressBarProvider;
    private final Map<String, ProgressBarProvider> modProgressBarProviders;

    private MinecraftServer server;

    public static class ModSettings {
        int maxProgress = 10;
        int windowTicks = 60;
        int cooldownTicks = 40;

        ModSettings() {}
    }

    private static class PredicateModEntry {
        final Predicate<ItemStack> predicate;
        final ShiftActivationHandler handler;
        final String modId;

        PredicateModEntry(Predicate<ItemStack> predicate, ShiftActivationHandler handler, String modId) {
            this.predicate = predicate;
            this.handler = handler;
            this.modId = modId;
        }
    }

    private static class ActiveAbility {
        final ItemStack stack;
        final Hand hand;
        final String modId;

        ActiveAbility(ItemStack stack, Hand hand, String modId) {
            this.stack = stack;
            this.hand = hand;
            this.modId = modId;
        }
    }

    private ShiftManager() {
        modSettings = new HashMap<>();
        modSettings.put(GLOBAL_MOD_ID, new ModSettings());

        itemModIds = new EnumMap<>(ShiftHand.class);
        tagModIds = new EnumMap<>(ShiftHand.class);
        classModIds = new EnumMap<>(ShiftHand.class);
        predicateHandlers = new EnumMap<>(ShiftHand.class);

        playerTrackers = new HashMap<>();
        activeAbilities = new HashMap<>();
        handlerModIds = new HashMap<>();
        modProgressBarProviders = new HashMap<>();
        defaultProgressBarProvider = null;

        for (ShiftHand hand : ShiftHand.values()) {
            itemModIds.put(hand, new HashMap<>());
            tagModIds.put(hand, new HashMap<>());
            classModIds.put(hand, new HashMap<>());
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

    public void registerItem(String modId, Item item, ShiftHand hand, ShiftActivationHandler handler) {
        if (ShiftItemRegisterEvent.EVENT.invoker().onRegister(item, ShiftRegistrationType.ITEM, hand) == ActionResult.FAIL) return;
        itemModIds.get(hand).put(item, modId);
        handlerModIds.put(handler, modId);
        getOrCreateSettings(modId);
    }

    public void registerTag(String modId, TagKey<Item> tag, ShiftHand hand, ShiftActivationHandler handler) {
        if (ShiftItemRegisterEvent.EVENT.invoker().onRegister(tag, ShiftRegistrationType.TAG, hand) == ActionResult.FAIL) return;
        tagModIds.get(hand).put(tag, modId);
        handlerModIds.put(handler, modId);
        getOrCreateSettings(modId);
    }

    public void registerItemClass(String modId, Class<? extends Item> itemClass, ShiftHand hand, ShiftActivationHandler handler) {
        if (ShiftItemRegisterEvent.EVENT.invoker().onRegister(itemClass, ShiftRegistrationType.CLASS, hand) == ActionResult.FAIL) return;
        classModIds.get(hand).put(itemClass, modId);
        handlerModIds.put(handler, modId);
        getOrCreateSettings(modId);
    }

    public void registerPredicate(String modId, Predicate<ItemStack> predicate, ShiftHand hand, ShiftActivationHandler handler) {
        if (ShiftItemRegisterEvent.EVENT.invoker().onRegister(predicate, ShiftRegistrationType.PREDICATE, hand) == ActionResult.FAIL) return;
        predicateHandlers.get(hand).add(new PredicateModEntry(predicate, handler, modId));
        handlerModIds.put(handler, modId);
        getOrCreateSettings(modId);
    }

    // Backwards-compatible registration (uses global modId)

    public void registerItem(Item item, ShiftHand hand, ShiftActivationHandler handler) {
        registerItem(GLOBAL_MOD_ID, item, hand, handler);
    }

    public void registerTag(TagKey<Item> tag, ShiftHand hand, ShiftActivationHandler handler) {
        registerTag(GLOBAL_MOD_ID, tag, hand, handler);
    }

    public void registerItemClass(Class<? extends Item> itemClass, ShiftHand hand, ShiftActivationHandler handler) {
        registerItemClass(GLOBAL_MOD_ID, itemClass, hand, handler);
    }

    public void registerPredicate(Predicate<ItemStack> predicate, ShiftHand hand, ShiftActivationHandler handler) {
        registerPredicate(GLOBAL_MOD_ID, predicate, hand, handler);
    }

    // Configuration

    private ModSettings getOrCreateSettings(String modId) {
        return modSettings.computeIfAbsent(modId, k -> new ModSettings());
    }

    private ModSettings getSettings(String modId) {
        return modSettings.getOrDefault(modId, modSettings.get(GLOBAL_MOD_ID));
    }

    public void setMaxProgress(String modId, int maxProgress) {
        getOrCreateSettings(modId).maxProgress = maxProgress;
    }

    public int getMaxProgress(String modId) {
        return getSettings(modId).maxProgress;
    }

    public void setWindowTicks(String modId, int ticks) {
        getOrCreateSettings(modId).windowTicks = ticks;
    }

    public int getWindowTicks(String modId) {
        return getSettings(modId).windowTicks;
    }

    public void setCooldownTicks(String modId, int ticks) {
        getOrCreateSettings(modId).cooldownTicks = ticks;
    }

    public int getCooldownTicks(String modId) {
        return getSettings(modId).cooldownTicks;
    }

    // Backwards-compatible config (uses global modId)

    public void setMaxProgress(int maxProgress) {
        setMaxProgress(GLOBAL_MOD_ID, maxProgress);
    }

    public int getMaxProgress() {
        return getMaxProgress(GLOBAL_MOD_ID);
    }

    public void setWindowTicks(int ticks) {
        setWindowTicks(GLOBAL_MOD_ID, ticks);
    }

    public int getWindowTicks() {
        return getWindowTicks(GLOBAL_MOD_ID);
    }

    public void setCooldownTicks(int ticks) {
        setCooldownTicks(GLOBAL_MOD_ID, ticks);
    }

    public int getCooldownTicks() {
        return getCooldownTicks(GLOBAL_MOD_ID);
    }

    // Progress bar provider

    public void setProgressBarProvider(String modId, ProgressBarProvider provider) {
        modProgressBarProviders.put(modId, provider);
    }

    public ProgressBarProvider getProgressBarProvider(String modId) {
        return modProgressBarProviders.get(modId);
    }

    public void setProgressBarProvider(ProgressBarProvider provider) {
        this.defaultProgressBarProvider = provider;
    }

    public ProgressBarProvider getProgressBarProvider() {
        return defaultProgressBarProvider;
    }

    // Core Logic

    public void handleSneak(ServerPlayerEntity player) {
        UUID playerUuid = player.getUuid();
        long currentTick = getCurrentTick();

        HandResult result = findHandler(player);
        if (result == null) return;

        String modId = result.modId;
        ModSettings settings = getSettings(modId);
        ModPlayerKey key = new ModPlayerKey(modId, playerUuid);

        PlayerShiftTracker tracker = playerTrackers.computeIfAbsent(key, k -> new PlayerShiftTracker(playerUuid, settings.windowTicks, settings.cooldownTicks));

        int pressCount = tracker.recordPress(currentTick);
        if (pressCount == 0) return;

        if (pressCount >= settings.maxProgress) {
            if (ShiftActivationEvent.EVENT.invoker().onActivate(player, result.stack, result.hand) == ActionResult.FAIL) return;

            if (result.handler.activate(player, result.stack, result.hand) == ActionResult.SUCCESS) {
                tracker.activate(currentTick);
                activeAbilities.put(key, new ActiveAbility(result.stack, result.hand, modId));
                ActionBarHelper.sendActivationSuccess(player);

                System.out.println("[ChoLib] (" + modId + ") " + result.stack.getName().getString() + " shift ability activated for " + player.getName().getString());
            }
        } else {
            int percentage = (pressCount * 100) / settings.maxProgress;
            if (ShiftProgressEvent.EVENT.invoker().onProgress(player, pressCount, settings.maxProgress, percentage) == ActionResult.FAIL) return;

            ProgressBarProvider provider = getProgressBarProvider(modId);
            Text bar;
            if (provider != null) {
                bar = provider.buildBar(player, pressCount, settings.maxProgress);
            } else {
                bar = ActionBarHelper.buildProgressBarAsText(player, pressCount, settings.maxProgress);
            }
            ActionBarHelper.sendActionBar(player, bar);
        }
    }

    public void checkItemSwap(ServerPlayerEntity player) {
        UUID playerUuid = player.getUuid();

        for (Iterator<Map.Entry<ModPlayerKey, ActiveAbility>> it = activeAbilities.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<ModPlayerKey, ActiveAbility> entry = it.next();
            ModPlayerKey key = entry.getKey();

            if (!key.playerUuid().equals(playerUuid)) continue;

            ActiveAbility active = entry.getValue();
            ItemStack currentMain = player.getMainHandStack();
            ItemStack currentOff = player.getOffHandStack();
            boolean stillHolding;

            if (active.hand == Hand.MAIN_HAND) {
                stillHolding = ItemStack.areEqual(currentMain, active.stack);
            } else {
                stillHolding = ItemStack.areEqual(currentOff, active.stack);
            }

            if (!stillHolding) {
                it.remove();
                deactivateTracker(key);
                ShiftDeactivationEvent.EVENT.invoker().onDeactivate(player, active.stack, active.hand, ShiftDeactivationReason.ITEM_SWAP);
            }
        }
    }

    private HandResult findHandler(ServerPlayerEntity player) {
        ItemStack mainHand = player.getMainHandStack();
        if (!mainHand.isEmpty()) {
            HandlerResult hr = findHandlerForStack(mainHand, ShiftHand.MAIN_HAND);
            if (hr != null) return new HandResult(hr.handler, mainHand, Hand.MAIN_HAND, hr.modId);
        }

        ItemStack offHand = player.getOffHandStack();
        if (!offHand.isEmpty()) {
            HandlerResult hr = findHandlerForStack(offHand, ShiftHand.OFF_HAND);
            if (hr != null) return new HandResult(hr.handler, offHand, Hand.OFF_HAND, hr.modId);
        }

        return null;
    }

    private HandlerResult findHandlerForStack(ItemStack stack, ShiftHand hand) {
        Item item = stack.getItem();

        String modId = itemModIds.get(hand).get(item);
        if (modId != null) {
            ShiftActivationHandler handler = handlerModIds.entrySet().stream()
                .filter(e -> e.getValue().equals(modId))
                .findFirst()
                .map(Map.Entry::getKey)
                .orElse(null);
            if (handler != null) return new HandlerResult(handler, modId);
        }

        for (Map.Entry<TagKey<Item>, String> entry : tagModIds.get(hand).entrySet()) {
            if (stack.isIn(entry.getKey())) {
                String tagModId = entry.getValue();
                ShiftActivationHandler handler = handlerModIds.entrySet().stream()
                    .filter(e -> e.getValue().equals(tagModId))
                    .findFirst()
                    .map(Map.Entry::getKey)
                    .orElse(null);
                if (handler != null) return new HandlerResult(handler, tagModId);
            }
        }

        for (Map.Entry<Class<? extends Item>, String> entry : classModIds.get(hand).entrySet()) {
            if (entry.getKey().isInstance(item)) {
                String classModId = entry.getValue();
                ShiftActivationHandler handler = handlerModIds.entrySet().stream()
                    .filter(e -> e.getValue().equals(classModId))
                    .findFirst()
                    .map(Map.Entry::getKey)
                    .orElse(null);
                if (handler != null) return new HandlerResult(handler, classModId);
            }
        }

        for (PredicateModEntry entry : predicateHandlers.get(hand)) {
            if (entry.predicate.test(stack)) {
                return new HandlerResult(entry.handler, entry.modId);
            }
        }

        return null;
    }

    private static class HandlerResult {
        final ShiftActivationHandler handler;
        final String modId;

        HandlerResult(ShiftActivationHandler handler, String modId) {
            this.handler = handler;
            this.modId = modId;
        }
    }

    private static class HandResult {
        final ShiftActivationHandler handler;
        final ItemStack stack;
        final Hand hand;
        final String modId;

        HandResult(ShiftActivationHandler handler, ItemStack stack, Hand hand, String modId) {
            this.handler = handler;
            this.stack = stack;
            this.hand = hand;
            this.modId = modId;
        }
    }

    // Deactivation

    public void deactivate(String modId, UUID playerUuid) {
        ModPlayerKey key = new ModPlayerKey(modId, playerUuid);
        ActiveAbility active = activeAbilities.remove(key);
        if (active != null && server != null) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerUuid);
            if (player != null) ShiftDeactivationEvent.EVENT.invoker().onDeactivate(player, active.stack, active.hand, ShiftDeactivationReason.MANUAL);
        }
        deactivateTracker(key);
    }

    public void deactivate(UUID playerUuid) {
        deactivate(GLOBAL_MOD_ID, playerUuid);
    }

    public void deactivateAll(String modId) {
        for (Iterator<ModPlayerKey> it = activeAbilities.keySet().iterator(); it.hasNext(); ) {
            ModPlayerKey key = it.next();
            if (key.modId().equals(modId)) {
                it.remove();
                deactivateTracker(key);
            }
        }
    }

    public void deactivateAll() {
        deactivateAll(GLOBAL_MOD_ID);
    }

    private void deactivateTracker(ModPlayerKey key) {
        playerTrackers.remove(key);
    }

    public boolean isActive(String modId, UUID playerUuid) {
        return activeAbilities.containsKey(new ModPlayerKey(modId, playerUuid));
    }

    public boolean isActive(UUID playerUuid) {
        return isActive(GLOBAL_MOD_ID, playerUuid);
    }

    // Cleanup

    public void onServerTick() {
        long currentTick = getCurrentTick();

        for (Iterator<Map.Entry<ModPlayerKey, PlayerShiftTracker>> it = playerTrackers.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<ModPlayerKey, PlayerShiftTracker> entry = it.next();
            ModPlayerKey key = entry.getKey();
            PlayerShiftTracker tracker = entry.getValue();

            if (tracker.hasShownBar() && !tracker.isWindowActive(currentTick)) {
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(key.playerUuid());
                if (player != null) ActionBarHelper.sendActionBar(player, Text.literal(""));
            }

            boolean shouldRemove = !activeAbilities.containsKey(key) &&
                tracker.getPressCount(currentTick) == 0 &&
                !tracker.isOnCooldown(currentTick);

            if (shouldRemove) it.remove();
        }
    }
}
