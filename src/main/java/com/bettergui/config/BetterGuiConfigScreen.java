package com.bettergui.config;

import com.bettergui.BetterGuiConfig;
import com.bettergui.BetterGuiRenderer;
import com.bettergui.GuiTextureProcessor;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.function.DoubleConsumer;
import java.util.function.IntSupplier;
import java.util.function.DoubleSupplier;

public class BetterGuiConfigScreen extends Screen {
	private static final double MAX_BLUR = 32.0;
	private static final int SLOT_ALPHA_TRANSLUCENT = 0;
	private static final int SLOT_ALPHA_OPAQUE = 255;
	private static final int SLOT_BORDER_ALPHA_TRANSLUCENT = 110;
	private static final int SLOT_HIGHLIGHT_ALPHA_TRANSLUCENT = 90;

	private static final int BUTTON_FILL = 0xE0242424;
	private static final int BUTTON_FILL_HOVER = 0xE0343434;
	private static final int BUTTON_BORDER = 0x55FFFFFF;
	private static final int DONE_FILL = 0xFF3D6B3F;
	private static final int DONE_FILL_HOVER = 0xFF4C804E;
	private static final int SUBTITLE_COLOR = 0xFFBE8C55;
	private static final String[] CORNER_NAMES = { "bettergui.corner.tl", "bettergui.corner.tr", "bettergui.corner.bl", "bettergui.corner.br" };

	private final Screen parent;
	private final Text subtitle = Text.translatable("bettergui.subtitle");

	private int settingsX;
	private int settingsWidth;
	private int colorRowY;
	private int dividerY;

	private int previewX;
	private int previewY;
	private int previewWidth;
	private int previewHeight;

	private ConfigSlider blurSlider;
	private FlatButton translucentToggle;
	private TextFieldWidget colorField;
	private SwatchButton colorSwatch;
	private FlatButton resetColorButton;
	private FlatButton rainbowToggle;
	private FlatButton gradientToggle;
	private final TextFieldWidget[] cornerFields = new TextFieldWidget[4];
	private final SwatchButton[] cornerSwatches = new SwatchButton[4];
	private final int[] cornerRowY = new int[4];

