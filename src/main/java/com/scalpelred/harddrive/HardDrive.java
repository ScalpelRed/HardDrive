package com.scalpelred.harddrive;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HardDrive implements ModInitializer {

    public final Logger logger = LoggerFactory.getLogger("harddrive");
	public final HardDriveConfig config = new HardDriveConfig(this);
	public final WorldIO worldIO = new WorldIO(this);

	@Override
	public void onInitialize() {

		String spl = "Megabytes of information!";
		if (Math.random() < 0.95) spl = "Yowza! " + spl;
		else spl = "oOwOo " + spl;
		logger.info(spl);

		config.loadOrCreate();

		new WriteToWorldCommand(this);
	}
}