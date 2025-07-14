package com.scalpelred.harddrive;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;

import java.io.File;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class ReadFromWorldCommand {

    public final HardDrive hardDrive;
    private final WorldReader reader;

    String overwritePath = "";

    public ReadFromWorldCommand(HardDrive hardDrive) {

        this.hardDrive = hardDrive;
        this.reader = new WorldReader(hardDrive.IOManager);
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> dispatcher.register(literal("readfromworld")
                        .requires(source -> source.hasPermissionLevel(hardDrive.config.commandsPermissionLevel.getValue()))
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
        if (!src.isExecutedByPlayer()) return -1;
        synchronized (hardDrive.IOManager) {
            if (hardDrive.IOManager.isBusy()) {
                sendFeedback("Other operation is running right now.", src);
                return -1;
            }
            String path = StringArgumentType.getString(context, "file_path");
            if (reader.waitsForOverwriteConfirm() && path.equals(overwritePath)) {
                reader.confirmOverwrite();
                hardDrive.IOManager.startWorker(reader);
            } else {
                WorldAccess world = src.getWorld();
                BlockPos pos = BlockPosArgumentType.getBlockPos(context, "position");
                File file = new File(path);
                long length = hasImmediateLength ? LongArgumentType.getLong(context, "length") : -1;
                PlayerEntity player = (PlayerEntity) src.getEntity();
                if (reader.reset(world, pos, file, length, player)) {
                    overwritePath = path;
                } else hardDrive.IOManager.startWorker(reader);
            }
            return 1;
        }
    }

    private void sendFeedback(String msg, ServerCommandSource src) {
        src.sendFeedback(() -> Text.literal(msg), true);
    }
}
