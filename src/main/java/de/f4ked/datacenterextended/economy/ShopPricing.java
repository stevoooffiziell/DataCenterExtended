package de.f4ked.datacenterextended.economy;

import de.f4ked.datacenterextended.Config;
import de.f4ked.datacenterextended.item.PrebuiltServerItem;
import de.f4ked.datacenterextended.registry.ModDataMaps;
import de.f4ked.datacenterextended.registry.ModItems;
import net.minecraft.world.item.Item;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;

/**
 * Shop prices at a calendar date (shared by server and client). An item is sold from its release date on
 * ({@code datacenterextended:release_date}); afterwards its price drops every year down to a floor. Prebuilt servers
 * cost their parts plus the case, times the prebuilt markup.
 */
public final class ShopPricing {
    private ShopPricing() {
    }

    /** Price factor for an item released at {@code release}, {@code years} later (pure, unit tested). */
    public static double ageFactor(double years, double yearlyDrop, double floor) {
        if (years <= 0) return 1.0;
        return Math.max(floor, Math.pow(1.0 - yearlyDrop, years));
    }

    /** Release date, or null if the item is always available. Prebuilt servers: their newest part. */
    public static @Nullable LocalDate releaseDate(Item item) {
        if (item instanceof PrebuiltServerItem prebuilt) {
            LocalDate newest = null;
            for (Item part : prebuilt.prebuilt().parts()) {
                LocalDate date = ModDataMaps.releaseDate(part);
                if (date != null && (newest == null || date.isAfter(newest))) newest = date;
            }
            return newest;
        }
        return ModDataMaps.releaseDate(item);
    }

    public static boolean released(Item item, double epochDay) {
        if (!Config.safe(Config.RELEASE_DATES, true)) return true;
        LocalDate date = releaseDate(item);
        return date == null || epochDay >= date.toEpochDay();
    }

    /** Whether the shop sells the item at all (at some date). */
    public static boolean sold(Item item) {
        return item instanceof PrebuiltServerItem || ModDataMaps.shopPrice(item) >= 0;
    }

    /** Price of one item in cents at the calendar date (also for unreleased items, for display), -1 if not sold. */
    public static long priceCents(Item item, double epochDay) {
        if (item instanceof PrebuiltServerItem prebuilt) {
            long total = priceCents(ModItems.SERVER.get(), epochDay);
            for (Item part : prebuilt.prebuilt().parts()) {
                long price = priceCents(part, epochDay);
                if (price < 0) return -1;
                total += price;
            }
            return Math.round(total * Config.safe(Config.PREBUILT_MARKUP, 1.35));
        }
        double dollars = ModDataMaps.shopPrice(item);
        if (dollars < 0) return -1;
        double factor = 1.0;
        LocalDate release = ModDataMaps.releaseDate(item);
        if (release != null) {
            factor = ageFactor((epochDay - release.toEpochDay()) / 365.25, Config.safe(Config.YEARLY_PRICE_DROP, 0.15),
                    Config.safe(Config.MIN_PRICE_FACTOR, 0.3));
        }
        return Money.toCents(dollars * factor * Config.safe(Config.SHOP_PRICE_MULTIPLIER, 1.0));
    }
}
