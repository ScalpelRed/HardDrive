package com.scalpelred.harddrive;

public class NullWorker implements IIOWorker {

    public static final NullWorker INSTANCE = new NullWorker();

    private NullWorker() {

    }

    @Override
    public void tick() {

    }

    @Override
    public boolean isWorking() {
        return false;
    }

    @Override
    public void stop() {

    }

    @Override
    public String describe() {
        return "No operation running.";
    }
}
