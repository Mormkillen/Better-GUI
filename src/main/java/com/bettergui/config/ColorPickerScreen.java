package com.bettergui.config;

import com.bettergui.BetterGuiConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.function.IntConsumer;

public class ColorPickerScreen extends Screen {
	private static final String[] CHANNEL_NAMES = { "H", "S", "V", "R", "G", "B", "A" };
	private static final int SV_COLUMNS = 48;
	private static final int HUE_BANDS = 48;
	private static final int SLIDER_BANDS = 24;

	private final Screen parent;
	private final IntConsumer onColorChanged;

	private int color;
	private float[] hsv;

	private TextFieldWidget hexField;
	private boolean updatingHexField;
	private int dragging = -1;

	private int panelX;
	private int panelY;
	private int panelWidth;
	private int panelHeight;
	private int svX;
	private int svY;
	private int svSize;
	private int hueX;
	private int hueWidth = 10;
	private int sliderX;
	private int sliderWidth;
	private int sliderTop;
	private int rowStep = 22;

	public ColorPickerScreen(Screen parent, int initialColor, IntConsumer onColorChanged) {
		super(Text.translatable("bettergui.picker.title"));
		this.parent = parent;
		this.onColorChanged = onColorChanged;
		this.color = initialColor;
		this.hsv = rgbToHsv(initialColor);
	}

	@Override
	protected void init() {
		this.panelWidth = Math.min(380, this.width - 20);
		this.panelHeight = 214;
		this.panelX = (this.width - this.panelWidth) / 2;
		this.panelY = Math.max(4, (this.height - this.panelHeight) / 2);

		this.svSize = 90;
		this.svX = this.panelX + 10;
		this.svY = this.panelY + 22;
		this.hueX = this.svX + this.svSize + 8;
		this.sliderX = this.hueX + this.hueWidth + 18;
		this.sliderWidth = Math.max(60, this.panelWidth - (this.sliderX - this.panelX) - 60);
		this.sliderTop = this.panelY + 26;

		this.hexField = new TextFieldWidget(this.textRenderer, this.panelX + 96, this.panelY + this.panelHeight - 24,
				86, 18, Text.literal("HEX"));
		this.hexField.setMaxLength(9);
		this.hexField.setText(toHex(this.color));
		this.hexField.setChangedListener(text -> {
			if (this.updatingHexField) {
				return;
			}
			int parsed = parseHex(text);
			if (parsed != -1) {
				this.applyColor(parsed, false);
			}
		});
		this.addDrawableChild(this.hexField);

		this.addDrawableChild(ButtonWidget.builder(Text.translatable("bettergui.reset"), button -> {
			BetterGuiConfig.get().resetPanelColor();
			this.applyColor(BetterGuiConfig.get().getPanelColor(), true);
		}).dimensions(this.panelX + 10, this.panelY + this.panelHeight - 25, 44, 20).build());

		this.addDrawableChild(ButtonWidget.builder(Text.translatable("bettergui.done"), button -> this.close())
				.dimensions(this.panelX + this.panelWidth - 74, this.panelY + this.panelHeight - 25, 64, 20).build());
	}

	private void applyColor(int argb, boolean syncHexField) {
		this.color = argb;
		this.hsv = rgbToHsv(argb);
		if (syncHexField && this.hexField != null) {
			this.updatingHexField = true;
			this.hexField.setText(toHex(argb));
			this.updatingHexField = false;
		}
		this.onColorChanged.accept(argb);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		this.renderBackground(context);
		context.fill(this.panelX, this.panelY, this.panelX + this.panelWidth, this.panelY + this.panelHeight, 0xF0101010);
		context.fill(this.panelX, this.panelY, this.panelX + this.panelWidth, this.panelY + 1, 0x60FFFFFF);
		context.drawTextWithShadow(this.textRenderer, this.title, this.panelX + 10, this.panelY + 7, 0xFFFFFF);

		this.drawSvSquare(context);
		this.drawHueStrip(context);
		this.drawChannelSliders(context);

		int previewX = this.panelX + 10;
		int previewY = this.svY + this.svSize + 10;
		this.drawCheckerboard(context, previewX, previewY, 40, 40);
		context.fill(previewX, previewY, previewX + 40, previewY + 40, this.color);
		context.fill(previewX, previewY, previewX + 40, previewY + 1, 0x60FFFFFF);
		context.fill(previewX, previewY + 39, previewX + 40, previewY + 40, 0x60FFFFFF);

		context.drawTextWithShadow(this.textRenderer, Text.translatable("bettergui.hex"), this.panelX + 62,
				this.panelY + this.panelHeight - 19, 0xFFFFFF);

		super.render(context, mouseX, mouseY, delta);
	}

