package com.bettergui.mixin;

import com.bettergui.BetterGuiRenderer;
import com.bettergui.BetterGuiTargets;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin {
	@Inject(method = "renderBackground(Lnet/minecraft/client/gui/DrawContext;)V", at = @At("HEAD"), cancellable = true)
	private void bettergui$renderPanelBackground(DrawContext context, CallbackInfo callbackInfo) {
		Screen self = (Screen) (Object) this;
		if (!BetterGuiTargets.isTarget(self)) {
			return;
		}
		BetterGuiRenderer.drawDim(context, self.width, self.height);
		callbackInfo.cancel();
	}
}
