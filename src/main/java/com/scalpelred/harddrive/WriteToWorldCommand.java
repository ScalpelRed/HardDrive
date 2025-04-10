package com.scalpelred.harddrive;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.io.File;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class WriteToWorldCommand {

    private final HardDrive hardDrive;

    public WriteToWorldCommand(HardDrive hardDrive) {

        this.hardDrive = hardDrive;
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> dispatcher.register(
                        literal("writetoworld").requires(source -> source.hasPermissionLevel(0))
                                .then(argument("position", BlockPosArgumentType.blockPos())
                                        .then(argument("file_path", StringArgumentType.greedyString())
                                                .executes(this::execute)
                                        )
                                )
                )
        );
    }

    private int execute(CommandContext<ServerCommandSource> context) {

        ServerCommandSource src = context.getSource();
        String fileName = StringArgumentType.getString(context, "file_path");
        File file = new File(fileName);
        if (!file.exists()) {
            src.sendError(Text.literal("File \"" + fileName + "\" is missing."));
            return -1;
        }

        try {
            if (hardDrive.worldIO.WriteToWorld(
                    context.getSource().getWorld(),
                    BlockPosArgumentType.getBlockPos(context, "position"),
                    file
            )) {
                src.sendFeedback(() -> Text.literal("File \"" + fileName + "\" written."), true);
                return 1;
            }
            else {
                src.sendFeedback(() -> Text.literal("File \"" + fileName + "\" written, but there's not enough " +
                        "space in the area, written as much as possible."), true);
                return 2;
            }
        }
        catch (Exception e) {
            context.getSource().sendError(Text.literal("Exception thrown during writing: " + e.getMessage()));
            return -2;
        }
    }
}
