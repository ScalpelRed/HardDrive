package com.scalpelred.harddrive;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.scalpelred.mcmodutil.BlockConfigEntry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.Block;
import net.minecraft.command.argument.BlockStateArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.*;

public class HardDriveCommand {

    private final HardDrive hardDrive;
    private final HardDriveConfig config;

    public HardDriveCommand(HardDrive hardDrive) {

        this.hardDrive = hardDrive;
        this.config = hardDrive.config;
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> dispatcher.register(literal("harddrive")
                        .requires(source -> source.hasPermissionLevel(hardDrive.config.commandsPermissionLevel.getValue()))
                        .then(literal("config")
                                .then(literal("save")
                                        .executes(this::save)
                                ).then(literal("load")
                                        .executes(this::load)
                                ).then(literal("size_x")
                                        .then(argument("value", IntegerArgumentType.integer())
                                                .executes(context -> setValue(context, LayoutParam.SIZE_X))
                                        ).executes(context -> getValue(context, LayoutParam.SIZE_X))
                                ).then(literal("size_z")
                                        .then(argument("value", IntegerArgumentType.integer())
                                                .executes(context -> setValue(context, LayoutParam.SIZE_Z))
                                        ).executes(context -> getValue(context, LayoutParam.SIZE_Z))
                                ).then(literal("size_y")
                                        .then(argument("value", IntegerArgumentType.integer())
                                                .executes(context -> setValue(context, LayoutParam.SIZE_Y))
                                        ).executes(context -> getValue(context, LayoutParam.SIZE_Y))
                                ).then(literal("embed_length")
                                        .then(argument("value", BoolArgumentType.bool())
                                                .executes(context -> setValue(context, LayoutParam.EMBED_LENGTH))
                                        ).executes(context -> getValue(context, LayoutParam.EMBED_LENGTH))
                                ).then(literal("block_zero")
                                        .then(argument("value", BlockStateArgumentType.blockState(registryAccess))
                                                .executes(context -> setValue(context, LayoutParam.BLOCK_ZERO))
                                        ).executes(context -> getValue(context, LayoutParam.BLOCK_ZERO))
                                ).then(literal("block_one")
                                        .then(argument("value", BlockStateArgumentType.blockState(registryAccess))
                                                .executes(context -> setValue(context, LayoutParam.BLOCK_ONE))
                                        ).executes(context -> getValue(context, LayoutParam.BLOCK_ONE))
                                ).then(literal("add_layer_spacing")
                                        .then(argument("value", BoolArgumentType.bool())
                                                .executes(context -> setValue(context, LayoutParam.LAYER_SPACING))
                                        ).executes(context -> getValue(context, LayoutParam.LAYER_SPACING))
                                )
                        ).then(literal("op")
                                .then(literal("stop")
                                        .executes(this::stopOperation)
                                ).then(literal("get")
                                        .executes(this::getOperation)
                                ).executes(this::getOperation)
                        )
                )
        );
    }

    private int save(CommandContext<ServerCommandSource> context) {
        try {
            hardDrive.config.save();
            sendFeedback("Config saved.", context.getSource());
            return 1;
        } catch (Exception e) {
            sendFeedback("Exception thrown during saving the config: " + e.getMessage(), context.getSource());
            return -1;
        }
    }

    private int load(CommandContext<ServerCommandSource> context) {
        try {
            boolean created = hardDrive.config.loadOrCreate();
            String message = created ? "Config created and loaded." : "Config loaded.";
            sendFeedback(message, context.getSource());
            return created ? 2 : 1;
        } catch (Exception e) {
            sendFeedback("Exception thrown during loading the config: " + e.getMessage(), context.getSource());
            return -1;
        }
    }

    private int setValue(CommandContext<ServerCommandSource> context, LayoutParam param) {
        String reportValue;
        switch (param) {
            case SIZE_X:
            case SIZE_Z:
            case SIZE_Y:
            case LAYER_SPACING:
                Integer vInt = IntegerArgumentType.getInteger(context, "value");
                reportValue = String.valueOf(vInt);
                switch (param) { // that's acceptable I guess?...
                    case SIZE_X -> config.sizeX.setValue(vInt);
                    case SIZE_Z -> config.sizeZ.setValue(vInt);
                    case SIZE_Y -> config.sizeY.setValue(vInt);
                    case LAYER_SPACING -> config.layerSpacing.setValue(vInt);
                }
                break;
            case EMBED_LENGTH:
                Boolean vBool = BoolArgumentType.getBool(context, "value");
                reportValue = String.valueOf(vBool);
                config.embedLength.setValue(vBool);
                break;
            case BLOCK_ZERO:
                Block vBlock0 = BlockStateArgumentType.getBlockState(context, "value")
                        .getBlockState().getBlock();
                reportValue = BlockConfigEntry.getBlockId(vBlock0);
                config.block_zero.setValue(vBlock0);
                break;
            case BLOCK_ONE:
                Block vBlock1 = BlockStateArgumentType.getBlockState(context, "value")
                        .getBlockState().getBlock();
                reportValue = BlockConfigEntry.getBlockId(vBlock1);
                config.block_one.setValue(vBlock1);
                break;
            default:
                reportValue = "INVALID";
                break;
        }
        sendFeedback("Value of \"" + param.toString().toLowerCase() + "\" was set to \"" + reportValue + "\"", context.getSource());
        return 1;
    }

    private int getValue(CommandContext<ServerCommandSource> context, LayoutParam param) {
        final Object value;
        switch (param) {
            case SIZE_X -> value = config.sizeX.getValue();
            case SIZE_Z -> value = config.sizeZ.getValue();
            case SIZE_Y -> value = config.sizeY.getValue();
            case EMBED_LENGTH -> value = config.embedLength.getValue();
            case BLOCK_ZERO -> value = config.block_zero.getValue().getName();
            case BLOCK_ONE -> value = config.block_one.getValue().getName();
            case LAYER_SPACING -> value = config.layerSpacing.getValue();
            default -> value = "INVALID";
        }
        sendFeedback("Value of \"" + param.toString().toLowerCase() + "\" is \"" + value + "\"", context.getSource());
        return 1;
    }

    private int stopOperation(CommandContext<ServerCommandSource> context) {
        if (hardDrive.IOManager.stopWorker()) sendFeedback("Operation stopped.", context.getSource());
        else sendFeedback("No operation to stop.", context.getSource());
        return 1;
    }

    private int getOperation(CommandContext<ServerCommandSource> context) {
        sendFeedback(hardDrive.IOManager.describeOperation(), context.getSource());
        return 1;
    }

    private void sendFeedback(String msg, ServerCommandSource src) {
        src.sendFeedback(() -> Text.literal(msg), true);
    }

    enum LayoutParam {
        SIZE_X,
        SIZE_Z,
        SIZE_Y,
        EMBED_LENGTH,
        BLOCK_ZERO,
        BLOCK_ONE,
        LAYER_SPACING,

        ALLOW_ANY_PATH,
        COMMANDS_PERMISSION_LEVEL,
        BYTES_PER_TICK_WRITE,
        BYTES_PER_TICK_READ
    }
}
