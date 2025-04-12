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
import java.nio.file.Files;
import java.nio.file.Paths;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class ReadFromWorldCommand {

    private final HardDrive hardDrive;
    private File outputDir;

    private String overwriteAskName = "";
    private boolean overwriteAsk = false;

    public ReadFromWorldCommand(HardDrive hardDrive) {

        this.hardDrive = hardDrive;
        outputDir = Paths.get(System.getProperty("user.dir"), "harddrive_out").toFile();
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

        WorldIO.IOResult res;
        try {
            File file = new File(fileName);
            if (!file.isAbsolute()) {
                if (!outputDir.exists()) {
                    outputDir = Files.createDirectories(outputDir.toPath()).toFile().getCanonicalFile();
                }
                file = Paths.get(outputDir.getPath(), file.getPath()).toFile();
            }
            file = file.getCanonicalFile();

            if (!hardDrive.config.allowAnyPath.getValue()) {
                if (!file.getPath().startsWith(outputDir.getPath())) {
                    sendFeedback("ERROR: Access denied (use \"harddrive_out\" folder or it's subfolders)", src);
                    return -1;
                }
            }

            if (file.exists()) {
                // if we didn't ask about overwriting - we ask
                // if we asked, but the file is different - we ask again
                if (!overwriteAsk || !overwriteAskName.equals(fileName)) {
                    overwriteAskName = fileName;
                    overwriteAsk = true;
                    src.sendFeedback(() -> Text.literal("OVERWRITE PREVENTED: File already exists, run the command again to overwrite it."), true);
                    return -1;
                }
            }
            overwriteAsk = false;

            long length = hasImmediateLength ? LongArgumentType.getLong(context, "length") : -1;
            res = hardDrive.worldIO.readFromWorld(context.getSource().getWorld(), pos, file, length);
        } catch (Exception e) {
            sendFeedback("ERROR: Exception thrown during reading: " + e.getMessage(), src);
            return -1;
        }

        return switch (res) {
            case SUCCESS -> {
                sendFeedback("File has been read.", src);
                yield 1;
            }
            case CEILING_HIT -> {
                sendFeedback("ERROR: World upper border was reached while reading the file.", src);
                yield -1;
            }
            case NEGATIVE_APPENDED_LENGTH -> {
                sendFeedback("ERROR: Appended length is negative.", src);
                yield -1;
            }
            case NO_ANY_LENGTH -> {
                sendFeedback("ERROR: File length is not present neither as argument nor appended.", src);
                yield -1;
            }
            default -> 0;
        };
    }

    private void sendFeedback(String msg, ServerCommandSource src) {
        src.sendFeedback(() -> Text.literal(msg), true);
    }
}
