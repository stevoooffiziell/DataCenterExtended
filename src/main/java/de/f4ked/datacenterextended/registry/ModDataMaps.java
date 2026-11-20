package de.f4ked.datacenterextended.registry;

import com.mojang.serialization.Codec;
import com.mojang.datafixers.util.Either;
import de.f4ked.datacenterextended.Config;
import de.f4ked.datacenterextended.Datacenterextended;
import de.f4ked.datacenterextended.hardware.FileSize;
import de.f4ked.datacenterextended.hardware.HardwareStats;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.datamaps.AdvancedDataMapType;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.DataMapValueMerger;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;
import org.jspecify.annotations.Nullable;

public final class ModDataMaps {

    /** Component stats for any item. */
    public static final DataMapType<Item, HardwareStats> HARDWARE = AdvancedDataMapType
            .builder(Datacenterextended.id("hardware"), Registries.ITEM, HardwareStats.CODEC)
            .synced(HardwareStats.CODEC, false)
            .merger(ModDataMaps.specificityMerger())
            .build();

    /** File size (bytes needed to fabricate) for any item. */
    public static final DataMapType<Item, FileSize> FILE_SIZE = AdvancedDataMapType
            .builder(Datacenterextended.id("file_size"), Registries.ITEM, FileSize.CODEC)
            .synced(FileSize.NETWORK_CODEC, false)
            .merger(ModDataMaps.specificityMerger())
            .build();

    /** Hardware shop price in dollars for any item. Items without an entry are not sold. */
    public static final DataMapType<Item, Double> SHOP_PRICE = DataMapType
            .builder(Datacenterextended.id("shop_price"), Registries.ITEM, Codec.doubleRange(0.0, 1.0E12))
            .synced(Codec.doubleRange(0.0, 1.0E12), false)
            .build();

    /** Items in this tag are never fabricable. */
    public static final TagKey<Item> NOT_FABRICABLE = ItemTags.create(Datacenterextended.id("not_fabricable"));

    private ModDataMaps() {
    }

    public static void register(RegisterDataMapTypesEvent event) {
        event.register(HARDWARE);
        event.register(FILE_SIZE);
        event.register(SHOP_PRICE);
    }

    /** Shop price in dollars from the data map (no multiplier), or -1 if the item is not sold. */
    public static double shopPrice(Item item) {
        Double price = BuiltInRegistries.ITEM.wrapAsHolder(item).getData(SHOP_PRICE);
        return price == null ? -1 : price;
    }

    public static @Nullable HardwareStats hardware(ItemStack stack) {
        return stack.isEmpty() ? null : stack.typeHolder().getData(HARDWARE);
    }

    /**
     * @return bytes needed to fabricate one of the given item, or {@code -1} if it cannot be fabricated.
     */
    public static long fileSize(ItemStack stack) {
        if (stack.isEmpty() || stack.is(NOT_FABRICABLE) || Config.isBlacklisted(stack.getItem())) {
            return -1;
        }
        FileSize size = stack.typeHolder().getData(FILE_SIZE);
        if (size != null) {
            return size.bytes();
        }
        return Config.ALLOW_UNLISTED_ITEMS.get() ? Config.DEFAULT_FILE_SIZE.get() : -1;
    }

    /** Raw data map lookup, ignoring config (safe on the client, e.g. for tooltips). */
    public static long listedFileSize(ItemStack stack) {
        FileSize size = stack.isEmpty() ? null : stack.typeHolder().getData(FILE_SIZE);
        return size == null ? -1 : size.bytes();
    }

    /**
     * Resolves conflicts when an item receives values from several entries: a direct item id beats any tag and a
     * more specific tag ({@code c:ingots/iron}) beats a generic one ({@code c:ingots}). On a tie the later entry wins.
     * Entries with {@code "replace": true} always win.
     */
    private static <T> DataMapValueMerger<Item, T> specificityMerger() {
        return (registry, first, firstValue, second, secondValue) ->
                specificity(second) >= specificity(first) ? secondValue : firstValue;
    }

    private static int specificity(Either<TagKey<Item>, ResourceKey<Item>> source) {
        return source.map(tag -> 1 + (int) tag.location().getPath().chars().filter(c -> c == '/').count(), key -> 1000);
    }
}
