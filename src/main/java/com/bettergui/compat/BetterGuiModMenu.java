package com.bettergui.compat;

import com.bettergui.config.BetterGuiConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class BetterGuiModMenu implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return (ConfigScreenFactory<BetterGuiConfigScreen>) BetterGuiConfigScreen::new;
	}
}
