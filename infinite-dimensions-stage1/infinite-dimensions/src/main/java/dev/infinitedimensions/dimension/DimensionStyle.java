package dev.infinitedimensions.dimension;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;

/**
 * Stage 1 reuses the three vanilla dimension types and vanilla chunk generators, so generation
 * is the exact code path Lithium/C2ME/etc. already optimise. Stage 2 adds per-dimension block
 * swaps, biomes, mobs and sky parameters on top.
 */
public enum DimensionStyle {
    OVERWORLD(BuiltinDimensionTypes.OVERWORLD, Level.OVERWORLD, 50),
    NETHER(BuiltinDimensionTypes.NETHER, Level.NETHER, 25),
    END(BuiltinDimensionTypes.END, Level.END, 25);

    public final ResourceKey<DimensionType> dimensionType;
    /** The vanilla level whose chunk generator we borrow. */
    public final ResourceKey<Level> vanillaLevel;
    public final int weight;

    DimensionStyle(ResourceKey<DimensionType> dimensionType, ResourceKey<Level> vanillaLevel, int weight) {
        this.dimensionType = dimensionType;
        this.vanillaLevel = vanillaLevel;
        this.weight = weight;
    }

    public static int totalWeight() {
        int sum = 0;
        for (DimensionStyle s : values()) sum += s.weight;
        return sum;
    }
}
