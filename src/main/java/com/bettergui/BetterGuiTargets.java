package com.bettergui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.Generic3x3ContainerScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.HopperScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.util.Identifier;

import java.util.Set;

public final class BetterGuiTargets {
	
	private static final Set<String> TEXTURES = Set.of(
			"textures/gui/container/inventory.png",
			"textures/gui/container/generic_54.png",
			"textures/gui/container/shulker_box.png",
			"textures/gui/container/hopper.png",
			"textures/gui/container/dispenser.png",
			"textures/gui/recipe_book.png"
	);

	private BetterGuiTargets() {
	}

	public static boolean isTarget(Screen screen) {
		BetterGuiConfig config = BetterGuiConfig.get();
		if (!config.enabled || screen == null) {
			return false;
		}
		if (screen instanceof InventoryScreen
				|| screen instanceof GenericContainerScreen
				|| screen instanceof ShulkerBoxScreen
				|| screen instanceof HopperScreen
				|| screen instanceof Generic3x3ContainerScreen) {
			return true;
		}
		return config.applyToAllContainers
				&& screen instanceof HandledScreen
				;
	}

	
	public static boolean isTargetScreenOpen() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null) {
			return false;
		}
		return isTarget(client.currentScreen);
	}

	
	public static boolean shouldProcess(Identifier texture) {
		if (texture == null || !isTargetScreenOpen()) {
			return false;
		}
		String path = texture.getPath();
		if (path.startsWith("textures/gui/container/creative_inventory/")) {
			return false;
		}
		if (TEXTURES.contains(path)) {
			return true;
		}
		return BetterGuiConfig.get().applyToAllContainers && path.startsWith("textures/gui/container/");
	}
}
