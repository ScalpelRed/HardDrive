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

    public SimpleConfigEntry<Integer> stepBit = new SimpleConfigEntry<>(this,
            "step_bit", Integer.class, 1, "Step in bit position within a byte");

    public SimpleConfigEntry<Integer> stepX = new SimpleConfigEntry<>(this,
            "step_x", Integer.class, 1, "Step in byte position at X direction");

    public SimpleConfigEntry<Integer> stepZ = new SimpleConfigEntry<>(this,
            "size_z", Integer.class, 1, "Step in byte position at Z direction");

    public SimpleConfigEntry<Integer> stepY = new SimpleConfigEntry<>(this,
            "size_y", Integer.class, 9, "Step in byte position at Y direction. " +
            "Note that bytes are vertical, and the value should be at least 8.");

    public SimpleConfigEntry<Boolean> embedLength = new SimpleConfigEntry<>(this,
            "embed_length", Boolean.class, true, "Add file length at the beginning");

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
        registerEntryHandle(stepBit);
        registerEntryHandle(stepX);
        registerEntryHandle(stepZ);
        registerEntryHandle(stepY);
        registerEntryHandle(embedLength);
        registerEntryHandle(block_zero);
        registerEntryHandle(block_one);
        registerEntryHandle(allowAnyPath);
        registerEntryHandle(commandsPermissionLevel);
        registerEntryHandle(bytesPerTickWrite);
        registerEntryHandle(bytesPerTickRead);
    }
}
