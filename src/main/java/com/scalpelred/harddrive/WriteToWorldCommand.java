package com.scalpelred.harddrive;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class WriteToWorldCommand {

    private final HardDrive hardDrive;
    private File inputDir;

    public WriteToWorldCommand(HardDrive hardDrive) {

        this.hardDrive = hardDrive;
        inputDir = Paths.get(System.getProperty("user.dir"), "harddrive_in").toFile();
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> dispatcher.register(literal("writetoworld")
                        .requires(source -> source.hasPermissionLevel(4))
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

        WorldIO.IOResult res;
        try {
            File file = new File(fileName);
            if (!file.isAbsolute()) {
                if (!inputDir.exists()) {
                    inputDir = Files.createDirectories(inputDir.toPath()).toFile().getCanonicalFile();
                }
                file = Paths.get(inputDir.getPath(), file.getPath()).toFile();
            }
            file = file.getCanonicalFile();

            if (!hardDrive.config.allowAnyPath.getValue()) {
                if (!file.getPath().startsWith(inputDir.getPath())) {
                    sendFeedback("ERROR: Access denied (use \"harddrive_in\" folder or it's subfolders)", src);
                    return -1;
                }
            }

            if (!file.exists()) {
                sendFeedback("ERROR: File is missing.", src);
                return -1;
            }

            res = hardDrive.worldIO.writeToWorld(
                    context.getSource().getWorld(),
                    BlockPosArgumentType.getBlockPos(context, "position"),
                    file);
        } catch (Exception e) {
            sendFeedback("ERROR: Exception thrown during writing: " + e.getMessage(), src);
            return -1;
        }

        switch (res) {
            case SUCCESS:
                sendFeedback("File written.", src);
                return 1;
            case NOT_ENOUGH_SPACE:
                sendFeedback("ERROR: There's not enough space in the area, written as much as possible.", src);
                return -1;
            case CEILING_HIT:
                String msg = "ERROR: World upper border was reached while writing the file.";
                if (hardDrive.config.appendLength.getValue())
                    msg += " APPENDED LENGTH IS INVALID, reading the file may be problematic.";
                sendFeedback(msg, src);
                return -1;
            default:
                return 0;
        }
    }

    private void sendFeedback(String msg, ServerCommandSource src) {
        src.sendFeedback(() -> Text.literal(msg), true);
    }
}