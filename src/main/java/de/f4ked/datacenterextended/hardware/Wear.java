package de.f4ked.datacenterextended.hardware;

import de.f4ked.datacenterextended.Config;
import de.f4ked.datacenterextended.registry.ModDataComponents;
import de.f4ked.datacenterextended.registry.ModDataMaps;
import net.minecraft.world.item.ItemStack;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Hardware wear, stored on part items in the {@code datacenterextended:wear} component (0 = new, 1 = broken). New parts
 * carry no component, so they keep stacking with each other.
 */
public final class Wear {
    /** A part at this wear (or more) is broken. */
    public static final float BROKEN = 1.0F;

    private Wear() {
    }

    public static float get(ItemStack stack) {
        Float wear = stack.get(ModDataComponents.WEAR.get());
        return wear == null ? 0.0F : wear;
    }

    /** 1.0 = new, 0.0 = broken. */
    public static double condition(ItemStack stack) {
        return 1.0 - get(stack);
    }

    public static boolean isBroken(ItemStack stack) {
        return !stack.isEmpty() && get(stack) >= BROKEN;
    }

    public static void set(ItemStack stack, float wear) {
        if (wear <= 0) {
            stack.remove(ModDataComponents.WEAR.get());
        } else {
            stack.set(ModDataComponents.WEAR.get(), Math.min(BROKEN, wear));
        }
    }

    /** Adds wear; returns true if the part just broke. */
    public static boolean add(ItemStack stack, double amount) {
        if (stack.isEmpty() || amount <= 0 || isBroken(stack)) return false;
        float next = (float) Math.min(BROKEN, get(stack) + amount);
        set(stack, next);
        return next >= BROKEN;
    }

    /** Speed multiplier of a CPU/GPU/ASIC with this wear (1.0 when new). */
    public static double performance(ItemStack stack) {
        double min = Config.safe(Config.WORN_MIN_PERFORMANCE, 0.7);
        return min + (1.0 - min) * condition(stack);
    }

    /**
     * Something the owner should hear about: a part dropped below the warning threshold or broke.
     *
     * @param part      a copy of the part after wearing
     * @param condition its new condition (0..1)
     * @param broke     true if it just broke (otherwise it crossed the warning threshold)
     */
    public record WearEvent(ItemStack part, double condition, boolean broke) {
    }

    /** Wears one part / ASIC; returns an event if it broke or crossed {@code threshold}, else null. */
    public static @Nullable WearEvent wear(ItemStack stack, double amount, double threshold) {
        if (stack.isEmpty() || isBroken(stack)) return null;
        double before = condition(stack);
        boolean broke = add(stack, amount);
        double after = condition(stack);
        if (broke) return new WearEvent(stack.copyWithCount(1), 0, true);
        if (before > threshold && after <= threshold) return new WearEvent(stack.copyWithCount(1), after, false);
        return null;
    }

    /** Wears every hardware part of a server item; returns the warnings (empty if nothing notable happened). */
    public static List<WearEvent> wearServer(ItemStack server, double amount, double threshold) {
        List<ItemStack> parts = ServerLayout.readParts(server);
        List<WearEvent> events = new ArrayList<>();
        boolean changed = false;
        for (ItemStack part : parts) {
            if (part.isEmpty() || ModDataMaps.hardware(part) == null || isBroken(part)) continue;
            WearEvent event = wear(part, amount, threshold);
            if (event != null) events.add(event);
            changed = true;
        }
        if (changed) ServerLayout.writeParts(server, parts);
        return events;
    }

    /** Wears every hardware part of a server item (no warning threshold). */
    public static List<WearEvent> wearServer(ItemStack server, double amount) {
        return wearServer(server, amount, 0);
    }

    /** Lowest condition (0..1) among the parts of a server item, 1 if it has none. */
    public static double worstCondition(List<ItemStack> parts) {
        double worst = 1.0;
        for (ItemStack part : parts) {
            if (!part.isEmpty()) worst = Math.min(worst, condition(part));
        }
        return worst;
    }
}
