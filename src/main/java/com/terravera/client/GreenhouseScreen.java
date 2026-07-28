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

/**
 * Control panel for a placed greenhouse.
 * <p>
 * The previous layout drew its four status lines at y=122..158, which is directly on top of the player inventory
 * slots that start at y=112 - the readouts and the item grid were rendered over one another, and the panel
 * backgrounds did not line up with either. This version gives every element a reserved band: readouts in a fixed
 * five-row grid, controls below them, and the inventory below that. The four status lines are gone entirely,
 * because a toggle button that says "Vent: open" tells the player the same thing in half the space and without a
 * second place to keep in sync.
 */
public class GreenhouseScreen extends AbstractContainerScreen<GreenhouseMenu>
{
    // Layout bands. Kept as named constants so a change in one place cannot silently overlap another band.
    private static final int PANEL_TOP = 18;
    private static final int PANEL_BOTTOM = 76;
    private static final int ROW_HEIGHT = 11;
    private static final int FIRST_ROW_Y = 21;
    private static final int RIGHT_COLUMN_X = 92;
    private static final int LEFT_COLUMN_X = 8;

    private static final int BUTTON_HEIGHT = 16;
    private static final int BUTTON_WIDTH = 76;
    private static final int BUTTON_ROW_1 = 78;
    private static final int BUTTON_ROW_2 = 96;
    private static final int BUTTON_ROW_3 = 114;

    private static final int INVENTORY_TOP = 141;

    private static final int TEXT_COLOUR = 0xff1f351f;
    private static final int TEXT_MUTED = 0xff4a5f4a;

    private Button ventButton;
    private Button irrigationButton;
    private Button heatButton;
    private Button coolButton;
    private Button harvestButton;

    public GreenhouseScreen(GreenhouseMenu menu, Inventory inventory, Component title)
    {
        super(menu, inventory, title);
        imageWidth = 176;
        // Panel + three button rows + label + three inventory rows + hotbar, with the gaps written out above.
        imageHeight = INVENTORY_TOP + 54 + 4 + 18 + 6;
        inventoryLabelY = INVENTORY_TOP - 11;
        titleLabelY = 6;
    }

