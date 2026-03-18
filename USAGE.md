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
        // Register diamond sword with shift ability
        ChoLibAPI.registerItem(
            Items.DIAMOND_SWORD,
            ShiftHand.MAIN_HAND,
            (player, stack, hand) -> {
                // Dash ability
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

### Method 1: Register Item
Register a specific item:
```java
ChoLibAPI.registerItem(
    Items.DIAMOND_SWORD,
    ShiftHand.MAIN_HAND,
    handler
);
```

### Method 2: Register Tag
Register all items in a tag:
```java
ChoLibAPI.registerTag(
    ItemTags.SWORDS,
    ShiftHand.MAIN_HAND,
    handler
);
```

### Method 3: Register Item Class
Register custom item classes:
```java
ChoLibAPI.registerItemClass(
    MyCustomSword.class,
    ShiftHand.MAIN_HAND,
    handler
);
```

### Method 4: Register Predicate
Register with custom conditions (component/data check):
```java
ChoLibAPI.registerPredicate(
    stack -> stack.isOf(Items.DIAMOND_SWORD) 
             && stack.getOrDefault(MyComponents.RARITY, 0) >= 5,
    ShiftHand.MAIN_HAND,
    handler
);
```

## Configuration

### Change Required Presses
```java
// Require 15 presses instead of default 10
ChoLibAPI.setMaxProgress(15);
```

### Change Timing
```java
// 5 second window (100 ticks at 20 TPS)
ChoLibAPI.setWindowTicks(100);

// 3 second cooldown (60 ticks)
ChoLibAPI.setCooldownTicks(60);
```

## Events

### Monitor Registration
```java
ShiftItemRegisterEvent.EVENT.register((registrant, type, hand) -> {
    System.out.println("Registered: " + type + " for hand " + hand);
    return ActionResult.PASS;
});
```

### Monitor Progress
```java
ShiftProgressEvent.EVENT.register((player, current, max, percent) -> {
    // Cancel sequence for certain conditions
    if (player.hasStatusEffect(StatusEffects.BLINDNESS)) {
        return ActionResult.FAIL;
    }
    return ActionResult.PASS;
});
```

### Monitor Activation
```java
ShiftActivationEvent.EVENT.register((player, stack, hand) -> {
    // Log activations
    System.out.println(player.getName() + " activated " + stack.getItem());
    return ActionResult.PASS;
});
```

### Handle Deactivation
```java
ShiftDeactivationEvent.EVENT.register((player, stack, hand, reason) -> {
    // Clean up effects when item is swapped or manually deactivated
    if (reason == ShiftDeactivationReason.ITEM_SWAP) {
        player.removeStatusEffect(StatusEffects.SPEED);
    }
    return ActionResult.PASS;
});
```

## Manual Deactivation

```java
// Deactivate specific player
ChoLibAPI.deactivate(player.getUuid());

// Deactivate all players
ChoLibAPI.deactivateAll();

// Check if active
boolean active = ChoLibAPI.isActive(player.getUuid());
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
3. **Progress is shown** on action bar (e.g., `█████░░░░░ 50%`)
4. **At threshold** (default: 10 presses), ability activates
5. **Cooldown begins** (default: 2s), ignoring further shifts
6. **Auto-deactivation** when player swaps items

## Example: Complete Ability

```java
public class MyMod implements ModInitializer {
    
    @Override
    public void onInitialize() {
        // Configure
        ChoLibAPI.setMaxProgress(8);  // Faster activation
        
        // Register legendary sword
        ChoLibAPI.registerPredicate(
            stack -> stack.isOf(Items.DIAMOND_SWORD) 
                     && stack.getName().getString().contains("Legendary"),
            ShiftHand.MAIN_HAND,
            (player, stack, hand) -> {
                // Powerful ability
                player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.STRENGTH, 200, 2
                ));
                player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.RESISTANCE, 200, 1
                ));
                
                // Visual effects
                player.getWorld().playSound(
                    null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BLOCK_BEACON_ACTIVATE,
                    SoundCategory.PLAYERS, 1.0f, 1.5f
                );
                
                return ActionResult.SUCCESS;
            }
        );
        
        // Handle deactivation
        ShiftDeactivationEvent.EVENT.register((player, stack, hand, reason) -> {
            player.removeStatusEffect(StatusEffects.STRENGTH);
            player.removeStatusEffect(StatusEffects.RESISTANCE);
            return ActionResult.PASS;
        });
    }
}
```

## Tips

1. **Specific Hand**: Always specify `MAIN_HAND` or `OFF_HAND` - system checks main hand first
2. **Handler Return**: Return `ActionResult.SUCCESS` to consume event, `ActionResult.PASS` to allow others
3. **Performance**: Predicates are evaluated every shift press - keep them fast
4. **Persistence**: Progress doesn't persist across item swaps or server restarts
5. **Canceling**: Use `ShiftProgressEvent` to cancel sequences based on conditions

## Support

For issues or questions, visit: https://github.com/thanachot/cholib/issues