	private void drawSvSquare(DrawContext context) {
		for (int i = 0; i < SV_COLUMNS; i++) {
			float s = i / (float) (SV_COLUMNS - 1);
			int x0 = this.svX + this.svSize * i / SV_COLUMNS;
			int x1 = this.svX + this.svSize * (i + 1) / SV_COLUMNS;
			context.fill(x0, this.svY, Math.max(x1, x0 + 1), this.svY + this.svSize,
					0xFF000000 | hsvToRgb(this.hsv[0], s, 1.0f));
		}
		context.fillGradient(this.svX, this.svY, this.svX + this.svSize, this.svY + this.svSize,
				0x00000000, 0xFF000000);

		int markerX = this.svX + Math.round(this.hsv[1] * this.svSize);
		int markerY = this.svY + Math.round((1.0f - this.hsv[2]) * this.svSize);
		context.fill(this.svX, markerY, this.svX + this.svSize, markerY + 1, 0xFFFFFFFF);
		context.fill(markerX, this.svY, markerX + 1, this.svY + this.svSize, 0xFFFFFFFF);
	}

	private void drawHueStrip(DrawContext context) {
		for (int i = 0; i < HUE_BANDS; i++) {
			int y0 = this.svY + this.svSize * i / HUE_BANDS;
			int y1 = this.svY + this.svSize * (i + 1) / HUE_BANDS;
			context.fill(this.hueX, y0, this.hueX + this.hueWidth, Math.max(y1, y0 + 1),
					0xFF000000 | hsvToRgb(i / (float) (HUE_BANDS - 1), 1.0f, 1.0f));
		}
		int markerY = this.svY + Math.round(this.hsv[0] * this.svSize);
		context.fill(this.hueX - 2, markerY, this.hueX + this.hueWidth + 2, markerY + 1, 0xFFFFFFFF);
	}

