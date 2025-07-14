package com.scalpelred.harddrive;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HardDrive implements ModInitializer {

    public final Logger logger = LoggerFactory.getLogger("harddrive");
	public final HardDriveConfig config = new HardDriveConfig(this);
	public final IOManager IOManager = new IOManager(this);

	@Override
	public void onInitialize() {

		String spl = "Megabytes of information!";
		if (Math.random() < 0.95) spl = "Yowza! " + spl;
		else spl = "oOwOo " + spl;
		logger.info(spl);

		config.loadOrCreate();
		new HardDriveCommand(this);
		new WriteToWorldCommand(this);
		new ReadFromWorldCommand(this);

		ServerTickEvents.START_SERVER_TICK.register((startTick) -> {
			IOManager.tick();
		});
	}
}