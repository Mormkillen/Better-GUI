package com.bettergui;

import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.resource.ResourceManager;

public class FramebufferTexture extends AbstractTexture {
	private Framebuffer framebuffer;

	public void setFramebuffer(Framebuffer framebuffer) {
		this.framebuffer = framebuffer;
	}

	@Override
	public int getGlId() {
		Framebuffer target = this.framebuffer;
		if (target != null) {
			int id = target.getColorAttachment();
			if (id > 0) {
				return id;
			}
		}
		return super.getGlId();
	}

	@Override
	public void load(ResourceManager manager) {
	}

	@Override
	public void close() {
	}
}
