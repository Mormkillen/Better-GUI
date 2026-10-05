package com.bettergui.mixin;

import com.bettergui.BetterGuiConfig;
import com.bettergui.BetterGuiRenderer;
import com.bettergui.BetterGuiTargets;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {
	@Shadow
	protected int x;
	@Shadow
	protected int y;
	@Shadow
	protected int backgroundWidth;
	@Shadow
	protected int backgroundHeight;

	@Inject(method = "render", at = @At("HEAD"))
	private void bettergui$drawPanel(DrawContext context, int mouseX, int mouseY, float delta,
			CallbackInfo callbackInfo) {
		Screen self = (Screen) (Object) this;
		if (!BetterGuiTargets.isTarget(self)) {
			return;
		}
		BetterGuiRenderer.drawPanel(context, this.x, this.y, this.backgroundWidth, this.backgroundHeight, delta);
	}

	@Redirect(method = "drawForeground", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;IIIZ)I"))
	private int bettergui$recolorLabel(DrawContext context, TextRenderer textRenderer, Text text,
			int textX, int textY, int color, boolean shadow) {
		BetterGuiConfig config = BetterGuiConfig.get();
		int finalColor = color;
		if (config.recolorLabels && BetterGuiTargets.isTarget((Screen) (Object) this)) {
			finalColor = config.labelColor;
		}
		return context.drawText(textRenderer, text, textX, textY, finalColor, shadow);
	}
}
