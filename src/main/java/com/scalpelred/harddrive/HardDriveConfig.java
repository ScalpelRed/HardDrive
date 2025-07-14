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

    public SimpleConfigEntry<Boolean> embedLength = new SimpleConfigEntry<>(this,
            "append_length", Boolean.class, true, "Add file length at the beginning");

    public SimpleConfigEntry<Integer> layerSpacing = new SimpleConfigEntry<>(this,
            "layer_spacing", Integer.class, 1, "Amount of blocks to add between Y layers");

    public BlockConfigEntry block_zero = new BlockConfigEntry(this,
            "block_zero", Blocks.LIME_CONCRETE, "Block that represents zeroes.\n" +
            "NOTE: transparent blocks in big files may cause intense lags.");

    public BlockConfigEntry block_one = new BlockConfigEntry(this,
            "block_one", Blocks.REDSTONE_BLOCK, "Block that represents ones");

    public SimpleConfigEntry<Boolean> allowAnyPath = new SimpleConfigEntry<>(this,
            "allow_any_path", Boolean.class, false,
            "Allow reading/writing files from any directories on the host computer. " +
                    "It grants filesystem access to all operators, USE CAREFULLY." +
                    "This entry cannot be changed by command.");

    public SimpleConfigEntry<Integer> commandsPermissionLevel = new SimpleConfigEntry<>(this,
            "commands_permission_level", Integer.class, 4,
            "Permission level required to run mod commands. This entry cannot be changed by command.");

    public SimpleConfigEntry<Long> bytesPerTickWrite = new SimpleConfigEntry<>(this,
            "bytes_per_tick_write", Long.class, 1024L,
            "How many bytes will be written per game tick. This entry cannot be changed by command.");

    public SimpleConfigEntry<Long> bytesPerTickRead = new SimpleConfigEntry<>(this,
            "bytes_per_tick_read", Long.class, 1024L,
            "How many bytes will be read per game tick. This entry cannot be changed by command.");

    public HardDriveConfig(HardDrive hardDrive) {
        super(hardDrive.logger, "main", false);

        registerEntryHandle(sizeX);
        registerEntryHandle(sizeZ);
        registerEntryHandle(sizeY);
        registerEntryHandle(embedLength);
        registerEntryHandle(layerSpacing);
        registerEntryHandle(block_zero);
        registerEntryHandle(block_one);
        registerEntryHandle(allowAnyPath);
        registerEntryHandle(commandsPermissionLevel);
        registerEntryHandle(bytesPerTickWrite);
        registerEntryHandle(bytesPerTickRead);
    }
}
