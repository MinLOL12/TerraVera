/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import net.dries007.tfc.client.TFCSounds;
import net.dries007.tfc.client.screen.TFCContainerScreen;
import net.dries007.tfc.client.screen.button.KnappingButton;

import com.terravera.common.container.ShapingContainer;
import com.terravera.config.TerraVeraConfig;

/**
 * The TerraVera shaping screen. Uses TFC's native knapping GUI components
 * (buttons, textures, sounds) but displays TerraVera's function-based feedback.
 * <p>
 * This gives players the familiar TFC knapping experience while using TerraVera's
 * more flexible knapping system that doesn't require exact pattern matching.
 */
public class ShapingScreen extends TFCContainerScreen<ShapingContainer>
{
    public static final ResourceLocation BACKGROUND = net.dries007.tfc.client.screen.KnappingScreen.BACKGROUND;

    private final ResourceLocation buttonTexture;

    public ShapingScreen(ShapingContainer container, Inventory inventory, Component name)
    {
        super(container, inventory, name, BACKGROUND);
        imageHeight = 186;
        inventoryLabelY += 22;
        titleLabelY -= 2;

        // Use TFC's per-rock knapping tile textures (same as vanilla TFC)
        final ItemStack stack = container.getOriginalStack();
        final ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        this.buttonTexture = ResourceLocation.fromNamespaceAndPath(id.getNamespace(),
            "textures/gui/knapping/" + id.getPath() + ".png");
    }

    @Override
    protected void init()
    {
        super.init();
        // Use TFC's grid size (5x5)
        for (int x = 0; x < 5; x++)
        {
            for (int y = 0; y < 5; y++)
            {
                final int bx = (width - getXSize()) / 2 + 12 + 16 * x;
                final int by = (height - getYSize()) / 2 + 12 + 16 * y;
                // Reuse TerraFirmaCraft's stone knapping click sound
                addRenderableWidget(new KnappingButton(x + 5 * y, bx, by, 16, 16,
                    buttonTexture, TFCSounds.KNAP_STONE.holder()));
            }
        }
        menu.setRequiresReset(true);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY)
    {
        if (menu.requiresReset())
        {
            for (Renderable widget : renderables)
            {
                if (widget instanceof KnappingButton button)
                {
                    button.visible = menu.getPattern().get(button.id);
                }
            }
            menu.setRequiresReset(false);
        }

        super.renderBg(graphics, partialTicks, mouseX, mouseY);

        for (Renderable widget : renderables)
        {
            if (widget instanceof KnappingButton button && button.visible)
            {
                graphics.blit(buttonTexture, button.getX(), button.getY(), 0, 0, 16, 16, 16, 16);
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        super.render(graphics, mouseX, mouseY, partialTick);

        // Show TerraVera's custom feedback if enabled
        final String feedback = menu.feedback();
        if (feedback != null && TerraVeraConfig.SERVER.showKnappingFeedback.get())
        {
            final Component text = Component.translatable("terravera.shaping.feedback." + feedback);
            graphics.drawCenteredString(font, text, width / 2, topPos + imageHeight - 96, 0xFF7F6A55);
        }
    }

    @Override
    public boolean mouseDragged(double x, double y, int clickType, double dragX, double dragY)
    {
        // Dragging across tiles knaps them, as in TFC
        if (clickType == 0) mouseClicked(x, y, clickType);
        return super.mouseDragged(x, y, clickType, dragX, dragY);
    }
}
