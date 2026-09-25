package com.bettergui;

import com.bettergui.config.ColorPickerScreen;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.Window;
import org.joml.Matrix4f;
import net.minecraft.util.Identifier;

public final class BetterGuiRenderer {
	private BetterGuiRenderer() {
	}

	
	public static void drawDim(DrawContext context, int width, int height) {
		BetterGuiConfig config = BetterGuiConfig.get();
		context.fillGradient(0, 0, width, height, config.dimTopColor, config.dimBottomColor);
	}

	public static void drawPanel(DrawContext context, int x, int y, int width, int height, float delta) {
		BetterGuiConfig config = BetterGuiConfig.get();
		MinecraftClient client = MinecraftClient.getInstance();
		Window window = client.getWindow();
		double scale = window.getScaleFactor();
		int framebufferWidth = client.getFramebuffer().textureWidth;
		int framebufferHeight = client.getFramebuffer().textureHeight;
		int radius = Math.max(0, Math.min(config.cornerRadius, Math.min(width, height) / 2));

		if (config.shadow && (config.shadowColor >>> 24) != 0) {
			for (int ring = 3; ring >= 1; ring--) {
				int color = withAlpha(config.shadowColor, (config.shadowColor >>> 24) * ring / 10);
				int left = x - ring;
				int top = y - ring;
				int right = x + width + ring;
				int bottom = y + height + ring;
				context.fill(left, top, right, top + ring, color);
				context.fill(left, bottom - ring, right, bottom, color);
				context.fill(left, top + ring, left + ring, bottom - ring, color);
				context.fill(right - ring, top + ring, right, bottom - ring, color);
			}
		}
		if (config.blurEnabled()) {
			context.enableScissor(x, y, x + width, y + height);
			Identifier blurTexture = BlurEffect.blur(framebufferWidth, framebufferHeight, delta);
			if (blurTexture != null) {
				drawRoundedBackdrop(context, blurTexture, x, y, width, height, radius, scale,
						framebufferWidth, framebufferHeight);
			}
			context.disableScissor();
		}

		int bandRows = Math.min(radius, height / 2);
		int bandStart = bandRows;
		int bandEnd = Math.max(bandStart, height - bandRows);
		if (bandEnd > bandStart) {
			float v0 = height <= 1 ? 0.0f : bandStart / (float) (height - 1);
			float v1 = height <= 1 ? 1.0f : (bandEnd - 1) / (float) (height - 1);
			drawQuadGradient(context, x, y + bandStart, x + width, y + bandEnd,
					panelColorAt(config, 0.0f, v0), panelColorAt(config, 1.0f, v0),
					panelColorAt(config, 0.0f, v1), panelColorAt(config, 1.0f, v1));
		}
		for (int row = 0; row < bandRows; row++) {
			int inset = cornerInset(row, radius, height);
			float t = height <= 1 ? 0.0f : row / (float) (height - 1);
			drawQuadGradient(context, x + inset, y + row, x + width - inset, y + row + 1,
					panelColorAt(config, 0.0f, t), panelColorAt(config, 1.0f, t),
					panelColorAt(config, 0.0f, t), panelColorAt(config, 1.0f, t));
		}
		for (int row = bandEnd; row < height; row++) {
			int inset = cornerInset(row, radius, height);
			float t = height <= 1 ? 0.0f : row / (float) (height - 1);
			drawQuadGradient(context, x + inset, y + row, x + width - inset, y + row + 1,
					panelColorAt(config, 0.0f, t), panelColorAt(config, 1.0f, t),
					panelColorAt(config, 0.0f, t), panelColorAt(config, 1.0f, t));
		}
		if ((config.borderColor >>> 24) != 0) {
			int borderRows = Math.min(radius, height / 2);
			int borderStart = borderRows;
			int borderEnd = Math.max(borderStart, height - borderRows);
			if (borderEnd > borderStart) {
				context.fill(x, y + borderStart, x + 1, y + borderEnd, config.borderColor);
				context.fill(x + width - 1, y + borderStart, x + width, y + borderEnd, config.borderColor);
			}
			for (int row = 0; row < borderRows; row++) {
				int inset = cornerInset(row, radius, height);
				context.fill(x + inset, y + row, x + inset + 1, y + row + 1, config.borderColor);
				context.fill(x + width - inset - 1, y + row, x + width - inset, y + row + 1, config.borderColor);
			}
			for (int row = borderEnd; row < height; row++) {
				int inset = cornerInset(row, radius, height);
				context.fill(x + inset, y + row, x + inset + 1, y + row + 1, config.borderColor);
				context.fill(x + width - inset - 1, y + row, x + width - inset, y + row + 1, config.borderColor);
			}
			int topInset = cornerInset(0, radius, height);
			int bottomInset = cornerInset(height - 1, radius, height);
			context.fill(x + topInset, y, x + width - topInset, y + 1, config.borderColor);
			context.fill(x + bottomInset, y + height - 1, x + width - bottomInset, y + height, config.borderColor);
		}	}

	
	private static void drawRoundedBackdrop(DrawContext context, Identifier texture, int x, int y,
			int width, int height, int radius, double scale, int framebufferWidth, int framebufferHeight) {
		int pixelX = (int) Math.round(x * scale);
		int pixelY = (int) Math.round(y * scale);
		int pixelWidth = (int) Math.round((x + width) * scale) - pixelX;
		int pixelHeight = (int) Math.round((y + height) * scale) - pixelY;
		if (pixelWidth <= 0 || pixelHeight <= 0) {
			return;
		}
		int pixelRadius = Math.min((int) Math.round(radius * scale), Math.min(pixelWidth, pixelHeight) / 2);

		context.getMatrices().push();
		context.getMatrices().scale(1.0f / (float) scale, 1.0f / (float) scale, 1.0f);

		for (int row = 0; row < pixelHeight; row++) {
			int inset = cornerInset(row, pixelRadius, pixelHeight);
			int rowX = pixelX + inset;
			int rowWidth = pixelWidth - inset * 2;
			if (rowWidth > 0) {
				drawBlurRow(context, texture, rowX, pixelY + row, rowWidth, framebufferWidth, framebufferHeight);
			}
		}

		context.getMatrices().pop();
	}

	
	private static void drawBlurRow(DrawContext context, Identifier texture, int pixelX, int pixelY,
			int pixelWidth, int framebufferWidth, int framebufferHeight) {
		float v = BetterGuiConfig.get().blurFlipVertical ? (float) (framebufferHeight - pixelY) : (float) pixelY;
		context.drawTexture(texture, pixelX, pixelY, 0, (float) pixelX, v,
				pixelWidth, 1, framebufferWidth, framebufferHeight);
	}


	
	private static int cornerInset(int row, int radius, int height) {
		int r = Math.min(radius, height / 2);
		if (r <= 0) {
			return 0;
		}
		double dy;
		if (row < r) {
			dy = r - row - 0.5;
		} else if (row >= height - r) {
			dy = row - (height - r) + 0.5;
		} else {
			return 0;
		}
		if (dy <= 0.0) {
			return 0;
		}
		double dx = r - Math.sqrt(Math.max(0.0, (double) r * r - dy * dy));
		return (int) Math.round(dx);
	}


