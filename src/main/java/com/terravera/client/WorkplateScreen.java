/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import net.dries007.tfc.common.component.heat.IHeatView;

import com.terravera.common.component.ToolMetalState;
import com.terravera.common.container.WorkplateContainer;
import com.terravera.common.smithing.SmithingOperation;

/**
 * A clean TFC-style GUI for the Workplate that lets the player repair metal tools through the screen rather than
 * memorising sneak-click shortcuts.
 * <p>
 * The entire background is drawn procedurally in the warm brown tones that TerraFirmaCraft uses for its own screens,
 * so no extra texture atlas is required. The layout is:
 * <pre>
 *   ┌─ Title ─────────────────────────────────────┐
 *   │  [Hammer]  ┌── Tool info panel ──────────┐  │
 *   │  [Tool  ]  │ name, durability, mass,     │  │
 *   │  [Flux  ]  │ heat bar, shape, operation  │  │
 *   │            └─────────────────────────────┘  │
 *   │  [Drawing] [Upsetting] [Flattening] ...     │
 *   │              [ ⚒ Strike ]                   │
 *   ├─ Player inventory ──────────────────────────┤
 *   └─ Hotbar ────────────────────────────────────┘
 * </pre>
 */
public class WorkplateScreen extends AbstractContainerScreen<WorkplateContainer>
{
    // ── Parchment palette (TFC-inspired warm browns) ──
    private static final int COL_BG_OUTER     = 0xFF3B2510;
    private static final int COL_BG_FILL      = 0xFFC6A97B;
    private static final int COL_BG_INNER     = 0xFFB89B6A;
    private static final int COL_PANEL        = 0xFFA08555;
    private static final int COL_PANEL_BORDER = 0xFF6B4F2E;
    private static final int COL_SLOT_BG      = 0xFF8B7355;
    private static final int COL_SLOT_BORDER  = 0xFF37261A;
    private static final int COL_TEXT_DARK     = 0xFF3B2510;
    private static final int COL_TEXT_LABEL    = 0xFF5C4023;
    private static final int COL_TEXT_SHADOW   = 0xFF2A190B;

    // ── Heat bar gradient stops ──
    private static final int COL_HEAT_COLD    = 0xFF222222;
    private static final int COL_HEAT_WARM    = 0xFF992200;
    private static final int COL_HEAT_HOT     = 0xFFDD4400;
    private static final int COL_HEAT_WORK    = 0xFFFF9900;
    private static final int COL_HEAT_WELD    = 0xFFFFFF55;

    private static final int COL_BAR_BG       = 0xFF1A1A1A;
    private static final int COL_DURABILITY_OK  = 0xFF44BB44;
    private static final int COL_DURABILITY_WARN = 0xFFBBBB22;
    private static final int COL_DURABILITY_LOW  = 0xFFBB3333;
    private static final int COL_MASS_FULL    = 0xFF8899AA;
    private static final int COL_MASS_LOW     = 0xFF554433;

    // ── Button styling ──
    private static final int COL_BTN_NORMAL   = 0xFF8B6F47;
    private static final int COL_BTN_HOVER    = 0xFFA8854F;
    private static final int COL_BTN_ACTIVE   = 0xFFD9A05B;
    private static final int COL_BTN_DISABLED = 0xFF554535;
    private static final int COL_BTN_BORDER   = 0xFF3B2510;
    private static final int COL_BTN_TEXT     = 0xFFFFFFFF;
    private static final int COL_BTN_DISABLED_TEXT = 0xFF9E8E7E;

    private final List<OperationButton> operationButtons = new ArrayList<>();
    private Button strikeButton;

    public WorkplateScreen(WorkplateContainer container, Inventory inventory, Component title)
    {
        super(container, inventory, title);
        imageWidth = 210;
        imageHeight = 240;
        titleLabelY = 7;
        inventoryLabelY = 145;
    }

