package com.scalpelred.harddrive;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class HardDrive implements ModInitializer {

    public final Logger logger = LoggerFactory.getLogger("harddrive");
	public final HardDriveConfig config = new HardDriveConfig(this);
	public final IOManager IOManager = new IOManager(this);

	@Override
	public void onInitialize() {
		logger.info("{} Megabytes of information!", (Math.random() < 0.95) ? "Yowza!" : "oOwOo");

		config.loadOrCreate();
		try {
			IOManager.createInputDirIfNeeded();
		}
		catch (IOException e) {
			logger.error("Failed to create input dir: {}", e.getMessage());
		}
		try {
			IOManager.createOutputDirIfNeeded();
		}
		catch (IOException e) {
			logger.error("Failed to create output dir: {}", e.getMessage());
		}
		new HardDriveCommand(this);
		new WriteToWorldCommand(this);
		new ReadFromWorldCommand(this);

		ServerTickEvents.START_SERVER_TICK.register((startTick) -> {
			IOManager.tick();
		});
	}
}