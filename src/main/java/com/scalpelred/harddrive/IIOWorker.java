package com.scalpelred.harddrive;

public interface IIOWorker {

    void tick();
    boolean isWorking();
    void stop();
    String describe();
}