	private static int panelColorAt(BetterGuiConfig config, float u, float v) {
		if (config.gradient) {
			int top = lerpColor(config.cornerTopLeft, config.cornerTopRight, u);
			int bottom = lerpColor(config.cornerBottomLeft, config.cornerBottomRight, u);
			int color = lerpColor(top, bottom, v);
			return config.translucent ? color : (color | 0xFF000000);
		}
		if (config.rainbow) {
			int alpha = config.translucent ? ((config.panelTopColor >>> 24) & 0xFF) : 255;
			double time = (System.currentTimeMillis() % 600000L) / 1000.0;
			float hue = (float) ((time * config.rainbowSpeed * 0.15 + (u + (1.0f - v)) * 1.5) % 1.0);
			if (hue < 0.0f) {
				hue += 1.0f;
			}
			return (alpha << 24) | ColorPickerScreen.hsvToRgb(hue, 1.0f, 1.0f);
		}
		return lerpColor(config.effectivePanelTop(), config.effectivePanelBottom(), v);
	}

	private static void drawQuadGradient(DrawContext context, int x0, int y0, int x1, int y1,
			int topLeft, int topRight, int bottomLeft, int bottomRight) {
		Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
		RenderSystem.enableBlend();
		RenderSystem.setShader(GameRenderer::getPositionColorProgram);
		BufferBuilder buffer = Tessellator.getInstance().getBuffer();
		buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
		buffer.vertex(matrix, x0, y1, 0.0F).color(bottomLeft).next();
		buffer.vertex(matrix, x1, y1, 0.0F).color(bottomRight).next();
		buffer.vertex(matrix, x1, y0, 0.0F).color(topRight).next();
		buffer.vertex(matrix, x0, y0, 0.0F).color(topLeft).next();
		BufferRenderer.drawWithGlobalProgram(buffer.end());
	}
	private static int withAlpha(int color, int alpha) {
		return (color & 0x00FFFFFF) | (Math.max(0, Math.min(255, alpha)) << 24);
	}

	private static int lerpColor(int from, int to, float t) {
		int a = lerpChannel(from >>> 24, to >>> 24, t);
		int r = lerpChannel((from >>> 16) & 0xFF, (to >>> 16) & 0xFF, t);
		int g = lerpChannel((from >>> 8) & 0xFF, (to >>> 8) & 0xFF, t);
		int b = lerpChannel(from & 0xFF, to & 0xFF, t);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	private static int lerpChannel(int from, int to, float t) {
		return Math.round(from + (to - from) * t);
	}
}
