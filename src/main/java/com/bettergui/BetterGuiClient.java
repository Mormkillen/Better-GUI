package com.bettergui;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BetterGuiClient implements ClientModInitializer {
	public static final String MOD_ID = "bettergui";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		BetterGuiConfig.load();
		BetterGuiClient.LOGGER.info("[BetterGUI] loaded (enabled={}, radius={})",
				BetterGuiConfig.get().enabled, BetterGuiConfig.get().blurRadius);
	}
}
