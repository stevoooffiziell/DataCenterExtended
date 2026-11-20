package de.f4ked.datacenterextended.hardware;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

/**
 * The "file size" of an item – how many bytes of compute are needed to fabricate one of it.
 * Attached via the {@code datacenterextended:file_size} data map. Accepts plain byte counts or
 * human readable strings: {@code 1048576}, {@code "1 MB"}, {@code "512KB"}, {@code "2.5 GB"}.
 */
public record FileSize(long bytes) {

    private static final Codec<FileSize> FROM_STRING = Codec.STRING.comapFlatMap(
            s -> {
                try {
                    long bytes = DataUnits.parseBytes(s);
                    return bytes > 0 ? DataResult.success(new FileSize(bytes)) : DataResult.error(() -> "File size must be positive: " + s);
                } catch (IllegalArgumentException e) {
                    return DataResult.error(e::getMessage);
                }
            },
            fs -> DataUnits.formatBytes(fs.bytes()));

    private static final Codec<FileSize> FROM_LONG = Codec.LONG.comapFlatMap(
            l -> l > 0 ? DataResult.success(new FileSize(l)) : DataResult.error(() -> "File size must be positive: " + l),
            FileSize::bytes);

    /** Decodes numbers or strings, encodes as a number. */
    public static final Codec<FileSize> CODEC = Codec.either(FROM_LONG, FROM_STRING)
            .xmap(e -> e.map(l -> l, s -> s), Either::left);

    /** Network codec – always a raw long. */
    public static final Codec<FileSize> NETWORK_CODEC = FROM_LONG;
}
