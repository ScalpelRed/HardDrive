package com.scalpelred.harddrive;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;

public class WorldReader implements IIOWorker {

    private IOManager IOManager;
    private HardDriveConfig config;

    private WorldAccess world;
    private BlockPos position;
    private File originalFile;
    private long originalLength;
    private PlayerEntity player;

    private final Object workingStateLock = new Object();
    private boolean working;
    private boolean overwriteAsked = false;

    private long length;
    private File file;
    private FileOutputStream fileStream;
    private int localX;
    private int localZ;
    private BlockPos.Mutable currentPos;
    private int sizeX;
    private int sizeZ;
    private Block block1;
    private int yStep;
    private final static int BUFFER_LENGTH = 4096;
    private final byte[] buffer = new byte[BUFFER_LENGTH];
    private int bufferByteIndex;
    private long totalBytes;

    private static final long LOG_NOTIFICATION_BYTES = 0x20000 - 1; // 128 KiB

    public WorldReader(IOManager IOManager) {
        this.IOManager = IOManager;
        this.config = IOManager.hardDrive.config;
    }

    public boolean reset(WorldAccess world, BlockPos pos, File file, long length, PlayerEntity player) {
        synchronized (workingStateLock) {
            this.world = world;
            this.position = pos;
            this.originalFile = file;
            this.originalLength = this.length = length;
            this.player = player;

            overwriteAsked = false;

            try {
                String outputDirPath = IOManager.getOutputDir().getPath();
                if (!file.isAbsolute()) {
                    file = Paths.get(outputDirPath, file.getPath()).toFile();
                }
                file = file.getCanonicalFile();

                if (!IOManager.hardDrive.config.allowAnyPath.getValue() && !file.getPath().startsWith(outputDirPath)) {
                    sendFeedback("ERROR: Access denied (use \"harddrive_out\" folder or it's subfolders)");
                    return false;
                }

                this.file = file;
                if (this.file.exists()) {
                    overwriteAsked = true;
                    sendFeedback("OVERWRITE PREVENTED: File already exists, run the command again to overwrite it.");
                    return true;
                }
                postFileResolving();
            } catch (Exception e) {
                sendFeedback("ERROR: Exception thrown during reading initialization: " + e.getMessage());
                working = false;
            }
        }
        return false;
    }

    private void postFileResolving() throws FileNotFoundException {
        synchronized (workingStateLock) {
            sizeX = config.sizeX.getValue();
            sizeZ = config.sizeZ.getValue();
            block1 = config.block_one.getValue();
            yStep = config.layerSpacing.getValue() + 8;
            bufferByteIndex = 0;
            totalBytes = 0;
            currentPos = position.mutableCopy();
            sendFeedback("Reading...");
            fileStream = new FileOutputStream(file);
            if (length >= 0) {
                if (config.embedLength.getValue()) {
                    // here we have embedded length, but ignoring it - just shifting the position
                    localX = 8 % sizeX;
                    localZ = 8 / sizeX % sizeZ;
                    currentPos.move(localX, 0, localZ);
                } else {
                    localX = 0;
                    localZ = 0;
                }
            } else {
                if (!config.embedLength.getValue()) {
                    onDoneNormally(Result.NO_ANY_LENGTH);
                    return;
                }
                localX = 0;
                localZ = 0;
                length = 0;
                long bit = 1;
                while (bit != 0) {
                    for (int b = 0; b < 8; b++) {
                        BlockState blockState = world.getBlockState(currentPos);
                        if (blockState.getBlock() == block1) length |= bit;
                        bit <<= 1;

                        currentPos.move(0, 1, 0);
                    }

                    localX++;
                    currentPos.move(1, -8, 0);
                    if (localX >= sizeX) {
                        localX = 0;
                        localZ++;
                        currentPos.setX(position.getX());
                        currentPos.move(0, 0, 1);

                        if (localZ >= sizeZ) {
                            localZ = 0;
                            currentPos.setZ(position.getZ());
                            currentPos.move(0, yStep, 0);
                            if (currentPos.getY() + yStep > 320) {
                                onDoneNormally(Result.CUT_BY_WORLD_EDGE);
                                return;
                            }
                        }
                    }
                }
            }
            if (length < 0) {
                onDoneNormally(Result.NEGATIVE_EMBEDDED_LENGTH);
                return;
            }
            fileStream = new FileOutputStream(file);
            working = true;
        }
    }

