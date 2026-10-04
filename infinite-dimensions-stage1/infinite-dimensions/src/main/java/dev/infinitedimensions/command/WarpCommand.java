package dev.infinitedimensions.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.infinitedimensions.dimension.DimensionHost;
import dev.infinitedimensions.dimension.DimensionIds;
import dev.infinitedimensions.dimension.DimensionRecipe;
import dev.infinitedimensions.dimension.DimensionStyle;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class WarpCommand {
    private WarpCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // No permission requirement: the snapshot's /warp worked without cheats.
        dispatcher.register(Commands.literal("warp")
                .then(Commands.argument("name", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            MinecraftServer server = ctx.getSource().getServer();
                            int id = DimensionIds.fromText(StringArgumentType.getString(ctx, "name"));
                            DimensionRecipe recipe = DimensionRecipe.of(DimensionHost.worldSeed(server), id);

                            ServerLevel target = DimensionHost.levelFor(server, recipe);
                            DimensionHost.sendTo(player, target, recipe.style());
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("Warped to dimension " + id + " (" + recipe.style() + ")"),
                                    false);
                            return 1;
                        })));

        // Not in the original: the snapshot had no way back by command.
        dispatcher.register(Commands.literal("infdim")
                .then(Commands.literal("home").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    MinecraftServer server = ctx.getSource().getServer();
                    DimensionHost.sendTo(player, server.overworld(), DimensionStyle.OVERWORLD);
                    return 1;
                })));
    }
}
