package com.bettergui.mixin;

import com.bettergui.BetterGuiConfig;
import com.bettergui.BetterGuiRenderer;
import com.bettergui.BetterGuiTargets;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RecipeBookWidget.class)
public abstract class RecipeBookWidgetMixin {
	@Shadow
	private int leftOffset;
	@Shadow
	private int parentWidth;
	@Shadow
	private int parentHeight;

	@Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;IIF)V", require = 0, at = @At("HEAD"))
	private void bettergui$drawPanel(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo callbackInfo) {
		RecipeBookWidget self = (RecipeBookWidget) (Object) this;
		if (!self.isOpen() || !BetterGuiTargets.isTargetScreenOpen()) {
			return;
		}
		int bookX = (this.parentWidth - 147) / 2 - this.leftOffset;
		int bookY = (this.parentHeight - 166) / 2;
		BetterGuiRenderer.drawPanel(context, bookX, bookY, 147, 166, delta);
	}
}