    @Override
    protected void init()
    {
        super.init();
        operationButtons.clear();

        final SmithingOperation[] ops = SmithingOperation.values();
        // Two rows of operation buttons, laid out in the lower part of the workplate area.
        final int row1Y = topPos + 97;
        final int row2Y = topPos + 115;
        final int startX = leftPos + 8;
        final int btnW = 46;
        final int btnH = 16;
        final int gap = 2;

        // First row: Drawing, Upsetting, Flattening, Straightening (4 buttons)
        for (int i = 0; i < 4 && i < ops.length; i++)
        {
            final SmithingOperation op = ops[i];
            final OperationButton btn = new OperationButton(
                startX + i * (btnW + gap), row1Y, btnW, btnH,
                Component.literal(shortName(op)), op, b -> sendButtonClick(op.ordinal()));
            operationButtons.add(btn);
            addRenderableWidget(btn);
        }
        // Second row: Bending, Controlled Strike, Forge Weld (3 buttons, slightly wider)
        final int btnW2 = 62;
        for (int i = 4; i < ops.length; i++)
        {
            final SmithingOperation op = ops[i];
            final OperationButton btn = new OperationButton(
                startX + (i - 4) * (btnW2 + gap), row2Y, btnW2, btnH,
                Component.literal(shortName(op)), op, b -> sendButtonClick(op.ordinal()));
            operationButtons.add(btn);
            addRenderableWidget(btn);
        }

        // Strike button: wide, centred under the operation rows.
        final int strikeW = 92;
        final int strikeH = 18;
        final int strikeX = leftPos + (imageWidth - strikeW) / 2;
        final int strikeY = topPos + 135;
        strikeButton = Button.builder(
                Component.literal("⚒ ").append(Component.translatable("terravera.workplate.gui.strike")),
                b -> sendButtonClick(WorkplateContainer.BUTTON_STRIKE))
            .pos(strikeX, strikeY)
            .size(strikeW, strikeH)
            .build();
        addRenderableWidget(strikeButton);
    }

    private void sendButtonClick(int id)
    {
        if (minecraft != null && minecraft.gameMode != null)
        {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    // ───────────────────────────────── rendering ─────────────────────────────────

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY)
    {
        drawParchmentBackground(g);
        drawSlotFrames(g);
        drawSlotLabels(g);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY)
    {
        g.drawString(font, title, titleLabelX, titleLabelY, COL_TEXT_DARK, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, COL_TEXT_DARK, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick)
    {
        super.render(g, mouseX, mouseY, partialTick);
        renderToolInfoPanel(g);
        highlightActiveOperation();
        renderTooltip(g, mouseX, mouseY);
    }

    /** Draws the warm-brown parchment background with a double border, the way TFC frames its own screens. */
    private void drawParchmentBackground(GuiGraphics g)
    {
        final int x = leftPos, y = topPos, w = imageWidth, h = imageHeight;
        // Outer dark border
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, COL_BG_OUTER);
        // Main parchment fill
        g.fill(x, y, x + w, y + h, COL_BG_FILL);
        // Subtle inner bevel (light top/left, dark bottom/right)
        g.fill(x + 1, y + 1, x + w - 1, y + 2, 0xFFD9C09A);
        g.fill(x + 1, y + 1, x + 2, y + h - 1, 0xFFD9C09A);
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, 0xFF8B6F47);
        g.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, 0xFF8B6F47);

        // Title underline
        g.fill(x + 8, y + 18, x + w - 8, y + 19, COL_PANEL_BORDER);

