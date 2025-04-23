package com.scalpelred.harddrive;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class WorldIO {

    private final int LONG_WAIT_NOTE_RATE = 0x1FFFF; // 128 KB

    private final HardDrive hardDrive;
    private final HardDriveConfig config;

    public WorldIO(HardDrive hardDrive) {
        this.hardDrive = hardDrive;
        this.config = hardDrive.config;
    }

    public IOResult writeToWorld(WorldAccess world, BlockPos pos, File file)
            throws IOException {

        byte[] buffer = new byte[4096];
        int bytesRead;
        int currentByteIndex;
        long totalBytes;

        int sizeX = config.sizeX.getValue();
        int sizeZ = config.sizeZ.getValue();
        int sizeY = config.sizeY.getValue();

        try (FileInputStream f = new FileInputStream(file)) {
            // for some reason I like to compare these two branches to x86's protected and real modes
            // (nothing similar, actually)
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
                totalBytes = -8;
            }
            else {
                bytesRead = f.read(buffer, 0, buffer.length);
                if (bytesRead == -1) return IOResult.SUCCESS; // no data, leaving
                totalBytes = 0;
            }
            currentByteIndex = 0;

            BlockState block0 = config.block_zero.getValue().getDefaultState();
            BlockState block1 = config.block_one.getValue().getDefaultState();
            int yStep = config.addLayerSpacing.getValue() ? 9 : 8;

            BlockPos.Mutable cpos = pos.mutableCopy();

            int x = 0;
            int z = 0;
            int y = 0;

            while (true) {

                if (currentByteIndex == bytesRead) {
                    bytesRead = f.read(buffer, 0, buffer.length);
                    if (bytesRead == -1) return IOResult.SUCCESS;
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

                x++;
                cpos.move(1, -8, 0);
                if (x >= sizeX) {
                    x = 0;
                    z++;
                    cpos.setX(pos.getX());
                    cpos.move(0, 0, 1);

                    if (z >= sizeZ) {
                        z = 0;
                        y++;
                        cpos.setZ(pos.getZ());
                        cpos.move(0, yStep, 0);
                        if (cpos.getY() + 8 > 320) return IOResult.CEILING_HIT;

                        if (y >= sizeY) return IOResult.NOT_ENOUGH_SPACE;
                    }
                }
            }
        }
    }

    public IOResult readFromWorld(WorldAccess world, BlockPos pos, File file, long length)
        throws IOException {

        int sizeX = config.sizeX.getValue();
        int sizeZ = config.sizeZ.getValue();
        Block block1 = config.block_one.getValue();
        int yStep = config.addLayerSpacing.getValue() ? 9 : 8;

        byte[] buffer = new byte[4096];
        int bytesRead = 0;
        long totalBytes = 0;

        int x;
        int z;
        BlockPos.Mutable cpos = pos.mutableCopy();

        if (length >= 0) {
            if (config.appendLength.getValue()) {
                x = 8 % sizeX;
                z = 8 / sizeX % sizeZ;
                cpos.move(x, 0, z);
            }
            else {
                x = 0;
                z = 0;
            }
        }
        else {
            if (!config.appendLength.getValue()) return IOResult.NO_ANY_LENGTH;

            x = 0;
            z = 0;
            length = 0;
            long bit = 1;
            while (bit != 0) {

                for (int b = 0; b < 8; b++) {

                    BlockState blockState = world.getBlockState(cpos);
                    if (blockState.getBlock() == block1) length |= bit;
                    bit <<= 1;

                    cpos.move(0, 1, 0);
                }

                x++;
                cpos.move(1, -8, 0);
                if (x >= sizeX) {
                    x = 0;
                    z++;
                    cpos.setX(pos.getX());
                    cpos.move(0, 0, 1);

                    if (z >= sizeZ) {
                        z = 0;
                        cpos.setZ(pos.getZ());
                        cpos.move(0, yStep, 0);
                        if (cpos.getY() + 8 > 320) return IOResult.CEILING_HIT;
                    }
                }
            }
        }
        if (length < 0) return IOResult.NEGATIVE_APPENDED_LENGTH;

        try (FileOutputStream f = new FileOutputStream(file)) {
            while (true) {

                if (totalBytes >= length) {
                    f.write(buffer, 0, bytesRead);
                    return IOResult.SUCCESS;
                }
                if (bytesRead == 4096) {
                    f.write(buffer, 0, 4096);
                    bytesRead = 0;
                }

                byte currentByte = 0;
                byte mask = 1;

                for (int b = 0; b < 8; b++) {

                    BlockState blockState = world.getBlockState(cpos);
                    if (blockState.getBlock().equals(block1)) currentByte |= mask;
                    mask <<= 1;

                    cpos.move(0, 1, 0);
                }
                buffer[bytesRead] = currentByte;
                bytesRead++;
                totalBytes++;
                if ((totalBytes & LONG_WAIT_NOTE_RATE) == 0) {
                    hardDrive.logger.info("STILL WORKING! Read {} bytes so far.", totalBytes);
                }

                x++;
                cpos.move(1, -8, 0);
                if (x >= sizeX) {
                    x = 0;
                    z++;
                    cpos.setX(pos.getX());
                    cpos.move(0, 0, 1);

                    if (z >= sizeZ) {
                        z = 0;
                        cpos.setZ(pos.getZ());
                        cpos.move(0, yStep, 0);
                        if (cpos.getY() + 8 > 320) return IOResult.CEILING_HIT;
                    }
                }
            }
        }
    }

    public enum IOResult {
        SUCCESS,

        NOT_ENOUGH_SPACE,
        CEILING_HIT,

        NO_ANY_LENGTH,
        NEGATIVE_APPENDED_LENGTH,
    }
}
