package de.f4ked.datacenterextended.recipe;

import com.mojang.serialization.MapCodec;
import de.f4ked.datacenterextended.Datacenterextended;
import de.f4ked.datacenterextended.StartupConfig;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Recipe condition {@code datacenterextended:part_recipes_enabled}: true unless part recipes are disabled in the startup config. */
public final class PartRecipesCondition implements ICondition {
    public static final PartRecipesCondition INSTANCE = new PartRecipesCondition();
    public static final MapCodec<PartRecipesCondition> CODEC = MapCodec.unit(INSTANCE);

    public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITION_CODECS =
            DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, Datacenterextended.MODID);

    static {
        CONDITION_CODECS.register("part_recipes_enabled", () -> CODEC);
    }

    private PartRecipesCondition() {
    }

    @Override
    public boolean test(IContext context) {
        try {
            return StartupConfig.ENABLE_PART_RECIPES.get();
        } catch (IllegalStateException e) {
            return true;
        }
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }
}
