package de.f4ked.datacenterextended;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * {@code config/datacenterextended-startup.toml}: loaded at game start, before datapacks, so recipe conditions can
 * read it. Changes need a restart.
 */
public final class StartupConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLE_PART_RECIPES = BUILDER
            .comment("If false, hardware parts (CPUs, GPUs, motherboards, memory, PSUs, storage) have no crafting recipe",
                    "and can only be bought in the Hardware Shop.")
            .define("enablePartRecipes", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private StartupConfig() {
    }
}
