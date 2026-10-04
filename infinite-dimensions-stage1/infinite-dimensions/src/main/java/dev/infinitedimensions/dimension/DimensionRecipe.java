package dev.infinitedimensions.dimension;

import java.util.SplittableRandom;

/**
 * Everything about a dimension that is derived from (world seed, dimension id). Pure and
 * immutable: safe to call from any thread, which matters for parallel chunk-generation mods.
 * Stage 2 will add tint, sky, block swaps, biome table, mob table etc. as further fields,
 * all drawn from the same SplittableRandom so existing fields never change meaning.
 */
public record DimensionRecipe(int id, long seed, DimensionStyle style) {

    public static DimensionRecipe of(long worldSeed, int id) {
        long mixed = splitMix(worldSeed ^ (id * 0x9E3779B97F4A7C15L));
        SplittableRandom rng = new SplittableRandom(mixed);

        DimensionStyle style = DimensionStyle.OVERWORLD;
        int roll = rng.nextInt(DimensionStyle.totalWeight());
        for (DimensionStyle candidate : DimensionStyle.values()) {
            if (roll < candidate.weight) {
                style = candidate;
                break;
            }
            roll -= candidate.weight;
        }
        return new DimensionRecipe(id, rng.nextLong(), style);
    }

    private static long splitMix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
