package com.scalpelred.harddrive;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Paths;

public class WorldWriter implements IIOWorker {

    private final IOManager IOManager;
    private final HardDriveConfig config;

    private WorldAccess world;
    private BlockPos position;
    private File originalFile;
    private PlayerEntity player;

    private final Object workingStateLock = new Object();
    private boolean working;

    private FileInputStream fileStream;
    private int localX;
    private int localY;
    private int localZ;
    private BlockPos.Mutable currentPos;
    private int sizeX;
    private int sizeZ;
    private int sizeY;
    private BlockState block0;
    private BlockState block1;
    private int stepBit;
    private int stepX;
    private int stepZ;
    private int stepY;
    private static final int BUFFER_LENGTH = 4096;
    private final byte[] buffer = new byte[BUFFER_LENGTH];
    private int bytesRead;
    private int bufferByteIndex;
    private long totalBytes;

    private static final long LOG_NOTIFICATION_BYTES = 0x20000 - 1; // 128 KiB

    public WorldWriter(IOManager IOManager) {
        this.IOManager = IOManager;
        this.config = IOManager.hardDrive.config;
    }

    public void reset(WorldAccess world, BlockPos pos, File file, PlayerEntity player) {
        synchronized (workingStateLock) {
            this.world = world;
            this.position = pos;
            this.originalFile = file;
            this.player = player;

            try {
                String inputDirPath = IOManager.getInputDir().getPath();
                if (!file.isAbsolute()) {
                    IOManager.createInputDirIfNeeded();
                    file = Paths.get(inputDirPath, file.getPath()).toFile();
                }
                file = file.getCanonicalFile();

                if (!IOManager.hardDrive.config.allowAnyPath.getValue() && !file.getPath().startsWith(inputDirPath)) {
                    sendFeedback("ERROR: Access denied (use \"harddrive_in\" folder or it's subfolders)");
                    return;
                }

                if (!file.exists()) {
                    sendFeedback("ERROR: File is missing.");
                    return;
                }

                sizeX = config.sizeX.getValue();
                sizeZ = config.sizeZ.getValue();
                sizeY = config.sizeY.getValue();
                sendFeedback("Writing...");
                fileStream = new FileInputStream(file);
                if (config.embedLength.getValue()) {
                    long volume = (long) sizeX * sizeZ * sizeY;
                    long length = file.length();

                    if (volume >= length) { // enough space, writing actual length
                        for (int i = 0; i <= 7; i++) {
                            buffer[i] = (byte) (length >> (i << 3));
                        }
                    } else { // not enough space, writing how much we can fit
                        for (int i = 0; i <= 7; i++) {
                            buffer[i] = (byte) (volume >> (i << 3));
                        }
                    }

                    bytesRead = fileStream.read(buffer, 8, BUFFER_LENGTH - 8);
                    if (bytesRead <= 0) bytesRead = 8; // even if no data, we still have to write length
                    else bytesRead += 8;
                } else {
                    bytesRead = fileStream.read(buffer, 0, BUFFER_LENGTH);
                    if (bytesRead <= 0) {
                        onDoneNormally(Result.SUCCESS); // no data, leaving
                        return;
                    }
                }
                stepBit = config.stepBit.getValue();
                stepX = config.stepX.getValue();
                stepZ = config.stepZ.getValue();
                stepY = config.stepY.getValue();
                bufferByteIndex = 0;
                totalBytes = 0;
                block0 = config.block_zero.getValue().getDefaultState();
                block1 = config.block_one.getValue().getDefaultState();
                localX = localY = localZ = 0;
                currentPos = pos.mutableCopy();
                working = true;
            } catch (Exception e) {
                sendFeedback("ERROR: Exception thrown during writing initialization: " + e.getMessage());
                working = false;
            }
        }
    }

    @Override
    public void tick() {
        synchronized (workingStateLock) {
            if (!working) return;

            try {
                long bytesPerTick = config.bytesPerTickWrite.getValue();
                for (long tickByteIndex = 0; tickByteIndex < bytesPerTick; tickByteIndex++) {
                    if (bufferByteIndex == bytesRead) {
                        bytesRead = fileStream.read(buffer, 0, BUFFER_LENGTH);
                        if (bytesRead == -1) {
                            onDoneNormally(Result.SUCCESS);
                            return;
                        }
                        bufferByteIndex = 0;
                    }

                    byte currentByte = buffer[bufferByteIndex];
                    byte mask = 1;
                    for (int b = 0; b < 8; b++) {
                        int bit = currentByte & mask;
                        mask <<= 1;
                        if (bit == 0) world.setBlockState(currentPos, block0, Block.NOTIFY_ALL);
                        else world.setBlockState(currentPos, block1, Block.NOTIFY_ALL);
                        currentPos.move(0, stepBit, 0);
                    }
                    bufferByteIndex++;
                    totalBytes++;

                    if ((totalBytes & LOG_NOTIFICATION_BYTES) == 0) {
                        IOManager.hardDrive.logger.info("STILL WRITING, {} bytes so far.", totalBytes);
                    }

                    localX++;
                    currentPos.move(stepX, -stepBit * 8, 0);
                    if (localX >= sizeX) {
                        localX = 0;
                        localZ++;
                        currentPos.setX(position.getX());
                        currentPos.move(0, 0, stepZ);

                        if (localZ >= sizeZ) {
                            localZ = 0;
                            localY++;
                            currentPos.setZ(position.getZ());
                            currentPos.move(0, stepY, 0);
                            if (currentPos.getY() + stepY > 320) {
                                onDoneNormally(Result.CUT_BY_WORLD_EDGE);
                                return;
                            }

                            if (localY >= sizeY) {
                                onDoneNormally(Result.NOT_ENOUGH_SPACE);
                                return;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                sendFeedback("ERROR: Exception thrown during writing: " + e.getMessage());
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
            } catch (Exception ignored) {
            }
            switch (res) {
                case SUCCESS:
                    sendFeedback("Writing done.");
                    break;
                case NOT_ENOUGH_SPACE:
                    sendFeedback("ERROR: There's not enough space in the area, written as much as possible.");
                    break;
                case CUT_BY_WORLD_EDGE:
                    String msg = "ERROR: World border was reached while writing the file, written as much as possible.";
                    if (config.embedLength.getValue())
                        msg += " EMBEDDED LENGTH IS INVALID!";
                    sendFeedback(msg);
                    break;
                case INTERRUPTED:
                    sendFeedback("Writing interrupted manually.");
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

    @Override
    public String describe() {
        synchronized (workingStateLock) {
            String msg = "Writing by %s at (%d, %d, %d), file %s";
            if (!working) msg = "(DONE) " + msg;
            return String.format(msg, player.getNameForScoreboard(), position.getX(), position.getY(), position.getZ(),
                    originalFile.getPath());
        }
    }

    private void sendFeedback(String msg) {
        if (player != null) player.sendMessage(Text.literal(msg), false);
    }

    enum Result {
        SUCCESS,
        NOT_ENOUGH_SPACE,
        CUT_BY_WORLD_EDGE,
        INTERRUPTED,
    }
}
