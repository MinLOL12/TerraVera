/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 */

package com.terravera.client;

import com.terravera.common.container.GreenhouseMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Simple in-world control panel for greenhouse climate and structure feedback. */
public class GreenhouseScreen extends AbstractContainerScreen<GreenhouseMenu>
{
    public GreenhouseScreen(GreenhouseMenu menu, Inventory inventory, Component title)
    {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 196;
        inventoryLabelY = 102;
    }

    @Override
    protected void init()
    {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("terravera.greenhouse.gui.vent"),
                button -> click(GreenhouseMenu.TOGGLE_VENT))
            .bounds(leftPos + 8, topPos + 72, 76, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("terravera.greenhouse.gui.irrigation"),
                button -> click(GreenhouseMenu.TOGGLE_IRRIGATION))
            .bounds(leftPos + 92, topPos + 72, 76, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("terravera.greenhouse.gui.heat"),
                button -> click(GreenhouseMenu.TOGGLE_HEATING))
            .bounds(leftPos + 8, topPos + 92, 76, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("terravera.greenhouse.gui.cool"),
                button -> click(GreenhouseMenu.TOGGLE_COOLING))
            .bounds(leftPos + 92, topPos + 92, 76, 18).build());
    }

    private void click(int id)
    {
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY)
    {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xffc8d8bd);
        graphics.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + 18, 0xff3d6b3d);
        graphics.fill(leftPos + 6, topPos + 22, leftPos + imageWidth - 6, topPos + 68, 0xffe7f0de);
        graphics.fill(leftPos + 6, topPos + 114, leftPos + imageWidth - 6, topPos + imageHeight - 6, 0xffb8a178);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY)
    {
        graphics.drawString(font, title, 8, 6, 0xffffffff, false);
        graphics.drawString(font, Component.translatable("terravera.greenhouse.gui.tier", menu.tierName()), 8, 24, 0xff1f351f, false);
        graphics.drawString(font, Component.translatable("terravera.greenhouse.gui.temperature", String.format("%.1f", menu.temperatureC())), 8, 36, 0xff1f351f, false);
        graphics.drawString(font, Component.translatable("terravera.greenhouse.gui.humidity", menu.humidityPercent()), 8, 48, 0xff1f351f, false);
        graphics.drawString(font, Component.translatable("terravera.greenhouse.gui.soil", menu.soilMoisturePercent()), 92, 36, 0xff1f351f, false);
        graphics.drawString(font, Component.translatable("terravera.greenhouse.gui.growth", menu.growthPercent()), 92, 48, 0xff1f351f, false);
        graphics.drawString(font, Component.translatable("terravera.greenhouse.gui.light", menu.sunlightPercent(), menu.glassPercent()), 8, 60, 0xff1f351f, false);
        graphics.drawString(font, Component.translatable("terravera.greenhouse.gui.plants", menu.plants(), menu.capacity()), 92, 60, 0xff1f351f, false);

        graphics.drawString(font, Component.translatable(menu.ventOpen() ? "terravera.greenhouse.gui.vent_open" : "terravera.greenhouse.gui.vent_closed"), 8, 122, 0xff1f351f, false);
        graphics.drawString(font, Component.translatable(menu.irrigationActive() ? "terravera.greenhouse.gui.irrigation_on" : "terravera.greenhouse.gui.irrigation_off"), 8, 134, 0xff1f351f, false);
        graphics.drawString(font, Component.translatable(menu.heatingOn() ? "terravera.greenhouse.gui.heat_on" : "terravera.greenhouse.gui.heat_off"), 8, 146, 0xff1f351f, false);
        graphics.drawString(font, Component.translatable(menu.coolingOn() ? "terravera.greenhouse.gui.cool_on" : "terravera.greenhouse.gui.cool_off"), 8, 158, 0xff1f351f, false);
        graphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0xff1f351f, false);
    }
}
