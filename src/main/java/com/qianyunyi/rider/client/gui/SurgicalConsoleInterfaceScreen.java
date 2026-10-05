package com.qianyunyi.rider.client.gui;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.GuiGraphics;

import java.util.HashMap;

import com.qianyunyi.rider.world.inventory.SurgicalConsoleInterfaceMenu;
import com.qianyunyi.rider.network.SurgicalConsoleInterfaceButtonMessage;
import com.qianyunyi.rider.KamenRiderInterwovenWorldsMod;

import com.mojang.blaze3d.systems.RenderSystem;

public class SurgicalConsoleInterfaceScreen extends AbstractContainerScreen<SurgicalConsoleInterfaceMenu> {
	private final static HashMap<String, Object> guistate = SurgicalConsoleInterfaceMenu.guistate;
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	ImageButton imagebutton_begin_the_surgery;

	public SurgicalConsoleInterfaceScreen(SurgicalConsoleInterfaceMenu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.world = container.world;
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
		this.imageWidth = 176;
		this.imageHeight = 166;
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		this.renderBackground(guiGraphics);
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		this.renderTooltip(guiGraphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();

		guiGraphics.blit(ResourceLocation.parse("kamen_rider_interwoven_worlds:textures/screens/shocker_research_base_gui.png"), this.leftPos + 0, this.topPos + 0, 0, 0, 176, 166, 176, 166);

		RenderSystem.disableBlend();
	}

	@Override
	public boolean keyPressed(int key, int b, int c) {
		if (key == 256) {
			this.minecraft.player.closeContainer();
			return true;
		}
		return super.keyPressed(key, b, c);
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
	}

	@Override
	public void init() {
		super.init();
		imagebutton_begin_the_surgery = new ImageButton(this.leftPos + 98, this.topPos + 66, 57, 13, 0, 0, 13, ResourceLocation.parse("kamen_rider_interwoven_worlds:textures/screens/atlas/imagebutton_begin_the_surgery.png"), 57, 26, e -> {
			if (true) {
				KamenRiderInterwovenWorldsMod.PACKET_HANDLER.sendToServer(new SurgicalConsoleInterfaceButtonMessage(0, x, y, z));
				SurgicalConsoleInterfaceButtonMessage.handleButtonAction(entity, 0, x, y, z);
			}
		});
		guistate.put("button:imagebutton_begin_the_surgery", imagebutton_begin_the_surgery);
		this.addRenderableWidget(imagebutton_begin_the_surgery);
	}
}
