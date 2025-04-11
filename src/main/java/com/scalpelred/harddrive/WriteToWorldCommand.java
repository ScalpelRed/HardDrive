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

    private final int SUCCESS = 1;
    private final int NOT_ENOUGH_SPACE = 2;

    private final int EXCEPTION_THROWN = -1;
    private final int FILE_MISSING = -2;
    private final int CEILING_HIT = -3;

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
            return FILE_MISSING;
        }

        WorldIO.IOResult res;
        try {
            res = hardDrive.worldIO.WriteToWorld(
                    context.getSource().getWorld(),
                    BlockPosArgumentType.getBlockPos(context, "position"),
                    file);
        } catch (Exception e) {
            context.getSource().sendError(Text.literal("Exception thrown during writing: " + e.getMessage()));
            return EXCEPTION_THROWN;
        }

        switch (res) {
            case SUCCESS:
                src.sendFeedback(() -> Text.literal("File written."), true);
                return SUCCESS;
            case NOT_ENOUGH_SPACE:
                src.sendFeedback(() -> Text.literal(
                        "There's not enough space in the area, written as much as possible."), true);
                return NOT_ENOUGH_SPACE;
            case CEILING_HIT:
                String msg = "World upper border was reached while writing the file!";
                if (hardDrive.config.appendLength.getValue()) msg += " APPENDED LENGTH IS INVALID, reading the file may be problematic!";
                final String msg2 = msg;
                    src.sendFeedback(() -> Text.literal(msg2), true);
                return CEILING_HIT;
            default:
                return 0;
        }
    }
}
