package dev.infinitedimensions.dimension;

import com.mojang.serialization.Codec;
import dev.infinitedimensions.InfiniteDimensions;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeLevelConfig;

/**
 * The ONLY class that touches Fantasy. If Fantasy's 26.x method names differ from what is used in
 * {@link #open}, this is the one place to fix.
 */
public final class DimensionHost {
    private static final Codec<HashSet<Integer>> KNOWN_CODEC =
            Codec.INT.listOf().xmap(HashSet<Integer>::new, ArrayList<Integer>::new);

    /** Ids of every dimension ever opened in this world; reopened on server start. */
    private static final AttachmentType<HashSet<Integer>> KNOWN = AttachmentRegistry.<HashSet<Integer>>create(
            InfiniteDimensions.id("known_dimensions"),
            builder -> builder.persistent(KNOWN_CODEC).initializer(HashSet::new));

    private DimensionHost() {}

    public static void init() {
        ServerLifecycleEvents.SERVER_STARTED.register(DimensionHost::reopenKnown);
    }

    public static long worldSeed(MinecraftServer server) {
        return server.overworld().getSeed();
    }

    public static ResourceKey<Level> levelKey(int id) {
        return ResourceKey.create(Registries.DIMENSION, InfiniteDimensions.id("dim_" + id));
    }

    /** Returns the dimension for this id, creating it on first use. */
    public static ServerLevel levelFor(MinecraftServer server, DimensionRecipe recipe) {
        ServerLevel existing = server.getLevel(levelKey(recipe.id()));
        if (existing != null) return existing;

        ServerLevel created = open(server, recipe);
        ServerLevel overworld = server.overworld();
        HashSet<Integer> known = overworld.getAttachedOrCreate(KNOWN);
        if (known.add(recipe.id())) overworld.setAttached(KNOWN, known);
        return created;
    }

    private static ServerLevel open(MinecraftServer server, DimensionRecipe recipe) {
        Identifier key = InfiniteDimensions.id("dim_" + recipe.id());

        ServerLevel donor = server.getLevel(recipe.style().vanillaLevel);
        if (donor == null) donor = server.overworld();
        ChunkGenerator generator = donor.getChunkSource().getGenerator();

        RuntimeLevelConfig config = new RuntimeLevelConfig()
                .setDimensionType(recipe.style().dimensionType)
                .setGenerator(generator)
                .setSeed(recipe.seed());

        return Fantasy.get(server).getOrOpenPersistentLevel(key, config).asLevel();
    }

    private static void reopenKnown(MinecraftServer server) {
        Set<Integer> ids = new HashSet<>(server.overworld().getAttachedOrCreate(KNOWN));
        long seed = worldSeed(server);
        for (int id : ids) {
            try {
                levelFor(server, DimensionRecipe.of(seed, id));
            } catch (Exception e) {
                InfiniteDimensions.LOGGER.error("Could not reopen dimension {}", id, e);
            }
        }
    }

    // ---- teleporting -------------------------------------------------------------------------

    /** Sends the player to a safe spot near (0, 0), like /warp in the snapshot. */
    public static void sendTo(ServerPlayer player, ServerLevel target, DimensionStyle style) {
        Vec3 spot = safeSpot(target, style);
        player.teleportTo(target, spot.x, spot.y, spot.z, Set.of(), player.getYRot(), player.getXRot(), true);
    }

    private static Vec3 safeSpot(ServerLevel level, DimensionStyle style) {
        final int x = 0, z = 0;
        level.getChunk(x >> 4, z >> 4); // generates and loads the chunk synchronously

        int y = style == DimensionStyle.NETHER
                ? netherFloor(level, x, z)
                : level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

        if (y <= level.getMinY() + 1) {
            // Void (or nothing found): build a small obsidian platform like the End does.
            y = 64;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    level.setBlockAndUpdate(new BlockPos(x + dx, y - 1, z + dz), Blocks.OBSIDIAN.defaultBlockState());
                    for (int dy = 0; dy < 3; dy++) {
                        level.setBlockAndUpdate(new BlockPos(x + dx, y + dy, z + dz), Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }
        return new Vec3(x + 0.5, y, z + 0.5);
    }

    private static int netherFloor(ServerLevel level, int x, int z) {
        for (int y = 120; y > level.getMinY() + 1; y--) {
            BlockPos p = new BlockPos(x, y, z);
            if (level.getBlockState(p).isAir()
                    && level.getBlockState(p.above()).isAir()
                    && isSolidFloor(level, p.below())) {
                return y;
            }
        }
        return level.getMinY();
    }

    private static boolean isSolidFloor(ServerLevel level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        return !s.getCollisionShape(level, pos).isEmpty() && s.getFluidState().isEmpty();
    }
}
