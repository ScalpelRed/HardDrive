package com.scalpelred.harddrive;

import com.scalpelred.mcmodutil.BlockConfigEntry;
import com.scalpelred.mcmodutil.Config;
import com.scalpelred.mcmodutil.SimpleConfigEntry;
import net.minecraft.block.Blocks;

public class HardDriveConfig extends Config {

    public SimpleConfigEntry<Integer> sizeX = new SimpleConfigEntry<>(this,
            "size_x", Integer.class, 16, "Number of X rows");

    public SimpleConfigEntry<Integer> sizeZ = new SimpleConfigEntry<>(this,
            "size_z", Integer.class, 16, "Number of Z rows");

    public SimpleConfigEntry<Integer> sizeY = new SimpleConfigEntry<>(this,
            "size_y", Integer.class, 4, "Number of Y layers");

    public SimpleConfigEntry<Boolean> appendLength = new SimpleConfigEntry<>(this,
            "append_length", Boolean.class, true, "Add file length at the beginning");

    public SimpleConfigEntry<Boolean> addLayerSpacing = new SimpleConfigEntry<>(this,
            "add_layer_spacing", Boolean.class, true, "Add one block between Y layers");

    public BlockConfigEntry block_zero = new BlockConfigEntry(this,
            "block_zero", Blocks.LIME_CONCRETE, "Block that represents zeroes.\n" +
            "NOTE: transparent blocks in big files may cause intense lags.");

    public BlockConfigEntry block_one = new BlockConfigEntry(this,
            "block_one", Blocks.REDSTONE_BLOCK, "Block that represents ones");

    public SimpleConfigEntry<Boolean> allowAnyPath = new SimpleConfigEntry<>(this,
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
        registerEntryHandle(block_zero);
        registerEntryHandle(block_one);
        registerEntryHandle(allowAnyPath);
    }
}
