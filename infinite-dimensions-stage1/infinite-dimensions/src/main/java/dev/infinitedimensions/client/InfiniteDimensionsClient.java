package dev.infinitedimensions.client;

import dev.infinitedimensions.ModBlocks;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

public class InfiniteDimensionsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Uses Fabric's public API (no renderer mixins), so Sodium/Iris see a normal translucent block.
        // If this line fails to compile, the class/method was renamed in 26.1: check the Fabric API
        // porting guide. Deleting it only makes the portal render less transparent.
        BlockRenderLayerMap.putBlock(ModBlocks.NEITHER_PORTAL, ChunkSectionLayer.TRANSLUCENT);
    }
}
