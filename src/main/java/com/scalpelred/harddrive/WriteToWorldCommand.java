package com.scalpelred.harddrive;

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

public class WriteToWorldCommand {

    public final HardDrive hardDrive;
    private final WorldWriter writer;

    public WriteToWorldCommand(HardDrive hardDrive) {

        this.hardDrive = hardDrive;
        this.writer = new WorldWriter(hardDrive.IOManager);
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> dispatcher.register(literal("writetoworld")
                        .requires(source -> source.hasPermissionLevel(hardDrive.config.commandsPermissionLevel.getValue()))
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
        if (!src.isExecutedByPlayer()) return -1;
        synchronized (hardDrive.IOManager) {
            if (hardDrive.IOManager.isBusy()) {
                sendFeedback("Other operation is running right now.", src);
                return 1;
            }
            WorldAccess world = src.getWorld();
            BlockPos pos = BlockPosArgumentType.getBlockPos(context, "position");
            File file = new File(StringArgumentType.getString(context, "file_path"));
            PlayerEntity player = (PlayerEntity) src.getEntity();
            writer.reset(world, pos, file, player);
            hardDrive.IOManager.startWorker(writer);
            return 1;
        }
    }

    private void sendFeedback(String msg, ServerCommandSource src) {
        src.sendFeedback(() -> Text.literal(msg), true);
    }
}