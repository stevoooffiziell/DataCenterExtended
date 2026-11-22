package de.f4ked.datacenterextended.economy;

import de.f4ked.datacenterextended.Config;
import de.f4ked.datacenterextended.hardware.ServerLayout;
import de.f4ked.datacenterextended.hardware.Wear;
import de.f4ked.datacenterextended.registry.ModDataMaps;
import de.f4ked.datacenterextended.registry.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Second-hand prices and repair costs (based on today's shop price, so old hardware is worth less) (used by the server and the Service tab, so both show the same numbers). Both
 * are based on the shop price: anything the shop doesn't sell can't be sold or repaired. A server item is valued as
 * its case plus all installed parts.
 */
public final class ItemValue {
    private ItemValue() {
    }

    /** Shop price of one item in cents at the calendar date, or -1 if the shop doesn't sell it. */
    public static long shopCents(Item item, double epochDay) {
        return ShopPricing.priceCents(item, epochDay);
    }

    /** Second-hand value of ONE item of the stack in cents, or -1 if it can't be sold. */
    public static long resaleCents(ItemStack stack, double epochDay) {
        if (stack.isEmpty()) return -1;
        long price = shopCents(stack.getItem(), epochDay);
        if (price < 0) return -1;
        double factor = Config.safe(Config.RESALE_FACTOR, 0.4);
        long value = (long) Math.floor(price * factor * Wear.condition(stack));
        if (stack.is(ModItems.SERVER.get())) {
            for (ItemStack part : ServerLayout.readParts(stack)) {
                long partValue = resaleCents(part, epochDay);
                if (partValue > 0) value += partValue * part.getCount();
            }
        }
        return value;
    }

    /** Repair cost of ONE item of the stack in cents: 0 if it isn't worn, -1 if it can't be repaired. */
    public static long repairCents(ItemStack stack, double epochDay) {
        if (stack.isEmpty()) return -1;
        if (stack.is(ModItems.SERVER.get())) {
            long total = 0;
            for (ItemStack part : ServerLayout.readParts(stack)) {
                long cost = repairCents(part, epochDay);
                if (cost > 0) total += cost * part.getCount();
            }
            return total;
        }
        float wear = Wear.get(stack);
        if (wear <= 0) return 0;
        long price = shopCents(stack.getItem(), epochDay);
        if (price < 0) return -1;
        return Math.max(1, (long) Math.ceil(price * wear * Config.safe(Config.REPAIR_COST, 0.5)));
    }

    /** Condition (0..1) shown for the stack: its own, or the worst part of a server. */
    public static double condition(ItemStack stack) {
        if (stack.is(ModItems.SERVER.get())) return Wear.worstCondition(ServerLayout.readParts(stack));
        return Wear.condition(stack);
    }

    /** Removes all wear from the stack (and from the parts of a server). */
    public static void repair(ItemStack stack) {
        if (stack.is(ModItems.SERVER.get())) {
            List<ItemStack> parts = ServerLayout.readParts(stack);
            for (ItemStack part : parts) Wear.set(part, 0);
            ServerLayout.writeParts(stack, parts);
        }
        Wear.set(stack, 0);
    }
}
