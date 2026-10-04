package dev.infinitedimensions.portal;

import com.mojang.serialization.Codec;
import dev.infinitedimensions.InfiniteDimensions;
import dev.infinitedimensions.ModBlocks;
import dev.infinitedimensions.NeitherPortalBlock;
import dev.infinitedimensions.dimension.DimensionHost;
import dev.infinitedimensions.dimension.DimensionIds;
import dev.infinitedimensions.dimension.DimensionRecipe;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Throw a written book (or book and quill) into a Nether portal: the portal becomes a Neither
 * portal linked to the dimension for that text, and the book is consumed. Standing in a linked
 * portal then teleports you there.
 *
 * Deliberately mixin-free: item entities are tracked through Fabric events, portal use is detected by
 * polling players once per tick. Nothing here touches rendering, chunk generation or ticking internals.
 */
public final class BookPortals {
    private static final int MAX_PORTAL_BLOCKS = 400;
    private static final int SURVIVAL_DELAY_TICKS = 80;
    private static final int COOLDOWN_TICKS = 200;

    private static final Codec<HashMap<String, Integer>> LINKS_CODEC = Codec.unboundedMap(Codec.STRING, Codec.INT)
            .xmap(HashMap<String, Integer>::new, map -> map);

    /** Portal block position (BlockPos.asLong as a string) -> dimension id, stored per level. */
    private static final AttachmentType<HashMap<String, Integer>> LINKS =
            AttachmentRegistry.<HashMap<String, Integer>>create(
                    InfiniteDimensions.id("portal_links"),
                    builder -> builder.persistent(LINKS_CODEC).initializer(HashMap::new));

    private static final Set<ItemEntity> TRACKED = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Integer> TICKS_INSIDE = new HashMap<>();
    private static final Map<UUID, Integer> COOLDOWN_UNTIL = new HashMap<>();

    private BookPortals() {}

    public static void init() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof ItemEntity item) TRACKED.add(item);
        });
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            if (entity instanceof ItemEntity item) TRACKED.remove(item);
        });
        ServerTickEvents.END_SERVER_TICK.register(BookPortals::tick);
    }

    private static void tick(MinecraftServer server) {
        tickItems();
        for (ServerLevel level : server.getAllLevels()) {
            for (ServerPlayer player : level.players()) {
                tickPlayer(server, level, player);
            }
        }
    }

    // ---- books -------------------------------------------------------------------------------

    private static void tickItems() {
        for (ItemEntity item : TRACKED) {
            if (item.isRemoved()) {
                TRACKED.remove(item);
                continue;
            }
            ItemStack stack = item.getItem();
            if (!DimensionIds.isBook(stack)) continue;
            if (!(item.level() instanceof ServerLevel level)) continue;

            BlockPos pos = item.blockPosition();
            if (!isPortal(level.getBlockState(pos))) continue;

            String text = DimensionIds.bookText(stack);
            if (text.isBlank()) continue; // leave empty books alone

            link(level, pos, DimensionIds.fromText(text));
            item.discard();
            TRACKED.remove(item);
        }
    }

    private static boolean isPortal(BlockState state) {
        return state.is(Blocks.NETHER_PORTAL) || state.is(ModBlocks.NEITHER_PORTAL);
    }

    /** Flood-fills the connected portal plane, converts it to Neither portal blocks and records the link. */
    private static void link(ServerLevel level, BlockPos start, int dimensionId) {
        Direction.Axis axis = level.getBlockState(start).getValue(BlockStateProperties.HORIZONTAL_AXIS);
        Direction along = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        Direction[] steps = {along, along.getOpposite(), Direction.UP, Direction.DOWN};

        Set<BlockPos> found = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        found.add(start);
        while (!queue.isEmpty() && found.size() < MAX_PORTAL_BLOCKS) {
            BlockPos current = queue.poll();
            for (Direction step : steps) {
                BlockPos next = current.relative(step);
                if (found.contains(next)) continue;
                BlockState state = level.getBlockState(next);
                if (isPortal(state) && state.getValue(BlockStateProperties.HORIZONTAL_AXIS) == axis) {
                    found.add(next.immutable());
                    queue.add(next);
                }
            }
        }

        HashMap<String, Integer> links = level.getAttachedOrCreate(LINKS);
        BlockState neither = ModBlocks.NEITHER_PORTAL.defaultBlockState().setValue(NeitherPortalBlock.AXIS, axis);
        // UPDATE_KNOWN_SHAPE + no neighbour updates: otherwise the vanilla portal blocks that haven't
        // been converted yet notice an "incomplete" portal and destroy themselves.
        int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        for (BlockPos pos : found) {
            level.setBlock(pos, neither, flags);
            links.put(Long.toString(pos.asLong()), dimensionId);
        }
        level.setAttached(LINKS, links);
        InfiniteDimensions.LOGGER.info("Linked {} portal blocks to dimension {}", found.size(), dimensionId);
    }

    // ---- players -----------------------------------------------------------------------------

    private static void tickPlayer(MinecraftServer server, ServerLevel level, ServerPlayer player) {
        UUID uuid = player.getUUID();
        BlockPos pos = player.blockPosition();
        if (!level.getBlockState(pos).is(ModBlocks.NEITHER_PORTAL)) {
            TICKS_INSIDE.remove(uuid);
            return;
        }
        if (COOLDOWN_UNTIL.getOrDefault(uuid, 0) > server.getTickCount()) return;

        int ticks = TICKS_INSIDE.merge(uuid, 1, Integer::sum);
        if (ticks < (player.isCreative() ? 1 : SURVIVAL_DELAY_TICKS)) return;
        TICKS_INSIDE.remove(uuid);

        Integer id = level.getAttachedOrCreate(LINKS).get(Long.toString(pos.asLong()));
        if (id == null) return; // unlinked (e.g. placed by command): does nothing

        DimensionRecipe recipe = DimensionRecipe.of(DimensionHost.worldSeed(server), id);
        ServerLevel target = DimensionHost.levelFor(server, recipe);
        DimensionHost.sendTo(player, target, recipe.style());
        COOLDOWN_UNTIL.put(uuid, server.getTickCount() + COOLDOWN_TICKS);
    }
}