	public BetterGuiConfigScreen(Screen parent) {
		super(Text.translatable("bettergui.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		BetterGuiConfig config = BetterGuiConfig.get();

		int gap = 20;
		int available = Math.max(300, this.width - 40 - gap);
		int columnWidth = Math.max(150, Math.min(240, available / 2));

		this.settingsX = this.width / 2 - gap / 2 - columnWidth;
		this.settingsWidth = columnWidth;
		this.previewX = this.width / 2 + gap / 2;
		this.previewWidth = Math.min(columnWidth, Math.max(120, this.width - this.previewX - 10));
		this.previewHeight = 150;

		int rowStep = 26;
		int rowHeight = 20;
		int rows = 10;
		int blockTop = Math.max(52, (this.height - rows * rowStep) / 2);
		this.previewY = Math.max(52, (this.height - this.previewHeight) / 2);


		int y = blockTop;

		this.translucentToggle = new FlatButton(this.textRenderer, this.settingsX, y, this.settingsWidth, rowHeight,
				translucentLabel(config), button -> {
					config.translucent = !config.translucent;
					button.setMessage(translucentLabel(config));
					this.blurSlider.active = config.translucent;
					GuiTextureProcessor.invalidate();
				}, BUTTON_FILL, BUTTON_FILL_HOVER, BUTTON_BORDER);
		this.addDrawableChild(this.translucentToggle);
		y += rowStep;

		this.blurSlider = new ConfigSlider(this.textRenderer, this.settingsX, y, this.settingsWidth, rowHeight,
				() -> config.blurRadius, value -> config.blurRadius = (float) value);
		this.blurSlider.active = config.translucent;
		this.addDrawableChild(this.blurSlider);
		y += rowStep;

		this.addDrawableChild(new FlatButton(this.textRenderer, this.settingsX, y, this.settingsWidth, rowHeight,
				slotLabel(config), button -> {
					boolean translucent = config.slotAlpha < SLOT_ALPHA_OPAQUE;
					config.slotAlpha = translucent ? SLOT_ALPHA_OPAQUE : SLOT_ALPHA_TRANSLUCENT;
					config.slotBorderAlpha = translucent ? SLOT_ALPHA_OPAQUE : SLOT_BORDER_ALPHA_TRANSLUCENT;
					config.highlightAlpha = translucent ? SLOT_ALPHA_OPAQUE : SLOT_HIGHLIGHT_ALPHA_TRANSLUCENT;
					GuiTextureProcessor.invalidate();
					button.setMessage(slotLabel(config));
				}, BUTTON_FILL, BUTTON_FILL_HOVER, BUTTON_BORDER));
		y += rowStep;

		this.dividerY = y + 2;
		y += 12;
		this.colorRowY = y;
		int fieldWidth = Math.max(60, this.settingsWidth - 148);
		this.colorField = new TextFieldWidget(this.textRenderer, this.settingsX + 58, y, fieldWidth, 20,
				Text.literal("HEX"));
		this.colorField.setMaxLength(9);
		this.colorField.setText(ColorPickerScreen.toHex(config.getPanelColor()));
		this.colorField.setChangedListener(text -> {
			int parsed = ColorPickerScreen.parseHex(text);
			if (parsed != -1) {
				config.setPanelColor(parsed);
			}
		});
		this.addDrawableChild(this.colorField);

		this.colorSwatch = new SwatchButton(this.settingsX + 64 + fieldWidth, y, this::openColorPicker,
				() -> BetterGuiConfig.get().getPanelColor());
		this.addDrawableChild(this.colorSwatch);

		this.resetColorButton = new FlatButton(this.textRenderer, this.settingsX + 92 + fieldWidth, y, 44, 20,
				Text.translatable("bettergui.reset"), button -> {
					config.resetPanelColor();
					this.colorField.setText(ColorPickerScreen.toHex(config.getPanelColor()));
				}, BUTTON_FILL, BUTTON_FILL_HOVER, BUTTON_BORDER);
		this.addDrawableChild(this.resetColorButton);
		y += rowStep;

		this.rainbowToggle = new FlatButton(this.textRenderer, this.settingsX, y, this.settingsWidth, rowHeight,
				rainbowLabel(config), button -> {
					config.rainbow = !config.rainbow;
					if (config.rainbow) {
						config.gradient = false;
					}
					button.setMessage(rainbowLabel(config));
					this.updateColorControls(config);
				}, BUTTON_FILL, BUTTON_FILL_HOVER, BUTTON_BORDER);
		this.addDrawableChild(this.rainbowToggle);
		y += rowStep;

		this.gradientToggle = new FlatButton(this.textRenderer, this.settingsX, y, this.settingsWidth, rowHeight,
				gradientLabel(config), button -> {
					config.gradient = !config.gradient;
					if (config.gradient) {
						config.rainbow = false;
					}
					button.setMessage(gradientLabel(config));
					this.updateColorControls(config);
				}, BUTTON_FILL, BUTTON_FILL_HOVER, BUTTON_BORDER);
		this.addDrawableChild(this.gradientToggle);
		y += rowStep;

		for (int i = 0; i < 4; i++) {
			final int index = i;
			this.cornerRowY[i] = y;
			TextFieldWidget cornerField = new TextFieldWidget(this.textRenderer, this.settingsX + 40, y, 76, 20,
					Text.translatable(CORNER_NAMES[i]));
			cornerField.setMaxLength(9);
			cornerField.setText(ColorPickerScreen.toHex(config.getCornerColor(index)));
			cornerField.setChangedListener(text -> {
				int parsed = ColorPickerScreen.parseHex(text);
				if (parsed != -1) {
					config.setCornerColor(index, parsed);
				}
			});
			this.cornerFields[i] = cornerField;
			this.addDrawableChild(cornerField);
			this.cornerSwatches[i] = new SwatchButton(this.settingsX + 120, y, () -> this.openCornerPicker(index),
					() -> BetterGuiConfig.get().getCornerColor(index));
			this.addDrawableChild(this.cornerSwatches[i]);
			y += rowStep;
		}

		this.updateColorControls(config);
		this.addDrawableChild(new FlatButton(this.textRenderer, this.width / 2 - 60, this.height - 40, 120, 20,
				Text.translatable("bettergui.done"), button -> this.close(),
				DONE_FILL, DONE_FILL_HOVER, BUTTON_BORDER));
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		BetterGuiRenderer.drawDim(context, this.width, this.height);

		context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 18, 0xFFFFFF);
		context.drawCenteredTextWithShadow(this.textRenderer, this.subtitle, this.width / 2, 34, SUBTITLE_COLOR);

		BetterGuiRenderer.drawPanel(context, this.previewX, this.previewY, this.previewWidth, this.previewHeight, delta);
		this.drawSlotPreview(context);

		Text header = Text.translatable("bettergui.section.color");
		int headerWidth = this.textRenderer.getWidth(header);
		int headerCenter = this.settingsX + this.settingsWidth / 2;
		context.fill(this.settingsX, this.dividerY + 4, headerCenter - headerWidth / 2 - 6, this.dividerY + 5, 0x66FFFFFF);
		context.fill(headerCenter + headerWidth / 2 + 6, this.dividerY + 4, this.settingsX + this.settingsWidth, this.dividerY + 5, 0x66FFFFFF);
		context.drawTextWithShadow(this.textRenderer, header, headerCenter - headerWidth / 2, this.dividerY, 0xFFE6E6E6);
		context.drawTextWithShadow(this.textRenderer, Text.translatable("bettergui.panel_color"), this.settingsX,
				this.colorRowY + 6, 0xFFFFFF);

		for (int i = 0; i < 4; i++) {
			context.drawTextWithShadow(this.textRenderer, Text.translatable(CORNER_NAMES[i]), this.settingsX,
					this.cornerRowY[i] + 6, 0xFFFFFF);
		}
		super.render(context, mouseX, mouseY, delta);
	}

	
	private void drawSlotPreview(DrawContext context) {
		BetterGuiConfig config = BetterGuiConfig.get();
		int size = 18;
		int gap = 4;
		int count = 4;
		int totalWidth = count * size + (count - 1) * gap;
		int startX = this.previewX + (this.previewWidth - totalWidth) / 2;
		int startY = this.previewY + 20;

		for (int i = 0; i < count; i++) {
			int x = startX + i * (size + gap);
			context.fill(x, startY, x + size, startY + size, argb(config.effectiveSlotBorderAlpha(), 0x373737));
			context.fill(x + 1, startY + 1, x + size - 1, startY + size - 1, argb(config.effectiveSlotAlpha(), 0x8B8B8B));
			context.fill(x + 1, startY + size - 1, x + size - 1, startY + size,
					argb(config.effectiveHighlightAlpha(), 0xFFFFFF));
			context.fill(x + size - 1, startY + 1, x + size, startY + size,
					argb(config.effectiveHighlightAlpha(), 0xFFFFFF));
		}
	}

	@Override
	public void close() {
		BetterGuiConfig.get().save();
		if (this.client != null) {
			this.client.setScreen(this.parent);
		}
	}

	@Override
	public void removed() {
		BetterGuiConfig.get().save();
	}

	private void openColorPicker() {
		if (this.client == null) {
			return;
		}
		this.client.setScreen(new ColorPickerScreen(this, BetterGuiConfig.get().getPanelColor(), argb -> {
			BetterGuiConfig.get().setPanelColor(argb);
			if (this.colorField != null) {
				this.colorField.setText(ColorPickerScreen.toHex(argb));
			}
		}));
	}

	private static Text translucentLabel(BetterGuiConfig config) {
		return Text.translatable("bettergui.translucent", onOff(config.translucent));
	}

	private void updateColorControls(BetterGuiConfig config) {
		boolean plain = config.plainColorEnabled();
		if (this.colorField != null) {
			this.colorField.setEditable(plain);
			this.colorField.active = plain;
		}
		if (this.colorSwatch != null) {
			this.colorSwatch.active = plain;
		}
		if (this.resetColorButton != null) {
			this.resetColorButton.active = plain;
		}
		if (this.rainbowToggle != null) {
			this.rainbowToggle.active = !config.gradient;
		}
		if (this.gradientToggle != null) {
			this.gradientToggle.active = !config.rainbow;
		}
		for (int i = 0; i < 4; i++) {
			if (this.cornerFields[i] != null) {
				this.cornerFields[i].setEditable(config.gradient);
				this.cornerFields[i].active = config.gradient;
			}
			if (this.cornerSwatches[i] != null) {
				this.cornerSwatches[i].active = config.gradient;
			}
		}
	}

	private static Text rainbowLabel(BetterGuiConfig config) {
		return Text.translatable("bettergui.rainbow", onOff(config.rainbow));
	}
	private void openCornerPicker(int index) {
		if (this.client == null || !BetterGuiConfig.get().gradient) {
			return;
		}
		this.client.setScreen(new ColorPickerScreen(this, BetterGuiConfig.get().getCornerColor(index), argb -> {
			BetterGuiConfig.get().setCornerColor(index, argb);
			if (this.cornerFields[index] != null) {
				this.cornerFields[index].setText(ColorPickerScreen.toHex(argb));
			}
		}));
	}

	private static Text gradientLabel(BetterGuiConfig config) {
		return Text.translatable("bettergui.gradient", onOff(config.gradient));
	}
	private static Text onOff(boolean value) {
		return Text.translatable(value ? "bettergui.on" : "bettergui.off");
	}

	private static Text slotLabel(BetterGuiConfig config) {
		return Text.translatable("bettergui.slots", onOff(config.slotAlpha < SLOT_ALPHA_OPAQUE));
	}


	private static int argb(int alpha, int rgb) {
		int clamped = Math.max(0, Math.min(255, alpha));
		return (clamped << 24) | (rgb & 0x00FFFFFF);
	}

	
	private class SwatchButton extends ButtonWidget {
		private final IntSupplier colorSource;

		SwatchButton(int x, int y, Runnable action, IntSupplier colorSource) {
			super(x, y, 20, 20, Text.empty(), button -> action.run(), DEFAULT_NARRATION_SUPPLIER);
			this.colorSource = colorSource;
		}

		@Override
		public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
			int x = this.getX();
			int y = this.getY();
			boolean hovered = this.active
					&& mouseX >= x && mouseX < x + 20 && mouseY >= y && mouseY < y + 20;
			context.fill(x - 1, y - 1, x + 21, y + 21, 0xFFFFFFFF);
			context.fill(x, y, x + 20, y + 20, this.colorSource.getAsInt());
			if (hovered) {
				context.fill(x, y, x + 20, y + 1, 0x80FFFFFF);
				context.fill(x, y + 19, x + 20, y + 20, 0x80FFFFFF);
			}
		}
	}
	
