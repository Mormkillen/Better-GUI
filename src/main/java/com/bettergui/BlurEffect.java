package com.bettergui;

import com.bettergui.mixin.PostEffectProcessorAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.PostEffectPass;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.util.Identifier;

import java.util.List;

public final class BlurEffect {
	private static final Identifier EFFECT_ID = new Identifier(BetterGuiClient.MOD_ID, "shaders/post/bettergui_blur.json");
	private static final String TARGET_NAME = "bettergui_blur";
	private static final Identifier TEXTURE_ID = new Identifier(BetterGuiClient.MOD_ID, "blur_target");

	private static final FramebufferTexture TEXTURE = new FramebufferTexture();

	private static PostEffectProcessor processor;
	private static boolean broken;
	private static boolean registered;
	private static int configuredWidth = -1;
	private static int configuredHeight = -1;
	private static float appliedRadius = Float.NaN;

	private BlurEffect() {
	}

	public static Identifier blur(int framebufferWidth, int framebufferHeight, float delta) {
		if (broken) {
			return null;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		try {
			if (processor == null) {
				processor = new PostEffectProcessor(client.getTextureManager(), client.getResourceManager(),
						client.getFramebuffer(), EFFECT_ID);
				configuredWidth = -1;
				configuredHeight = -1;
				appliedRadius = Float.NaN;
			}

			if (framebufferWidth != configuredWidth || framebufferHeight != configuredHeight) {
				processor.setupDimensions(framebufferWidth, framebufferHeight);
				configuredWidth = framebufferWidth;
				configuredHeight = framebufferHeight;
			}

			float radius = BetterGuiConfig.get().blurRadius;
			if (radius != appliedRadius) {
				applyRadius(radius);
				appliedRadius = radius;
			}

			processor.render(delta);
			client.getFramebuffer().beginWrite(true);

			Framebuffer target = processor.getSecondaryTarget(TARGET_NAME);
			if (target == null) {
				return null;
			}

			TEXTURE.setFramebuffer(target);
			if (!registered) {
				client.getTextureManager().registerTexture(TEXTURE_ID, TEXTURE);
				registered = true;
			}
			return TEXTURE_ID;
		} catch (Throwable t) {
			BetterGuiClient.LOGGER.warn("[BetterGUI] blur post-effect unavailable, disabling blur", t);
			broken = true;
			processor = null;
			return null;
		}
	}

	private static void applyRadius(float radius) {
		List<PostEffectPass> passes = ((PostEffectProcessorAccessor) (Object) processor).bettergui$getPasses();
		for (PostEffectPass pass : passes) {
			GlUniform uniform = pass.getProgram().getUniformByName("Radius");
			if (uniform != null) {
				uniform.set(radius);
			}
		}
	}
}
