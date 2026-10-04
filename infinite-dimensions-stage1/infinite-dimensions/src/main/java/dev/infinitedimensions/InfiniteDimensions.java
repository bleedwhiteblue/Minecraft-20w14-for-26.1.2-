package dev.infinitedimensions;

import dev.infinitedimensions.command.WarpCommand;
import dev.infinitedimensions.dimension.DimensionHost;
import dev.infinitedimensions.portal.BookPortals;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InfiniteDimensions implements ModInitializer {
    public static final String MOD_ID = "infinitedimensions";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ModBlocks.init();
        DimensionHost.init();
        BookPortals.init();
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> WarpCommand.register(dispatcher));
    }
}
