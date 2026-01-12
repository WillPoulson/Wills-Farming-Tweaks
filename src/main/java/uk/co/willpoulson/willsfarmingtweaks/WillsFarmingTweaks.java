package uk.co.willpoulson.willsfarmingtweaks;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.willpoulson.willsfarmingtweaks.config.ConfigManager;

public class WillsFarmingTweaks implements ModInitializer {
	public static final String MOD_ID = "wills-farming-tweaks";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ConfigManager.load();
		LOGGER.info("Will's: Farming Tweaks Initialised");
	}
}