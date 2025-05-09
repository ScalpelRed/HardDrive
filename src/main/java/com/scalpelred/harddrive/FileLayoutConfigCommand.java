package com.scalpelred.harddrive;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.*;

public class FileLayoutConfigCommand {

    private final HardDrive hardDrive;
    private final HardDriveConfig config;

    public FileLayoutConfigCommand(HardDrive hardDrive) {

        this.hardDrive = hardDrive;
        this.config = hardDrive.config;
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> dispatcher.register(literal("filelayout")
                        .requires(source -> source.hasPermissionLevel(4))
                        .then(literal("save")
                                .executes(this::save)
                        ).then(literal("load")
                                .executes(this::load)
                        ).then(literal("load-nocreate")
                                .executes(this::loadNoCreate)
                        ).then(literal("set")
                                .then(literal("size_x")
                                        .then(argument("value", IntegerArgumentType.integer())
                                                .executes(context -> setValue(context, LayoutParam.size_x))
                                        )
                                ).then(literal("size_z")
                                        .then(argument("value", IntegerArgumentType.integer())
                                                .executes(context -> setValue(context, LayoutParam.size_z))
                                        )
                                ).then(literal("size_y")
                                        .then(argument("value", IntegerArgumentType.integer())
                                                .executes(context -> setValue(context, LayoutParam.size_y))
                                        )
                                ).then(literal("append_length")
                                        .then(argument("value", BoolArgumentType.bool())
                                                .executes(context -> setValue(context, LayoutParam.append_length))
                                        )
                                ).then(literal("add_layer_spacing")
                                        .then(argument("value", BoolArgumentType.bool())
                                                .executes(context -> setValue(context, LayoutParam.add_layer_spacing)))
                                )
                        ).then(literal("get")
                                .then(literal("size_x")
                                        .executes(context -> getValue(context, LayoutParam.size_x))
                                ).then(literal("size_z")
                                        .executes(context -> getValue(context, LayoutParam.size_z))
                                ).then(literal("size_y")
                                        .executes(context -> getValue(context, LayoutParam.size_y))
                                ).then(literal("append_length")
                                        .executes(context -> getValue(context, LayoutParam.append_length))
                                ).then(literal("add_layer_spacing")
                                        .executes(context -> getValue(context, LayoutParam.add_layer_spacing))
                                )
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

    private int loadNoCreate(CommandContext<ServerCommandSource> context) {
        try {
            hardDrive.config.load();
            boolean loaded = hardDrive.config.isLoaded();
            String message = loaded ? "Config loaded." : "Config does not exist.";
            sendFeedback(message, context.getSource());
            return loaded ? 1 : 0;
        } catch (Exception e) {
            sendFeedback("Exception thrown during loading the config: " + e.getMessage(), context.getSource());
            return -1;
        }
    }

    private int setValue(CommandContext<ServerCommandSource> context, LayoutParam param) {
        String reportValue;
        switch (param) {
            case size_x:
            case size_z:
            case size_y:
                Integer vInt = IntegerArgumentType.getInteger(context, "value");
                reportValue = String.valueOf(vInt);
                switch (param) { // that's acceptable I guess?...
                    case size_x -> config.sizeX.setValue(vInt);
                    case size_z -> config.sizeZ.setValue(vInt);
                    case size_y -> config.sizeY.setValue(vInt);
                }
                break;
            case append_length:
            case add_layer_spacing:
                Boolean vBool = BoolArgumentType.getBool(context, "value");
                reportValue = String.valueOf(vBool);
                if (param == LayoutParam.append_length) config.appendLength.setValue(vBool);
                else config.addLayerSpacing.setValue(vBool);
                break;
            default:
                reportValue = "INVALID";
                break;
        }
        sendFeedback("Value of \"" + param + "\" was set to \"" + reportValue + "\"", context.getSource());
        return 1;
    }

    private int getValue(CommandContext<ServerCommandSource> context, LayoutParam param) {
        final Object value;
        switch (param) {
            case size_x -> value = config.sizeX.getValue();
            case size_z -> value = config.sizeZ.getValue();
            case size_y -> value = config.sizeY.getValue();
            case append_length -> value = config.appendLength.getValue();
            case add_layer_spacing -> value = config.addLayerSpacing.getValue();
            default -> value = "INVALID";
        }
        sendFeedback("Value of \"" + param + "\" is \"" + value + "\"", context.getSource());
        return 1;
    }

    private void sendFeedback(String msg, ServerCommandSource src) {
        src.sendFeedback(() -> Text.literal(msg), true);
    }

    enum LayoutParam {
        size_x,
        size_z,
        size_y,
        append_length,
        add_layer_spacing
    }
}
