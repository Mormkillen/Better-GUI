package com.bettergui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class BetterGuiConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String FILE_NAME = "better-gui.json";

	private static BetterGuiConfig instance = new BetterGuiConfig();

	
	public boolean enabled = true;
	
	public boolean applyToAllContainers = true;

	
	public float blurRadius = 0.0f;

	
	public boolean translucent = true;
	public boolean rainbow = false;
	public float rainbowSpeed = 1.0f;
	public boolean gradient = false;
	public int cornerTopLeft = 0x3AFFFFFF;
	public int cornerTopRight = 0x3AFFFFFF;
	public int cornerBottomLeft = 0x1CFFFFFF;
	public int cornerBottomRight = 0x1CFFFFFF;
	
	public boolean blurFlipVertical = true;

	
	public int cornerRadius = 6;
	
	public int panelTopColor = 0x3AFFFFFF;
	
	public int panelBottomColor = 0x1CFFFFFF;
	
	public int borderColor = 0x33FFFFFF;
	
	public boolean shadow = true;
	
	public int shadowColor = 0x40000000;

	
	public int dimTopColor = 0x28000000;
	public int dimBottomColor = 0x40000000;

	
	public int panelAlpha = 0;
	
	public int slotAlpha = 0;
	
	public int slotBorderAlpha = 110;
	
	public int highlightAlpha = 90;
	
	public int darkAlpha = 0;
	
	public int frameThickness = 4;

	
	public boolean recolorLabels = true;
	
	public int labelColor = 0xFFE8E8E8;

	public static BetterGuiConfig get() {
		return instance;
	}

	
	public int getPanelColor() {
		return this.panelTopColor;
	}

	
	public void setPanelColor(int argb) {
		this.panelTopColor = argb;
		this.panelBottomColor = argb;
	}

	
	public void resetPanelColor() {
		BetterGuiConfig defaults = new BetterGuiConfig();
		this.panelTopColor = defaults.panelTopColor;
		this.panelBottomColor = defaults.panelBottomColor;
	}
	public int getCornerColor(int index) {
		return switch (index) {
			case 0 -> this.cornerTopLeft;
			case 1 -> this.cornerTopRight;
			case 2 -> this.cornerBottomLeft;
			default -> this.cornerBottomRight;
		};
	}

	public void setCornerColor(int index, int argb) {
		switch (index) {
			case 0 -> this.cornerTopLeft = argb;
			case 1 -> this.cornerTopRight = argb;
			case 2 -> this.cornerBottomLeft = argb;
			default -> this.cornerBottomRight = argb;
		}
	}

	public boolean plainColorEnabled() {
		return !this.rainbow && !this.gradient;
	}
	public boolean blurEnabled() {
		return this.translucent && this.blurRadius > 0.0f;
	}

	public int effectivePanelTop() {
		return this.translucent ? this.panelTopColor : (this.panelTopColor | 0xFF000000);
	}

	public int effectivePanelBottom() {
		return this.translucent ? this.panelBottomColor : (this.panelBottomColor | 0xFF000000);
	}

	public int effectiveSlotAlpha() {
		return this.translucent ? this.slotAlpha : 255;
	}

	public int effectiveSlotBorderAlpha() {
		return this.translucent ? this.slotBorderAlpha : 255;
	}

	public int effectiveHighlightAlpha() {
		return this.translucent ? this.highlightAlpha : 255;
	}

	

	public static void load() {
		Path path = configPath();
		if (Files.exists(path)) {
			try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
				BetterGuiConfig loaded = GSON.fromJson(reader, BetterGuiConfig.class);
				if (loaded != null) {
					instance = loaded;
				}
			} catch (Exception e) {
				BetterGuiClient.LOGGER.warn("[BetterGUI] failed to read config, using defaults", e);
			}
		}
		instance.sanitize();
		instance.save();
	}

	public void save() {
		Path path = configPath();
		try {
			Files.createDirectories(path.getParent());
			try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
				GSON.toJson(this, writer);
			}
		} catch (Exception e) {
			BetterGuiClient.LOGGER.warn("[BetterGUI] failed to write config", e);
		}
	}

	private static Path configPath() {
		return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
	}

	private void sanitize() {
		this.blurRadius = clamp(this.blurRadius, 0.0f, 64.0f);
		this.cornerRadius = clamp(this.cornerRadius, 0, 64);
		this.frameThickness = clamp(this.frameThickness, 0, 16);
		this.panelAlpha = clamp(this.panelAlpha, 0, 255);
		this.slotAlpha = clamp(this.slotAlpha, 0, 255);
		this.slotBorderAlpha = clamp(this.slotBorderAlpha, 0, 255);
		this.highlightAlpha = clamp(this.highlightAlpha, 0, 255);
		this.darkAlpha = clamp(this.darkAlpha, 0, 255);
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private static float clamp(float value, float min, float max) {
		return Math.max(min, Math.min(max, value));
	}
}
