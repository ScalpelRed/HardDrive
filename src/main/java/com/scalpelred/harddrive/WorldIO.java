package com.scalpelred.harddrive;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class WorldIO {

    private final int LONG_WAIT_NOTE_RATE = 0x1FFFF; // 128 KB

    private final HardDrive hardDrive;
    private final HardDriveConfig config;

    public WorldIO(HardDrive hardDrive) {
        this.hardDrive = hardDrive;
        this.config = hardDrive.config;
    }

    public boolean WriteToWorld(WorldAccess world, BlockPos pos, File file)
            throws IOException {

        try (FileInputStream f = new FileInputStream(file))
        {
            byte[] buffer = new byte[4096];
            int bytesRead;
            int currentByteIndex;

            int sizeX;
            int sizeZ;
            int sizeY;

            // for some reason I like to compare these two branches to x86's protected and real modes
            // (nothing similar, actually)
                sizeX = config.sizeX.getValue();
                sizeZ = config.sizeZ.getValue();
                sizeY = config.sizeY.getValue();
            if (config.appendLength.getValue()) {
                long volume = (long)sizeX * sizeZ * sizeY;
                long length = file.length();

                if (volume >= length) { // enough space, writing actual length
                    for (int i = 0; i <= 7; i++) {
                        buffer[i] = (byte)(length >> (i << 3));
                    }
                }
                else { // not enough space, writing how much we can fit
                    for (int i = 0; i <= 7; i++) {
                        buffer[i] = (byte)(volume >> (i << 3));
                    }
                }

                bytesRead = f.read(buffer, 8, buffer.length - 8);
                if (bytesRead == -1) bytesRead = 8; // even if no data, we still have to write length
                else bytesRead += 8;
            }
            else {
                bytesRead = f.read(buffer, 0, buffer.length);
                if (bytesRead == -1) return true; // no data, leaving

                sizeX = config.sizeX.getValue();
                sizeZ = config.sizeZ.getValue();
                sizeY = config.sizeY.getValue();
            }
            currentByteIndex = 0;
            long totalBytes = 0;

            BlockState block0 = Blocks.GREEN_STAINED_GLASS.getDefaultState();
            BlockState block1 = Blocks.REDSTONE_BLOCK.getDefaultState();
            int yStep = config.addLayerSpacing.getValue() ? 9 : 8;

            BlockPos.Mutable cpos = pos.mutableCopy();
            for (int y = 0; y < sizeY; y++) {
                for (int z = 0; z < sizeZ; z++) {
                    for (int x = 0; x < sizeX; x++) {

                        if (currentByteIndex == bytesRead) {
                            bytesRead = f.read(buffer, 0, buffer.length);
                            if (bytesRead == -1) return true;
                            currentByteIndex = 0;
                        }
                        byte currentByte = buffer[currentByteIndex];
                        currentByteIndex++;
                        byte mask = 1;
                        totalBytes++;
                        if ((totalBytes & LONG_WAIT_NOTE_RATE) == 0) {
                            hardDrive.logger.info("STILL WORKING! Wrote {} bytes so far.", totalBytes);
                        }

                        for (int b = 0; b < 8; b++) {

                            int bit = currentByte & mask;
                            mask <<= 1;
                            if (bit == 0) world.setBlockState(cpos, block0, Block.NOTIFY_ALL);
                            else world.setBlockState(cpos, block1, Block.NOTIFY_ALL);

                            cpos.move(0, 1, 0);
                        }
                        cpos.move(1, -8, 0);
                    }
                    cpos.setX(pos.getX());
                    cpos.move(0, 0, 1);
                }
                cpos.setZ(pos.getZ());
                cpos.move(0, yStep, 0);
            }
            return false;
        }
    }
}