	private void drawChannelSliders(DrawContext context) {
		for (int i = 0; i < 7; i++) {
			int y = this.sliderTop + i * this.rowStep;
			context.drawTextWithShadow(this.textRenderer, Text.literal(CHANNEL_NAMES[i] + ":"),
					this.sliderX - 14, y + 1, 0xFFFFFF);
			if (i == 6) {
				this.drawCheckerboard(context, this.sliderX, y, this.sliderWidth, 10);
			}
			for (int b = 0; b < SLIDER_BANDS; b++) {
				float t = b / (float) (SLIDER_BANDS - 1);
				int x0 = this.sliderX + this.sliderWidth * b / SLIDER_BANDS;
				int x1 = this.sliderX + this.sliderWidth * (b + 1) / SLIDER_BANDS;
				context.fill(x0, y, Math.max(x1, x0 + 1), y + 10, channelColor(i, t));
			}
			int markerX = this.sliderX + Math.round(this.channelValue(i) * this.sliderWidth);
			context.fill(markerX, y - 1, markerX + 1, y + 11, 0xFFFFFFFF);
			int boxX = this.sliderX + this.sliderWidth + 6;
			context.fill(boxX, y - 1, boxX + 34, y + 11, 0xFF101010);
			context.fill(boxX, y - 1, boxX + 34, y, 0xFF909090);
			context.fill(boxX, y + 10, boxX + 34, y + 11, 0xFF909090);
			context.fill(boxX, y - 1, boxX + 1, y + 11, 0xFF909090);
			context.fill(boxX + 33, y - 1, boxX + 34, y + 11, 0xFF909090);
			context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(String.valueOf(this.channelInt(i))),
					boxX + 17, y + 1, 0xFFFFFF);
		}
	}

	private void drawCheckerboard(DrawContext context, int x, int y, int width, int height) {
		for (int i = 0; i < width; i += 8) {
			for (int j = 0; j < height; j += 8) {
				boolean dark = ((i / 8) + (j / 8)) % 2 == 0;
				context.fill(x + i, y + j, Math.min(x + i + 8, x + width), Math.min(y + j + 8, y + height),
						dark ? 0xFF404040 : 0xFF808080);
			}
		}
	}

	private int channelColor(int channel, float t) {
		int r = (this.color >> 16) & 0xFF;
		int g = (this.color >> 8) & 0xFF;
		int b = this.color & 0xFF;
		return switch (channel) {
			case 0 -> 0xFF000000 | hsvToRgb(t, 1.0f, 1.0f);
			case 1 -> 0xFF000000 | hsvToRgb(this.hsv[0], t, this.hsv[2]);
			case 2 -> 0xFF000000 | hsvToRgb(this.hsv[0], this.hsv[1], t);
			case 3 -> 0xFF000000 | (Math.round(t * 255) << 16);
			case 4 -> 0xFF000000 | (Math.round(t * 255) << 8);
			case 5 -> 0xFF000000 | Math.round(t * 255);
			default -> (Math.round(t * 255) << 24) | (r << 16) | (g << 8) | b;
		};
	}

	private float channelValue(int channel) {
		int r = (this.color >> 16) & 0xFF;
		int g = (this.color >> 8) & 0xFF;
		int b = this.color & 0xFF;
		int a = (this.color >>> 24) & 0xFF;
		return switch (channel) {
			case 0 -> this.hsv[0];
			case 1 -> this.hsv[1];
			case 2 -> this.hsv[2];
			case 3 -> r / 255.0f;
			case 4 -> g / 255.0f;
			case 5 -> b / 255.0f;
			default -> a / 255.0f;
		};
	}

	private int channelInt(int channel) {
		int value = Math.round(this.channelValue(channel) * 255.0f);
		return Math.max(0, Math.min(255, value));
	}

	private void setChannel(int channel, float t) {
		float value = Math.max(0.0f, Math.min(1.0f, t));
		int r = (this.color >> 16) & 0xFF;
		int g = (this.color >> 8) & 0xFF;
		int b = this.color & 0xFF;
		int a = (this.color >>> 24) & 0xFF;
		switch (channel) {
			case 0 -> this.hsv[0] = value;
			case 1 -> this.hsv[1] = value;
			case 2 -> this.hsv[2] = value;
			case 3 -> r = Math.round(value * 255);
			case 4 -> g = Math.round(value * 255);
			case 5 -> b = Math.round(value * 255);
			default -> a = Math.round(value * 255);
		}
		int rgb = channel <= 2 ? hsvToRgb(this.hsv[0], this.hsv[1], this.hsv[2]) : ((r << 16) | (g << 8) | b);
		this.applyColor((a << 24) | (rgb & 0x00FFFFFF), true);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (super.mouseClicked(mouseX, mouseY, button)) {
			return true;
		}
		if (this.isInside(mouseX, mouseY, this.svX, this.svY, this.svSize, this.svSize)) {
			this.dragging = 0;
			return this.updateFromPointer(mouseX, mouseY);
		}
		if (this.isInside(mouseX, mouseY, this.hueX, this.svY, this.hueWidth, this.svSize)) {
			this.dragging = 1;
			return this.updateFromPointer(mouseX, mouseY);
		}
		for (int i = 0; i < 7; i++) {
			int y = this.sliderTop + i * this.rowStep;
			if (this.isInside(mouseX, mouseY, this.sliderX, y - 2, this.sliderWidth, 14)) {
				this.dragging = 2 + i;
				return this.updateFromPointer(mouseX, mouseY);
			}
		}
		return false;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (this.dragging >= 0) {
			return this.updateFromPointer(mouseX, mouseY);
		}
		return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		this.dragging = -1;
		return super.mouseReleased(mouseX, mouseY, button);
	}

	private boolean updateFromPointer(double mouseX, double mouseY) {
		int alpha = this.color & 0xFF000000;
		if (this.dragging == 0) {
			this.hsv[1] = clamp01((float) ((mouseX - this.svX) / this.svSize));
			this.hsv[2] = clamp01(1.0f - (float) ((mouseY - this.svY) / this.svSize));
			this.applyColor(alpha | (hsvToRgb(this.hsv[0], this.hsv[1], this.hsv[2]) & 0x00FFFFFF), true);
			return true;
		}
		if (this.dragging == 1) {
			this.hsv[0] = clamp01((float) ((mouseY - this.svY) / this.svSize));
			this.applyColor(alpha | (hsvToRgb(this.hsv[0], this.hsv[1], this.hsv[2]) & 0x00FFFFFF), true);
			return true;
		}
		if (this.dragging >= 2) {
			this.setChannel(this.dragging - 2, (float) ((mouseX - this.sliderX) / this.sliderWidth));
			return true;
		}
		return false;
	}

	private static float clamp01(float value) {
		return Math.max(0.0f, Math.min(1.0f, value));
	}

	private boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	@Override
	public void close() {
		BetterGuiConfig.get().save();
		if (this.client != null) {
			this.client.setScreen(this.parent);
		}
	}

	public static String toHex(int argb) {
		return String.format("#%08X", argb);
	}

	public static int parseHex(String text) {
		String value = text.trim();
		if (value.startsWith("#")) {
			value = value.substring(1);
		}
		try {
			if (value.length() == 6) {
				return 0xFF000000 | (int) Long.parseLong(value, 16);
			}
			if (value.length() == 8) {
				return (int) Long.parseLong(value, 16);
			}
		} catch (NumberFormatException ignored) {
			return -1;
		}
		return -1;
	}

	private static float[] rgbToHsv(int argb) {
		float r = ((argb >> 16) & 0xFF) / 255.0f;
		float g = ((argb >> 8) & 0xFF) / 255.0f;
		float b = (argb & 0xFF) / 255.0f;
		float max = Math.max(r, Math.max(g, b));
		float min = Math.min(r, Math.min(g, b));
		float delta = max - min;
		float hue;
		if (delta == 0.0f) {
			hue = 0.0f;
		} else if (max == r) {
			hue = ((g - b) / delta) / 6.0f;
		} else if (max == g) {
			hue = (2.0f + (b - r) / delta) / 6.0f;
		} else {
			hue = (4.0f + (r - g) / delta) / 6.0f;
		}
		if (hue < 0.0f) {
			hue += 1.0f;
		}
		return new float[] { hue, max == 0.0f ? 0.0f : delta / max, max };
	}

	public static int hsvToRgb(float hue, float saturation, float value) {
		float h = ((hue % 1.0f) + 1.0f) % 1.0f * 6.0f;
		int i = (int) h;
		float f = h - i;
		float p = value * (1.0f - saturation);
		float q = value * (1.0f - f * saturation);
		float t = value * (1.0f - (1.0f - f) * saturation);
		float r;
		float g;
		float b;
		switch (i % 6) {
			case 0 -> { r = value; g = t; b = p; }
			case 1 -> { r = q; g = value; b = p; }
			case 2 -> { r = p; g = value; b = t; }
			case 3 -> { r = p; g = q; b = value; }
			case 4 -> { r = t; g = p; b = value; }
			default -> { r = value; g = p; b = q; }
		}
		return (Math.round(r * 255) << 16) | (Math.round(g * 255) << 8) | Math.round(b * 255);
	}
}