    public void confirmOverwrite() {
        synchronized (workingStateLock) {
            try {
                if (!overwriteAsked) throw new IllegalStateException("No overwrite to be confirmed.");
                overwriteAsked = false;
                postFileResolving();
            } catch (Exception e) {
                sendFeedback("ERROR: Exception thrown during reading initialization: " + e.getMessage());
                working = false;
            }
        }
    }

    @Override
    public void tick() {
        synchronized (workingStateLock) {
            if (!working) return;

            try {
                long bytesPerTick = config.bytesPerTickRead.getValue();
                for (int tickByteIndex = 0; tickByteIndex < bytesPerTick; tickByteIndex++) {
                    if (totalBytes >= length) {
                        fileStream.write(buffer, 0, bufferByteIndex);
                        onDoneNormally(Result.SUCCESS);
                        return;
                    }
                    if (bufferByteIndex == BUFFER_LENGTH) {
                        fileStream.write(buffer, 0, BUFFER_LENGTH);
                        bufferByteIndex = 0;
                    }

                    byte currentByte = 0;
                    byte mask = 1;
                    for (int b = 0; b < 8; b++) {
                        BlockState blockState = world.getBlockState(currentPos);
                        if (blockState.getBlock().equals(block1)) currentByte |= mask;
                        mask <<= 1;
                        currentPos.move(0, 1, 0);
                    }
                    buffer[bufferByteIndex] = currentByte;
                    bufferByteIndex++;
                    totalBytes++;

                    if ((totalBytes & LOG_NOTIFICATION_BYTES) == 0) {
                        IOManager.hardDrive.logger.info("STILL READING, {} bytes so far.", totalBytes);
                    }

                    localX++;
                    currentPos.move(1, -8, 0);
                    if (localX >= sizeX) {
                        localX = 0;
                        localZ++;
                        currentPos.setX(position.getX());
                        currentPos.move(0, 0, 1);

                        if (localZ >= sizeZ) {
                            localZ = 0;
                            currentPos.setZ(position.getZ());
                            currentPos.move(0, yStep, 0);
                            if (currentPos.getY() + yStep > 320) {
                                onDoneNormally(Result.CUT_BY_WORLD_EDGE);
                                return;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                sendFeedback("ERROR: Exception thrown during reading: " + e.getMessage());
                working = false;
            }
        }
    }

    @Override
    public void stop() {
        synchronized (workingStateLock) {
            if (working) onDoneNormally(Result.INTERRUPTED);
        }
    }

    private void onDoneNormally(Result res) {
        synchronized (workingStateLock) {
            working = false;
            try {
                fileStream.close();
            } catch (Exception ignored) { }
            switch (res) {
                case SUCCESS:
                    sendFeedback("Reading done.");
                    break;
                case NOT_ENOUGH_SPACE:
                    sendFeedback("ERROR: There's not enough space in the area, written as much as possible.");
                    break;
                case NEGATIVE_EMBEDDED_LENGTH:
                    sendFeedback("ERROR: Embedded length is negative.");
                    break;
                case NO_ANY_LENGTH:
                    sendFeedback("ERROR: No length provided, specify it as an argument or use embedded length.");
                    break;
                case CUT_BY_WORLD_EDGE:
                    sendFeedback("ERROR: World border was reached while reading the file.");
                    break;
                case INTERRUPTED:
                    sendFeedback("Reading interrupted manually.");
                    break;
                default:
                    sendFeedback("Reading stopped for unknown reason.");
                    break;
            }
        }
    }

    @Override
    public boolean isWorking() {
        synchronized (workingStateLock) {
            return working;
        }
    }

    public boolean waitsForOverwriteConfirm() {
        synchronized (workingStateLock) {
            return overwriteAsked;
        }
    }

    @Override
    public String describe() {
        synchronized (workingStateLock) {
            String msg = "Reading by %s at (%d, %d, %d), %s, file %s";
            if (!working) msg = "(DONE) " + msg;
            return String.format(msg, player.getNameForScoreboard(), position.getX(), position.getY(), position.getZ(),
                    (originalLength >= 0) ? (originalLength + " bytes") : "embedded length", originalFile.getPath());
        }
    }

    private void sendFeedback(String msg) {
        if (player != null) player.sendMessage(Text.literal(msg), false);
    }

    enum Result {
        SUCCESS,
        NOT_ENOUGH_SPACE,
        CUT_BY_WORLD_EDGE,
        NEGATIVE_EMBEDDED_LENGTH,
        NO_ANY_LENGTH,
        INTERRUPTED
    }
}
