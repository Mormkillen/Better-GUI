package com.bettergui.mixin;

import com.bettergui.BetterGuiTargets;
import com.bettergui.GuiTextureProcessor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(DrawContext.class)
public abstract class DrawContextMixin {
	@ModifyVariable(method = "drawTexture(Lnet/minecraft/util/Identifier;IIIIII)V",
			at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private Identifier bettergui$useBlurredTexture(Identifier texture) {
		if (texture == null || !BetterGuiTargets.shouldProcess(texture)) {
			return texture;
		}
		return GuiTextureProcessor.get(texture);
	}
}
