package com.scalpelred.harddrive;

import com.scalpelred.mcmodutil.Config;
import com.scalpelred.mcmodutil.ConfigEntryHandle;

public class HardDriveConfig extends Config {

    public ConfigEntryHandle<Integer> sizeX = new ConfigEntryHandle<>(
            "size_x", Integer.class, 16, "Number of X rows");
    public ConfigEntryHandle<Integer> sizeZ = new ConfigEntryHandle<>(
            "size_z", Integer.class, 16, "Number of Z rows");
    public ConfigEntryHandle<Integer> sizeY = new ConfigEntryHandle<>(
            "size_y", Integer.class, 4, "Number of Y layers");
    public ConfigEntryHandle<Boolean> appendLength = new ConfigEntryHandle<>(
            "append_length", Boolean.class, true, "Add file length at the beginning");
    public ConfigEntryHandle<Boolean> addLayerSpacing = new ConfigEntryHandle<>(
            "add_layer_spacing", Boolean.class, true, "Add one block between Y layers");
    public ConfigEntryHandle<Boolean> allowAnyPath = new ConfigEntryHandle<>(
            "allow_any_path", Boolean.class, false,
            "Allow reading/writing files from any directories on the host computer. " +
                    "It grants filesystem access to all operators, USE CAREFULLY.");

    public HardDriveConfig(HardDrive hardDrive) {
        super(hardDrive.logger, "main", false);

        registerEntryHandle(sizeX);
        registerEntryHandle(sizeZ);
        registerEntryHandle(sizeY);
        registerEntryHandle(appendLength);
        registerEntryHandle(addLayerSpacing);
        registerEntryHandle(allowAnyPath);
    }
}
