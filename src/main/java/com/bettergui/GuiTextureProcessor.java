package com.bettergui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class GuiTextureProcessor {
	
	private static final Map<String, int[]> PANEL_SIZES = new HashMap<>();
	private static final Map<Identifier, Identifier> CACHE = new HashMap<>();

	static {
		PANEL_SIZES.put("textures/gui/container/inventory.png", new int[] { 176, 166 });
		PANEL_SIZES.put("textures/gui/container/generic_54.png", new int[] { 176, 222 });
		PANEL_SIZES.put("textures/gui/container/shulker_box.png", new int[] { 176, 166 });
		PANEL_SIZES.put("textures/gui/container/hopper.png", new int[] { 176, 133 });
		PANEL_SIZES.put("textures/gui/container/dispenser.png", new int[] { 176, 166 });
		PANEL_SIZES.put("textures/gui/recipe_book.png", new int[] { 148, 167 });
	}

	private GuiTextureProcessor() {
	}

	
	public static void invalidate() {
		CACHE.clear();
	}

	
	public static Identifier get(Identifier original) {
		Identifier cached = CACHE.get(original);
		if (cached != null) {
			return cached;
		}

		Identifier result = original;
		try {
			result = process(original);
		} catch (Throwable t) {
			BetterGuiClient.LOGGER.warn("[BetterGUI] failed to process texture {}", original, t);
		}
		CACHE.put(original, result);
		return result;
	}

	private static Identifier process(Identifier original) throws Exception {
		MinecraftClient client = MinecraftClient.getInstance();
		Optional<Resource> resource = client.getResourceManager().getResource(original);
		if (resource.isEmpty()) {
			return original;
		}

		NativeImage image;
		try (InputStream input = resource.get().getInputStream()) {
			image = NativeImage.read(input);
		}

		applyTranslucency(image, original.getPath(), PANEL_SIZES.get(original.getPath()));

		Identifier processed = new Identifier(BetterGuiClient.MOD_ID,
				"dynamic/" + original.getNamespace() + "/" + original.getPath());
		NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
		texture.setFilter(false, false);
		texture.upload();
		client.getTextureManager().registerTexture(processed, texture);
		return processed;
	}

	private static void applyTranslucency(NativeImage image, String path, int[] panelSize) {
		BetterGuiConfig config = BetterGuiConfig.get();
		int width = image.getWidth();
		int height = image.getHeight();
		int panelWidth = panelSize != null ? panelSize[0] : width;
		int panelHeight = panelSize != null ? panelSize[1] : height;
		int frame = config.frameThickness;
		boolean recipeBook = path.endsWith("recipe_book.png");

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int color = image.getColor(x, y);
				int alpha = (color >>> 24) & 0xFF;
				if (alpha == 0) {
					continue;
				}

				int red = color & 0xFF;
				int green = (color >>> 8) & 0xFF;
				int blue = (color >>> 16) & 0xFF;

				boolean insidePanel = x < panelWidth && y < panelHeight;
				boolean onFrame = insidePanel && (x < frame || y < frame
						|| x >= panelWidth - frame || y >= panelHeight - frame);

				int newAlpha;
				if (!insidePanel) {
					continue;
				}
				if (onFrame) {
					newAlpha = 0;
				} else if (red == 198 && green == 198 && blue == 198) {
					newAlpha = recipeBook ? config.effectiveHighlightAlpha() : config.panelAlpha;
				} else if (red == 139 && green == 139 && blue == 139) {
					newAlpha = config.effectiveSlotAlpha();
				} else if (red == 55 && green == 55 && blue == 55) {
					newAlpha = recipeBook ? config.panelAlpha : config.effectiveSlotBorderAlpha();
				} else if (red == 255 && green == 255 && blue == 255) {
					newAlpha = config.effectiveHighlightAlpha();
				} else if (red == 85 && green == 85 && blue == 85) {
					newAlpha = config.effectiveHighlightAlpha();
				} else if (red == 0 && green == 0 && blue == 0) {
					newAlpha = config.darkAlpha;
				} else if (red == 33 && green == 33 && blue == 33) {
					newAlpha = config.darkAlpha;
				} else {
					newAlpha = alpha;
				}

				if (newAlpha != alpha) {
					image.setColor(x, y, (color & 0x00FFFFFF) | (newAlpha << 24));
				}
			}
		}
	}
}
