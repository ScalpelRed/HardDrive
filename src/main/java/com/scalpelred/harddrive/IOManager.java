package com.scalpelred.harddrive;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class IOManager {

    public final HardDrive hardDrive;

    private File inputDir;
    private File outputDir;

    private final Object workingStateLock = new Object();
    private IIOWorker currentWorker;

    public IOManager(HardDrive hardDrive) {
        this.hardDrive = hardDrive;

        inputDir = Paths.get(System.getProperty("user.dir"), "harddrive_in").toFile();
        try {
            createInputDirIfNeeded();
        } catch (IOException e) {
            hardDrive.logger.error("Error creating input folder: {}", e.getMessage());
        }

        outputDir = Paths.get(System.getProperty("user.dir"), "harddrive_out").toFile();
        try {
            createOutputDirIfNeeded();
        } catch (IOException e) {
            hardDrive.logger.error("Error creating output folder: {}", e.getMessage());
        }

        currentWorker = NullWorker.INSTANCE;
    }

    public File getInputDir() throws IOException {
        createInputDirIfNeeded();
        return inputDir;
    }

    public File getOutputDir() throws IOException {
        createOutputDirIfNeeded();
        return outputDir;
    }

    public boolean createInputDirIfNeeded() throws IOException {
        if (!inputDir.exists()) {
            inputDir = Files.createDirectories(inputDir.toPath()).toFile().getCanonicalFile();
            return true;
        } else return false;
    }

    public boolean createOutputDirIfNeeded() throws IOException {
        if (!outputDir.exists()) {
            outputDir = Files.createDirectories(outputDir.toPath()).toFile().getCanonicalFile();
            return true;
        } else return false;
    }

    public void startWorker(IIOWorker worker) {
        synchronized (workingStateLock) {
            currentWorker = worker;
        }
    }

    public void tick() {
        synchronized (workingStateLock) {
            currentWorker.tick();
        }
    }

    public boolean isBusy() {
        synchronized (workingStateLock) {
            return currentWorker.isWorking();
        }
    }

    public boolean stopWorker() {
        synchronized (workingStateLock) {
            boolean res = currentWorker.isWorking();
            currentWorker.stop();
            return res;
        }
    }

    public String describeOperation() {
        synchronized (workingStateLock) {
            return currentWorker.describe();
        }
    }
}