	private static class FlatButton extends ButtonWidget {
		private final TextRenderer textRenderer;
		private final int fillColor;
		private final int hoverColor;
		private final int borderColor;

		FlatButton(TextRenderer textRenderer, int x, int y, int width, int height, Text message, PressAction onPress,
				int fillColor, int hoverColor, int borderColor) {
			super(x, y, width, height, message, onPress, DEFAULT_NARRATION_SUPPLIER);
			this.textRenderer = textRenderer;
			this.fillColor = fillColor;
			this.hoverColor = hoverColor;
			this.borderColor = borderColor;
		}

		@Override
		public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
			int x = this.getX();
			int y = this.getY();
			int width = this.getWidth();
			int height = this.getHeight();
			boolean hovered = this.active
					&& mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;

			int fill = this.active ? (hovered ? this.hoverColor : this.fillColor) : 0xC0181818;
			context.fill(x, y, x + width, y + height, fill);
			context.fill(x, y, x + width, y + 1, this.borderColor);
			context.fill(x, y + height - 1, x + width, y + height, this.borderColor);
			context.fill(x, y, x + 1, y + height, this.borderColor);
			context.fill(x + width - 1, y, x + width, y + height, this.borderColor);
			context.drawCenteredTextWithShadow(this.textRenderer, this.getMessage(),
					x + width / 2, y + (height - 8) / 2, this.active ? 0xFFFFFF : 0x909090);
		}
	}

	
	private static class ConfigSlider extends SliderWidget {
		private static final int TRACK_FILL = 0xE01A1A1A;
		private static final int TRACK_BORDER = 0x50FFFFFF;
		private static final int HANDLE_FILL = 0xFFB0B0B0;
		private static final int HANDLE_FILL_HOVER = 0xFFD0D0D0;

		private final TextRenderer textRenderer;
		private final DoubleSupplier read;
		private final DoubleConsumer apply;

		ConfigSlider(TextRenderer textRenderer, int x, int y, int width, int height,
				DoubleSupplier read, DoubleConsumer apply) {
			super(x, y, width, height, Text.translatable("bettergui.blur", ""), normalize(read.getAsDouble()));
			this.textRenderer = textRenderer;
			this.read = read;
			this.apply = apply;
			this.updateMessage();
		}

		private static double normalize(double value) {
			return Math.max(0.0, Math.min(1.0, value / MAX_BLUR));
		}

		private double currentValue() {
			return Math.max(0.0, Math.min(MAX_BLUR, Math.round(this.value * MAX_BLUR)));
		}

		@Override
		protected void updateMessage() {
			if (this.read == null || this.apply == null) {
				return;
			}
			double value = this.currentValue();
			Text message = value <= 0.0
					? Text.translatable("bettergui.blur_off")
					: Text.translatable("bettergui.blur", (int) Math.round(value / MAX_BLUR * 100.0) + "%");
			this.setMessage(message);
		}

		@Override
		protected void applyValue() {
			this.apply.accept(this.currentValue());
		}

		@Override
		public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
			int x = this.getX();
			int y = this.getY();
			int width = this.getWidth();
			int height = this.getHeight();
			boolean hovered = this.active
					&& mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;

			context.fill(x, y, x + width, y + height, TRACK_FILL);
			context.fill(x, y, x + width, y + 1, TRACK_BORDER);
			context.fill(x, y + height - 1, x + width, y + height, TRACK_BORDER);
			context.fill(x, y, x + 1, y + height, TRACK_BORDER);
			context.fill(x + width - 1, y, x + width, y + height, TRACK_BORDER);

			int handleX = x + 1 + (int) Math.round(this.value * (width - 10));
			context.fill(handleX, y + 1, handleX + 8, y + height - 1,
					this.active ? (hovered ? HANDLE_FILL_HOVER : HANDLE_FILL) : 0xFF606060);
			context.drawCenteredTextWithShadow(this.textRenderer, this.getMessage(),
					x + width / 2, y + (height - 8) / 2, this.active ? 0xFFFFFF : 0x909090);
		}
	}
}
