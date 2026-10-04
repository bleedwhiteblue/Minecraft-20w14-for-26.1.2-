package dev.infinitedimensions;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;

public final class ModBlocks {
    private static final ResourceKey<Block> NEITHER_PORTAL_KEY =
            ResourceKey.create(Registries.BLOCK, InfiniteDimensions.id("neither_portal"));

    /** The portal block a Nether portal turns into once a book has been thrown in. No item form. */
    public static final Block NEITHER_PORTAL = Registry.register(
            BuiltInRegistries.BLOCK,
            NEITHER_PORTAL_KEY,
            new NeitherPortalBlock(BlockBehaviour.Properties.of()
                    .setId(NEITHER_PORTAL_KEY)
                    .noCollision()
                    .noOcclusion()
                    .strength(-1.0F)
                    .lightLevel(state -> 11)
                    .sound(SoundType.GLASS)
                    .pushReaction(PushReaction.BLOCK)));

    private ModBlocks() {}

    /** Forces class loading so the static registration above runs at mod init. */
    public static void init() {}
}