    @Override
    protected void init()
    {
        super.init();
        ventButton = addRenderableWidget(Button.builder(Component.empty(), b -> click(GreenhouseMenu.TOGGLE_VENT))
            .bounds(leftPos + LEFT_COLUMN_X, topPos + BUTTON_ROW_1, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        irrigationButton = addRenderableWidget(Button.builder(Component.empty(), b -> click(GreenhouseMenu.TOGGLE_IRRIGATION))
            .bounds(leftPos + RIGHT_COLUMN_X, topPos + BUTTON_ROW_1, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        heatButton = addRenderableWidget(Button.builder(Component.empty(), b -> click(GreenhouseMenu.TOGGLE_HEATING))
            .bounds(leftPos + LEFT_COLUMN_X, topPos + BUTTON_ROW_2, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        coolButton = addRenderableWidget(Button.builder(Component.empty(), b -> click(GreenhouseMenu.TOGGLE_COOLING))
            .bounds(leftPos + RIGHT_COLUMN_X, topPos + BUTTON_ROW_2, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        harvestButton = addRenderableWidget(Button.builder(Component.empty(), b -> click(GreenhouseMenu.HARVEST))
            .bounds(leftPos + LEFT_COLUMN_X, topPos + BUTTON_ROW_3, imageWidth - LEFT_COLUMN_X * 2, BUTTON_HEIGHT).build());
        refreshButtons();
    }

    @Override
    protected void containerTick()
    {
        super.containerTick();
        refreshButtons();
    }

    /** Keep the button labels showing live state, so the panel needs no separate status block to overlap. */
    private void refreshButtons()
    {
        if (ventButton == null) return;
        ventButton.setMessage(Component.translatable(
            menu.ventOpen() ? "terravera.greenhouse.gui.vent_open" : "terravera.greenhouse.gui.vent_closed"));
        irrigationButton.setMessage(Component.translatable(
            menu.irrigationActive() ? "terravera.greenhouse.gui.irrigation_on" : "terravera.greenhouse.gui.irrigation_off"));
        heatButton.setMessage(Component.translatable(
            menu.heatingOn() ? "terravera.greenhouse.gui.heat_on" : "terravera.greenhouse.gui.heat_off"));
        coolButton.setMessage(Component.translatable(
            menu.coolingOn() ? "terravera.greenhouse.gui.cool_on" : "terravera.greenhouse.gui.cool_off"));

        final int ready = menu.readyTrays();
        harvestButton.setMessage(ready > 0
            ? Component.translatable("terravera.greenhouse.gui.harvest_ready", ready)
            : Component.translatable("terravera.greenhouse.gui.harvest_none"));
        harvestButton.active = ready > 0;

        // Heating and cooling only exist on an automated greenhouse; grey them out rather than letting the player
        // press a button that can only ever refuse.
        final boolean automated = menu.tier() >= 3;
        heatButton.active = automated;
        coolButton.active = automated;
    }

    private void click(int id)
    {
        if (minecraft != null && minecraft.gameMode != null)
        {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY)
    {
        // Frame
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xffc8d8bd);
        // Title bar
        graphics.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + 16, 0xff3d6b3d);
        // Readout panel, sized to the rows it actually contains
        graphics.fill(leftPos + 6, topPos + PANEL_TOP, leftPos + imageWidth - 6, topPos + PANEL_BOTTOM, 0xffe7f0de);
        // Inventory backing, aligned to the slot grid rather than guessed at
        graphics.fill(leftPos + 6, topPos + INVENTORY_TOP - 4,
            leftPos + imageWidth - 6, topPos + imageHeight - 4, 0xffb8a178);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY)
    {
        graphics.drawString(font, title, LEFT_COLUMN_X, titleLabelY, 0xffffffff, false);

        row(graphics, 0, Component.translatable("terravera.greenhouse.gui.tier", menu.tierName()),
            Component.translatable("terravera.greenhouse.gui.plants", menu.plantedTrays(), menu.capacity()));
        row(graphics, 1,
            Component.translatable("terravera.greenhouse.gui.temperature", String.format("%.1f", menu.temperatureC())),
            Component.translatable("terravera.greenhouse.gui.humidity", menu.humidityPercent()));
        row(graphics, 2, Component.translatable("terravera.greenhouse.gui.soil", menu.soilMoisturePercent()),
            Component.translatable("terravera.greenhouse.gui.growth", menu.growthPercent()));
        row(graphics, 3, Component.translatable("terravera.greenhouse.gui.sun", menu.sunlightPercent()),
            Component.translatable("terravera.greenhouse.gui.glass", menu.glassPercent()));

        // Last row reports the crop itself: how far the slowest tray has to go, and how many are waiting.
        final Component progress = Component.translatable("terravera.greenhouse.gui.crop_progress", menu.trayProgressPercent());
        final Component ready = menu.readyTrays() > 0
            ? Component.translatable("terravera.greenhouse.gui.ready", menu.readyTrays())
            : Component.translatable("terravera.greenhouse.gui.none_ready");
        graphics.drawString(font, progress, LEFT_COLUMN_X, FIRST_ROW_Y + 4 * ROW_HEIGHT, TEXT_COLOUR, false);
        graphics.drawString(font, ready, RIGHT_COLUMN_X, FIRST_ROW_Y + 4 * ROW_HEIGHT,
            menu.readyTrays() > 0 ? TEXT_COLOUR : TEXT_MUTED, false);

        graphics.drawString(font, playerInventoryTitle, LEFT_COLUMN_X, inventoryLabelY, TEXT_COLOUR, false);
    }

    /** Draw one two-column readout row, clipped to its column so a long translation cannot bleed across. */
    private void row(GuiGraphics graphics, int index, Component left, Component right)
    {
        final int y = FIRST_ROW_Y + index * ROW_HEIGHT;
        final int columnWidth = RIGHT_COLUMN_X - LEFT_COLUMN_X - 2;
        graphics.drawString(font, font.plainSubstrByWidth(left.getString(), columnWidth),
            LEFT_COLUMN_X, y, TEXT_COLOUR, false);
        graphics.drawString(font, font.plainSubstrByWidth(right.getString(), imageWidth - RIGHT_COLUMN_X - 8),
            RIGHT_COLUMN_X, y, TEXT_COLOUR, false);
    }
}
