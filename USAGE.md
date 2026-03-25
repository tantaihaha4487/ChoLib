# ChoLib Shift Mechanism - Usage Guide

## Overview
ChoLib provides an easy-to-use shift (sneak) activation system for Minecraft Fabric mods. Players rapidly press shift while holding registered items to activate special abilities.

## Quick Start

### 1. Add Dependency
Add ChoLib to your `build.gradle`:
```gradle
dependencies {
    modImplementation "net.thanachot:cholib:${project.cholib_version}"
}
```

### 2. Register Items
In your mod's `onInitialize()`:

```java
import net.thanachot.choLib.api.ChoLibAPI;
import net.thanachot.choLib.api.ShiftHand;
import net.minecraft.item.Items;

public class MyMod implements ModInitializer {
    @Override
    public void onInitialize() {
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
    }
}
```

## Registration Methods

### Register Item
```java
ChoLibAPI.registerItem(
    "mymod",
    Items.DIAMOND_SWORD,
    ShiftHand.MAIN_HAND,
    handler
);
```

### Register Tag
```java
ChoLibAPI.registerTag(
    "mymod",
    ItemTags.SWORDS,
    ShiftHand.MAIN_HAND,
    handler
);
```

### Register Item Class
```java
ChoLibAPI.registerItemClass(
    "mymod",
    MyCustomSword.class,
    ShiftHand.MAIN_HAND,
    handler
);
```

### Register Predicate
```java
ChoLibAPI.registerPredicate(
    "mymod",
    stack -> stack.isOf(Items.DIAMOND_SWORD)
             && stack.getOrDefault(MyComponents.RARITY, 0) >= 5,
    ShiftHand.MAIN_HAND,
    handler
);
```

## The modId Parameter

Every registration method takes a modId as the first parameter. This isolates your registrations from other mods using ChoLib, so your settings don't conflict with theirs.

## Configuration

```java
// Require 15 presses for mymod
ChoLibAPI.setMaxProgress("mymod", 15);

// 5 second window (100 ticks)
ChoLibAPI.setWindowTicks("mymod", 100);

// 3 second cooldown (60 ticks)
ChoLibAPI.setCooldownTicks("mymod", 60);
```

Reading values:
```java
int presses = ChoLibAPI.getMaxProgress("mymod");
```

## Events

### Monitor Progress
```java
ShiftProgressEvent.EVENT.register((player, current, max, percentage) -> {
    if (player.hasStatusEffect(StatusEffects.BLINDNESS)) {
        return ActionResult.FAIL;
    }
    return ActionResult.PASS;
});
```

### Monitor Activation
```java
ShiftActivationEvent.EVENT.register((player, stack, hand) -> {
    return ActionResult.PASS;
});
```

### Handle Deactivation
```java
ShiftDeactivationEvent.EVENT.register((player, stack, hand, reason) -> {
    if (reason == ShiftDeactivationReason.ITEM_SWAP) {
        player.removeStatusEffect(StatusEffects.SPEED);
    }
    return ActionResult.PASS;
});
```

## Manual Deactivation

```java
ChoLibAPI.deactivate("mymod", player.getUuid());
ChoLibAPI.deactivateAll("mymod");
boolean active = ChoLibAPI.isActive("mymod", player.getUuid());
```

## Default Settings

| Setting | Default | Description |
|---------|---------|-------------|
| Max Progress | 10 presses | Shift presses required for activation |
| Window | 60 ticks (3s) | Time window to count shifts |
| Cooldown | 40 ticks (2s) | Cooldown after successful activation |

## How It Works

1. **Player presses shift** while holding registered item
2. **Press is recorded** in a sliding time window
3. **Progress is shown** on action bar (e.g., `╞▰════════╡ 50%`)
4. **At threshold** (default: 10 presses), ability activates
5. **Cooldown begins** (default: 2s), ignoring further shifts
6. **Auto-deactivation** when player swaps items

## Example: Complete Ability

```java
public class MyMod implements ModInitializer {

    @Override
    public void onInitialize() {
        ChoLibAPI.setMaxProgress("mymod", 8);

        ChoLibAPI.registerPredicate(
            "mymod",
            stack -> stack.isOf(Items.DIAMOND_SWORD)
                     && stack.getName().getString().contains("Legendary"),
            ShiftHand.MAIN_HAND,
            (player, stack, hand) -> {
                player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.STRENGTH, 200, 2
                ));
                player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.RESISTANCE, 200, 1
                ));

                player.getWorld().playSound(
                    null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BLOCK_BEACON_ACTIVATE,
                    SoundCategory.PLAYERS, 1.0f, 1.5f
                );

                return ActionResult.SUCCESS;
            }
        );

        ShiftDeactivationEvent.EVENT.register((player, stack, hand, reason) -> {
            player.removeStatusEffect(StatusEffects.STRENGTH);
            player.removeStatusEffect(StatusEffects.RESISTANCE);
            return ActionResult.PASS;
        });
    }
}
```

## Handler Return Value

The handler receives a vanilla `Hand` (not `ShiftHand`):

- `ActionResult.SUCCESS` - activation succeeded, cooldown begins
- `ActionResult.PASS` - activation did nothing, sequence continues
- `ActionResult.FAIL` - cancels the sequence entirely

## Tips

1. **Use modId consistently**: All registrations and config for your mod should use the same modId
2. **Predicates are fast**: They're evaluated every shift press
3. **Progress doesn't persist**: Item swaps or server restarts reset progress
4. **Cancel mid-sequence**: Use `ShiftProgressEvent` to cancel based on conditions

## Support

For issues or questions, visit: https://github.com/thanachot/cholib/issues