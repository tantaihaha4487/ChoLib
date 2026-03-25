# ChoLib

Minecraft Fabric library for shift (sneak) activation systems
Rapidly press shift while holding registered items to trigger abilities

## Quick Start

Add dependency to `build.gradle`

```gradle
dependencies {
    modImplementation "net.thanachot:cholib:${project.cholib_version}"
}
```

Register items in `onInitialize()`

```java
ChoLibAPI.registerItem(
    "mymod",
    Items.DIAMOND_SWORD,
    ShiftHand.MAIN_HAND,
    (player, stack, hand) -> {
        Vec3d look = player.getRotationVector();
        player.addVelocity(look.x * 2, 0.5, look.z * 2);
        
        player.getWorld().playSound(
            null,
            player.getX(), player.getY(), player.getZ(),
            SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,
            SoundCategory.PLAYERS,
            1.0f, 1.0f
        );

        return ActionResult.SUCCESS;
    }
);
```

## Registration Methods

Use unique `modId` strings to isolate configurations

```java
ChoLibAPI.registerItem("mymod", Items.DIAMOND_SWORD, ShiftHand.MAIN_HAND, handler);

ChoLibAPI.registerTag("mymod", ItemTags.SWORDS, ShiftHand.MAIN_HAND, handler);

ChoLibAPI.registerItemClass("mymod", MyCustomSword.class, ShiftHand.MAIN_HAND, handler);

ChoLibAPI.registerPredicate(
    "mymod",
    stack -> stack.isOf(Items.DIAMOND_SWORD) && stack.getOrDefault(MyComponents.RARITY, 0) >= 5,
    ShiftHand.MAIN_HAND,
    handler
);
```

## Configuration

```java
ChoLibAPI.setMaxProgress("mymod", 15);
ChoLibAPI.setWindowTicks("mymod", 100);
ChoLibAPI.setCooldownTicks("mymod", 60);

int presses = ChoLibAPI.getMaxProgress("mymod");
```

## Events

```java
ShiftProgressEvent.EVENT.register((player, current, max, percentage) -> {
    if (player.hasStatusEffect(StatusEffects.BLINDNESS)) {
        return ActionResult.FAIL;
    }
    return ActionResult.PASS;
});

ShiftActivationEvent.EVENT.register((player, stack, hand) -> {
    return ActionResult.PASS;
});

ShiftDeactivationEvent.EVENT.register((player, stack, hand, reason) -> {
    if (reason == ShiftDeactivationReason.ITEM_SWAP) {
        player.removeStatusEffect(StatusEffects.SPEED);
    }
    return ActionResult.PASS;
});
```

## Manual Control

```java
ChoLibAPI.deactivate("mymod", player.getUuid());
ChoLibAPI.deactivateAll("mymod");
boolean active = ChoLibAPI.isActive("mymod", player.getUuid());
```

## Default Settings

| Setting | Default | Description |
|---|---|---|
| Max Progress | 10 | Shifts required for activation |
| Window | 60 ticks | Time limit for shift count |
| Cooldown | 40 ticks | Delay after success |

## Custom Progress Bars

Override default action bar display

```java
ChoLibAPI.setProgressBarProvider("mymod", (player, current, max) -> {
    int pct = (max == 0) ? 0 : (current * 100) / max;
    int filled = pct / 10;
    int empty = 10 - filled;

    StringBuilder bar = new StringBuilder();
    for (int i = 0; i < filled; i++) bar.append("═");
    for (int i = 0; i < empty; i++) bar.append("─");

    return Text.literal("╞" + bar + "╡ " + pct + "%").formatted(Formatting.GREEN);
});
```

Produces -> `╞═══════───╡ 70%`

## Handler Return Values

* `ActionResult.SUCCESS` -> Action succeeds, cooldown begins
* `ActionResult.PASS` -> Action ignored, sequence continues
* `ActionResult.FAIL` -> Cancels sequence entirely

---

## Documentation

╭────── · · ୨୧ · · ──────╮
-> [Full Web Documentation](https://tantaihaha4487.github.io/ChoLib-Docs/)
╰────── · · ୨୧ · · ──────╯

