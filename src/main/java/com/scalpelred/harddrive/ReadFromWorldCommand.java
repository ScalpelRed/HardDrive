package com.scalpelred.harddrive;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.io.File;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class ReadFromWorldCommand {

    private final int SUCCESS = 1;
    private final int OVERWRITE_WARNING = 2;

    private final int EXCEPTION_THROWN = -1;
    private final int CEILING_HIT = -2;
    private final int NEGATIVE_APPENDED_LENGTH = -3;
    private final int NO_ANY_LENGTH = -4;

    private final HardDrive hardDrive;

    private String overwriteAskName = "";
    private boolean overwriteAsk = false;

    public ReadFromWorldCommand(HardDrive hardDrive) {

        this.hardDrive = hardDrive;
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> dispatcher.register(literal("readfromworld")
                        .requires(source -> source.hasPermissionLevel(4))
                        .then(argument("position", BlockPosArgumentType.blockPos())
                                .then(argument("length", LongArgumentType.longArg())
                                        .then(argument("file_path", StringArgumentType.greedyString())
                                                .executes(context -> execute(context, true))
                                        )
                                ).then(argument("file_path", StringArgumentType.greedyString())
                                        .executes(context -> execute(context, false))
                                )
                        )
                )
        );
    }

    private int execute(CommandContext<ServerCommandSource> context, boolean hasImmediateLength) {

        ServerCommandSource src = context.getSource();
        String fileName = StringArgumentType.getString(context, "file_path");
        BlockPos pos = BlockPosArgumentType.getBlockPos(context, "position");
        File file = new File(fileName);
        if (file.exists()) {
            // if we didn't ask about overwriting - we ask
            // if we asked, but the file is different - we ask again
            if (!overwriteAsk || !overwriteAskName.equals(fileName)) {
                overwriteAskName = fileName;
                overwriteAsk = true;
                src.sendFeedback(() -> Text.literal("OVERWRITE PREVENTED: File already exists, run the command again to overwrite it."), true);
                return OVERWRITE_WARNING;
            }
        }
        overwriteAsk = false;

        WorldIO.IOResult res;
        try {
            long length = hasImmediateLength ? LongArgumentType.getLong(context, "length") : -1;
            res = hardDrive.worldIO.readFromWorld(context.getSource().getWorld(), pos, file, length);
        } catch (Exception e) {
            context.getSource().sendFeedback(() -> Text.literal("ERROR: Exception thrown during writing: " + e.getMessage()), true);
            return EXCEPTION_THROWN;
        }

        return switch (res) {
            case SUCCESS -> {
                src.sendFeedback(() -> Text.literal("File has been read."), true);
                yield SUCCESS;
            }
            case CEILING_HIT -> {
                src.sendFeedback(() -> Text.literal("ERROR: World upper border was reached while reading the file."), true);
                yield CEILING_HIT;
            }
            case NEGATIVE_APPENDED_LENGTH -> {
                src.sendFeedback(() -> Text.literal("ERROR: Appended length is negative."), true);
                yield NEGATIVE_APPENDED_LENGTH;
            }
            case NO_ANY_LENGTH -> {
                src.sendFeedback(() -> Text.literal("ERROR: File length is not present neither as argument nor appended."), true);
                yield NO_ANY_LENGTH;
            }
            default -> 0;
        };
    }
}
