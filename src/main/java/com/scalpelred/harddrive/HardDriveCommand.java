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
                                                .executes(context -> setValue(context, ConfigParam.SIZE_X))
                                        ).executes(context -> getValue(context, ConfigParam.SIZE_X))
                                ).then(literal("size_z")
                                        .then(argument("value", IntegerArgumentType.integer())
                                                .executes(context -> setValue(context, ConfigParam.SIZE_Z))
                                        ).executes(context -> getValue(context, ConfigParam.SIZE_Z))
                                ).then(literal("size_y")
                                        .then(argument("value", IntegerArgumentType.integer())
                                                .executes(context -> setValue(context, ConfigParam.SIZE_Y))
                                        ).executes(context -> getValue(context, ConfigParam.SIZE_Y))
                                ).then(literal("step_bit")
                                        .then(argument("value", IntegerArgumentType.integer())
                                                .executes(context -> setValue(context, ConfigParam.STEP_BIT))
                                        ).executes(context -> getValue(context, ConfigParam.STEP_BIT))
                                ).then(literal("step_x")
                                        .then(argument("value", IntegerArgumentType.integer())
                                                .executes(context -> setValue(context, ConfigParam.STEP_X))
                                        ).executes(context -> getValue(context, ConfigParam.STEP_X))
                                ).then(literal("step_z")
                                        .then(argument("value", IntegerArgumentType.integer())
                                                .executes(context -> setValue(context, ConfigParam.STEP_Z))
                                        ).executes(context -> getValue(context, ConfigParam.STEP_Z))
                                ).then(literal("step_y")
                                        .then(argument("value", IntegerArgumentType.integer())
                                                .executes(context -> setValue(context, ConfigParam.STEP_Y))
                                        ).executes(context -> getValue(context, ConfigParam.STEP_Y))
                                ).then(literal("embed_length")
                                        .then(argument("value", BoolArgumentType.bool())
                                                .executes(context -> setValue(context, ConfigParam.EMBED_LENGTH))
                                        ).executes(context -> getValue(context, ConfigParam.EMBED_LENGTH))
                                ).then(literal("block_zero")
                                        .then(argument("value", BlockStateArgumentType.blockState(registryAccess))
                                                .executes(context -> setValue(context, ConfigParam.BLOCK_ZERO))
                                        ).executes(context -> getValue(context, ConfigParam.BLOCK_ZERO))
                                ).then(literal("block_one")
                                        .then(argument("value", BlockStateArgumentType.blockState(registryAccess))
                                                .executes(context -> setValue(context, ConfigParam.BLOCK_ONE))
                                        ).executes(context -> getValue(context, ConfigParam.BLOCK_ONE))
                                )
                        ).then(literal("adv_config")
                                .then(literal("allow_any_path")
                                        .executes(context -> getValue(context, ConfigParam.ALLOW_ANY_PATH))
                                ).then(literal("commands_permission_level")
                                        .executes(context -> getValue(context, ConfigParam.COMMANDS_PERMISSION_LEVEL))
                                ).then(literal("bytes_per_tick_write")
                                        .executes(context -> getValue(context, ConfigParam.BYTES_PER_TICK_WRITE))
                                ).then(literal("bytes_per_tick_read")
                                        .executes(context -> getValue(context, ConfigParam.BYTES_PER_TICK_READ))
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

    private int setValue(CommandContext<ServerCommandSource> context, ConfigParam param) {
        String reportValue;
        switch (param) {
            case SIZE_X:
            case SIZE_Z:
            case SIZE_Y:
            case STEP_BIT:
            case STEP_X:
            case STEP_Z:
            case STEP_Y:
                Integer vInt = IntegerArgumentType.getInteger(context, "value");
                reportValue = String.valueOf(vInt);
                switch (param) { // that's acceptable I guess?...
                    case SIZE_X -> config.sizeX.setValue(vInt);
                    case SIZE_Z -> config.sizeZ.setValue(vInt);
                    case SIZE_Y -> config.sizeY.setValue(vInt);
                    case STEP_BIT -> config.stepBit.setValue(vInt);
                    case STEP_X -> config.stepX.setValue(vInt);
                    case STEP_Z -> config.stepZ.setValue(vInt);
                    case STEP_Y -> config.stepY.setValue(vInt);
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

    private int getValue(CommandContext<ServerCommandSource> context, ConfigParam param) {
        final Object value;
        switch (param) {
            case SIZE_X -> value = config.sizeX.getValue();
            case SIZE_Z -> value = config.sizeZ.getValue();
            case SIZE_Y -> value = config.sizeY.getValue();
            case STEP_BIT -> value = config.stepBit.getValue();
            case STEP_X -> value = config.stepX.getValue();
            case STEP_Z -> value = config.stepZ.getValue();
            case STEP_Y -> value = config.stepY.getValue();
            case EMBED_LENGTH -> value = config.embedLength.getValue();
            case BLOCK_ZERO -> value = config.block_zero.getValue().getName();
            case BLOCK_ONE -> value = config.block_one.getValue().getName();
            case ALLOW_ANY_PATH -> value = config.allowAnyPath.getValue();
            case COMMANDS_PERMISSION_LEVEL -> value = config.commandsPermissionLevel.getValue();
            case BYTES_PER_TICK_WRITE -> value = config.bytesPerTickWrite.getValue();
            case BYTES_PER_TICK_READ -> value = config.bytesPerTickRead.getValue();
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

    enum ConfigParam {
        SIZE_X,
        SIZE_Z,
        SIZE_Y,
        STEP_BIT,
        STEP_X,
        STEP_Y,
        STEP_Z,
        EMBED_LENGTH,
        BLOCK_ZERO,
        BLOCK_ONE,

        ALLOW_ANY_PATH,
        COMMANDS_PERMISSION_LEVEL,
        BYTES_PER_TICK_WRITE,
        BYTES_PER_TICK_READ
    }
}