        // Divider above the player inventory
        g.fill(x + 6, y + 154, x + w - 6, y + 155, COL_PANEL_BORDER);
    }

    /** Frames the three workplate slots with a darker inset so they read as recesses in the plate. */
    private void drawSlotFrames(GuiGraphics g)
    {
        drawSlotFrame(g, leftPos + 26 - 1, topPos + 25 - 1);
        drawSlotFrame(g, leftPos + 26 - 1, topPos + 50 - 1);
        drawSlotFrame(g, leftPos + 26 - 1, topPos + 75 - 1);
    }

    private void drawSlotFrame(GuiGraphics g, int x, int y)
    {
        g.fill(x, y, x + 18, y + 18, COL_SLOT_BORDER);
        g.fill(x + 1, y + 1, x + 17, y + 17, COL_SLOT_BG);
    }

    /** "Hammer", "Tool", "Flux" labels to the left of each slot. */
    private void drawSlotLabels(GuiGraphics g)
    {
        // Slot column headings are drawn in the info panel area instead, to keep the left gutter narrow.
    }

    /**
     * The heart of the GUI: a panel on the right that summarises everything the player needs to know about the tool
     * they are about to strike — name, durability, remaining metal mass, current temperature, shape metrics, and the
     * currently selected operation.
     */
    private void renderToolInfoPanel(GuiGraphics g)
    {
        final ItemStack tool = menu.getTool();
        final int panelX = leftPos + 52;
        final int panelY = topPos + 22;
        final int panelW = imageWidth - 58;
        final int panelH = 78;

        // Panel background
        g.fill(panelX - 1, panelY - 1, panelX + panelW + 1, panelY + panelH + 1, COL_PANEL_BORDER);
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, COL_PANEL);
        // Inner lighter strip for text
        g.fill(panelX + 2, panelY + 2, panelX + panelW - 2, panelY + panelH - 2, COL_BG_INNER);

        if (tool.isEmpty())
        {
            // Empty state: tell the player what the GUI wants.
            drawLabel(g, panelX + 4, panelY + 5,
                Component.translatable("terravera.workplate.gui.place_tool").withStyle(ChatFormatting.ITALIC),
                COL_TEXT_LABEL);
            drawLabel(g, panelX + 4, panelY + 17,
                Component.translatable("terravera.workplate.gui.place_hammer_hint").withStyle(ChatFormatting.ITALIC),
                COL_TEXT_LABEL);
            drawLabel(g, panelX + 4, panelY + 34,
                Component.translatable("terravera.workplate.gui.heat_in_forge").withStyle(ChatFormatting.DARK_GRAY),
                COL_TEXT_LABEL);
            return;
        }

        int y = panelY + 3;

        // ── Tool name ──
        drawLabel(g, panelX + 4, y, tool.getHoverName(), COL_TEXT_DARK);
        y += 11;

        // ── Durability bar ──
        final int maxDamage = Math.max(1, tool.getMaxDamage());
        final float durabilityFrac = 1f - (float) tool.getDamageValue() / maxDamage;
        drawLabel(g, panelX + 4, y, Component.translatable("terravera.workplate.gui.durability"), COL_TEXT_LABEL);
        drawBar(g, panelX + 56, y, panelW - 62, 6, durabilityFrac, durabilityColor(durabilityFrac));
        drawPercentText(g, panelX + panelW - 4, y, durabilityFrac);
        y += 10;

        // ── Metal mass bar ──
        final ToolMetalState state = menu.toolMetalState();
        final float massFrac = state.remainingMassFraction();
        drawLabel(g, panelX + 4, y, Component.translatable("terravera.workplate.gui.metal_mass"), COL_TEXT_LABEL);
        drawBar(g, panelX + 56, y, panelW - 62, 6, massFrac, massColor(massFrac));
        drawPercentText(g, panelX + panelW - 4, y, massFrac);
        y += 10;

        // ── Heat bar ──
        final IHeatView heat = menu.toolHeat();
        final float temperature = heat != null ? heat.getTemperature() : 0f;
        final float workTemp = heat != null ? heat.getWorkingTemperature() : 0f;
        final float weldTemp = heat != null ? heat.getWeldingTemperature() : 0f;
        final float displayMax = Math.max(1500f, Math.max(workTemp, weldTemp) * 1.1f);
        drawLabel(g, panelX + 4, y, Component.translatable("terravera.workplate.gui.heat"), COL_TEXT_LABEL);
        drawHeatBar(g, panelX + 56, y, panelW - 62, 6, temperature, workTemp, weldTemp, displayMax);
        // Temperature text
        final String tempText = Math.round(temperature) + "°C";
        g.drawString(font, tempText, panelX + panelW - 4 - font.width(tempText), y, heatTextColor(temperature, workTemp), false);
        y += 10;

        // ── Shape summary ──
        final Component shape = Component.translatable("terravera.workplate.gui.shape",
            state.length(), state.width(), state.thickness(), state.bend(), state.edge(), state.strain());
        drawLabel(g, panelX + 4, y, shape, COL_TEXT_LABEL);
        y += 11;

        // ── Selected operation ──
        final SmithingOperation selected = SmithingOperation.byId(state.operation());
        final MutableComponent opLine = Component.translatable("terravera.workplate.gui.operation")
            .append(": ").append(selected.displayName());
        drawLabel(g, panelX + 4, y, opLine, COL_TEXT_DARK);
    }

    // ───────────────────────────────── drawing helpers ─────────────────────────────────

    private void drawLabel(GuiGraphics g, int x, int y, Component text, int color)
    {
        g.drawString(font, text, x, y, color, false);
    }

    private void drawBar(GuiGraphics g, int x, int y, int w, int h, float fraction, int fillColor)
    {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, COL_BG_OUTER);
        g.fill(x, y, x + w, y + h, COL_BAR_BG);
        final int filled = Math.round(Mth.clamp(fraction, 0f, 1f) * w);
        if (filled > 0) g.fill(x, y, x + filled, y + h, fillColor);
    }

    private void drawPercentText(GuiGraphics g, int rightX, int y, float fraction)
    {
        final String s = Math.round(Mth.clamp(fraction, 0f, 1f) * 100f) + "%";
        g.drawString(font, s, rightX - font.width(s), y - 1, COL_TEXT_SHADOW, false);
    }

    /**
     * A heat bar that mirrors TFC's own temperature display: the fill colour moves from cold grey through dull red,
     * bright orange, working yellow, and finally welding white as the temperature climbs. Markers show the working
     * and welding thresholds so the player can see at a glance whether the tool is ready.
     */
    private void drawHeatBar(GuiGraphics g, int x, int y, int w, int h, float temperature, float workTemp, float weldTemp, float displayMax)
    {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, COL_BG_OUTER);
        g.fill(x, y, x + w, y + h, COL_BAR_BG);

        if (temperature > 0f)
        {
            final int filled = Math.round(Mth.clamp(temperature / displayMax, 0f, 1f) * w);
            final int color = heatBarColor(temperature, workTemp, weldTemp);
            if (filled > 0) g.fill(x, y, x + filled, y + h, color);
        }

        // Working-temperature marker
        if (workTemp > 0f)
        {
            final int mx = x + Math.round(workTemp / displayMax * w);
            g.fill(mx, y - 1, mx + 1, y + h + 1, 0xFFFFFFFF);
        }
        // Welding-temperature marker
        if (weldTemp > 0f && weldTemp > workTemp)
        {
            final int mx = x + Math.round(weldTemp / displayMax * w);
            g.fill(mx, y - 1, mx + 1, y + h + 1, 0xFFFFFF55);
        }
    }

    private int heatBarColor(float temperature, float workTemp, float weldTemp)
    {
        if (weldTemp > 0f && temperature >= weldTemp) return COL_HEAT_WELD;
        if (workTemp > 0f && temperature >= workTemp) return COL_HEAT_WORK;
        if (temperature >= workTemp * 0.7f) return COL_HEAT_HOT;
        if (temperature >= workTemp * 0.4f) return COL_HEAT_WARM;
        return COL_HEAT_COLD;
    }

    private int heatTextColor(float temperature, float workTemp)
    {
        if (workTemp <= 0f) return COL_TEXT_DARK;
        if (temperature >= workTemp) return 0xFFDD5500;
        if (temperature >= workTemp * 0.5f) return 0xFFAA3300;
        return COL_TEXT_DARK;
    }

    private int durabilityColor(float fraction)
    {
        if (fraction > 0.5f) return COL_DURABILITY_OK;
        if (fraction > 0.2f) return COL_DURABILITY_WARN;
        return COL_DURABILITY_LOW;
    }

    private int massColor(float fraction)
    {
        // Blend from steel-blue (full) to dark brown (nearly gone)
        final float t = Mth.clamp(fraction, 0f, 1f);
        final int r = (int) (0x55 + (0x88 - 0x55) * t);
        final int gr = (int) (0x44 + (0x99 - 0x44) * t);
        final int b = (int) (0x33 + (0xAA - 0x33) * t);
        return 0xFF000000 | (r << 16) | (gr << 8) | b;
    }

    /** Highlights the button whose operation is currently stored on the tool. */
    private void highlightActiveOperation()
    {
        final SmithingOperation active = SmithingOperation.byId(menu.toolMetalState().operation());
        for (OperationButton btn : operationButtons)
        {
            btn.active_ = btn.operation == active;
        }
    }

    // ───────────────────────────────── tooltips ─────────────────────────────────

    @Override
    protected void renderTooltip(GuiGraphics g, int mouseX, int mouseY)
    {
        super.renderTooltip(g, mouseX, mouseY);
        for (OperationButton btn : operationButtons)
        {
            if (btn.isMouseOver(mouseX, mouseY))
            {
                final List<Component> tip = new ArrayList<>();
                tip.add(btn.operation.displayName().copy().withStyle(ChatFormatting.GOLD));
                tip.add(Component.literal(btn.operation.description()).withStyle(ChatFormatting.GRAY));
                tip.add(Component.translatable("terravera.workplate.gui.repair_amount", btn.operation.repairPercent())
                    .withStyle(ChatFormatting.DARK_GREEN));
                tip.add(Component.translatable("terravera.workplate.gui.mass_loss",
                    String.format("%.1f%%", btn.operation.massLoss() * 100f / 100f))
                    .withStyle(ChatFormatting.DARK_RED));
                if (btn.operation == SmithingOperation.FORGE_WELD)
                {
                    tip.add(Component.translatable("terravera.workplate.gui.weld_requires")
                        .withStyle(ChatFormatting.RED));
                }
                g.renderComponentTooltip(font, tip, mouseX, mouseY);
                return;
            }
        }
    }

    // ───────────────────────────────── operation button ─────────────────────────────────

    private static String shortName(SmithingOperation op)
    {
        return switch (op)
        {
            case DRAWING -> "Draw";
            case UPSETTING -> "Upset";
            case FLATTENING -> "Flatten";
            case STRAIGHTENING -> "Straighten";
            case BENDING -> "Bend";
            case CONTROLLED_STRIKE -> "Controlled";
            case FORGE_WELD -> "Forge Weld";
        };
    }

    /**
     * A flat, parchment-styled button that lights up when its operation is the one currently stored on the tool.
     * Drawn procedurally so it matches the rest of the screen without needing a texture atlas.
     */
    private class OperationButton extends Button
    {
        final SmithingOperation operation;
        boolean active_ = false;

        OperationButton(int x, int y, int w, int h, Component label, SmithingOperation op, OnPress press)
        {
            super(x, y, w, h, label, press, DEFAULT_NARRATION);
            this.operation = op;
        }

        @Override
        public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick)
        {
            final boolean hovered = isMouseOver(mouseX, mouseY);
            final int fill = active_ ? COL_BTN_ACTIVE : hovered ? COL_BTN_HOVER : COL_BTN_NORMAL;
            final int textColor = active_ ? COL_TEXT_DARK : COL_BTN_TEXT;

            // Border
            g.fill(getX() - 1, getY() - 1, getX() + getWidth() + 1, getY() + getHeight() + 1, COL_BTN_BORDER);
            // Fill
            g.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), fill);
            // Top highlight
            g.fill(getX() + 1, getY() + 1, getX() + getWidth() - 1, getY() + 2,
                active_ ? 0xFFE8C887 : 0xFFA8854F);
            // Bottom shadow
            g.fill(getX() + 1, getY() + getHeight() - 2, getX() + getWidth() - 1, getY() + getHeight() - 1,
                0xFF5C4023);

            final Component msg = getMessage();
            final int tx = getX() + (getWidth() - font.width(msg)) / 2;
            final int ty = getY() + (getHeight() - 8) / 2;
            g.drawString(font, msg, tx, ty, textColor, false);
        }
    }
}
