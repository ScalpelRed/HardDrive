package com.scalpelred.harddrive;

import com.scalpelred.mcmodutil.Config;
import com.scalpelred.mcmodutil.ConfigEntryHandle;

public class PlacementConfig extends Config {

    public ConfigEntryHandle<Integer> sizeX = new ConfigEntryHandle<>(
            "size_x", Integer.class, 16, "Number of X rows");
    public ConfigEntryHandle<Integer> sizeZ = new ConfigEntryHandle<>(
            "size_z", Integer.class, 16, "Number of Z rows");
    public ConfigEntryHandle<Integer> sizeY = new ConfigEntryHandle<>(
            "size_y", Integer.class, 4, "Number of Y layers");
    public ConfigEntryHandle<Boolean> addLength = new ConfigEntryHandle<>(
            "add_length", Boolean.class, true, "Add file length at the beginning");
    public ConfigEntryHandle<Boolean> addLayerSpacing = new ConfigEntryHandle<>(
            "add_layer_spacing", Boolean.class, false, "Add one block between Y layers");

    public PlacementConfig(HardDrive hardDrive) {
        super(hardDrive.logger, "placement", true);

        registerEntryHandle(sizeX);
        registerEntryHandle(sizeZ);
        registerEntryHandle(sizeY);
        registerEntryHandle(addLength);
        registerEntryHandle(addLayerSpacing);
    }
}
