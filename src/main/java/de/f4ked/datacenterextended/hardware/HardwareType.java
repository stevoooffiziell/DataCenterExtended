package de.f4ked.datacenterextended.hardware;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum HardwareType implements StringRepresentable {
    MOTHERBOARD,
    CPU,
    GPU,
    PSU,
    STORAGE,
    MEMORY,
    /** A dedicated mining unit (e.g. an Antminer). Goes straight into a rack bay instead of a server; mines Bitcoin only. */
    ASIC;

    public static final Codec<HardwareType> CODEC = StringRepresentable.fromEnum(HardwareType::values);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String translationKey() {
        return "hardware.datacenterextended.type." + getSerializedName();
    }
}
