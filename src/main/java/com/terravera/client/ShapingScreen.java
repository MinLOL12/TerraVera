/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import net.dries007.tfc.client.TFCSounds;
import net.dries007.tfc.client.screen.TFCContainerScreen;
import net.dries007.tfc.client.screen.button.KnappingButton;

import com.terravera.common.component.KnappedHead;
import com.terravera.common.container.ShapingContainer;
import com.terravera.common.knapping.HeadProfile;
import com.terravera.common.knapping.KnapAnalysis;
import com.terravera.common.knapping.KnapGrid;
import com.terravera.config.TerraVeraConfig;

/**
 * The TerraVera shaping screen. Uses TFC's native knapping GUI components
 * (buttons, textures, sounds) but displays TerraVera's function-based analysis.
 * <p>
 * This gives players the familiar TFC knapping experience while using TerraVera's
 * more flexible knapping system that doesn't require exact pattern matching.
 * <p>
 * <strong>Why the analysis is computed here, on the client:</strong> a knapping click is sent to the server as a
 * {@code ScreenButtonPacket}, so only the server runs {@link ShapingContainer#onButtonPress}. The container's
 * {@code feedback} field is therefore only ever populated on the server and is never synced back, which is exactly why
 * the analysis used to be invisible and the screen looked identical to vanilla TFC knapping - the client always read a
 * {@code null} feedback string. Instead we re-run the (pure, side-effect free) {@link KnapAnalysis} right here against
 * the shape the client can see. That shape is reconstructed from the <em>button visibility state</em> - each
 * {@link KnappingButton#onPress()} hides its button locally - rather than from {@code menu.getPattern()}, because this
 * screen (unlike TFC's own) does not mirror clicks into the client-side pattern. {@code HeadProfile.MANAGER} is synced
 * to clients like all of TerraFirmaCraft's data managers, so the client ranks against the very same profiles the server
 * uses and the two sides agree on what the player has made.
 */
public class ShapingScreen extends TFCContainerScreen<ShapingContainer>
{
    public static final ResourceLocation BACKGROUND = net.dries007.tfc.client.screen.KnappingScreen.BACKGROUND;

    private static final int GRID = 5;
    private static final int CELLS = GRID * GRID;

    private final ResourceLocation buttonTexture;

    /** The 25 knapping tiles, indexed by button id ({@code x + 5 * y}). */
    private final KnappingButton[] gridButtons = new KnappingButton[CELLS];

    /** Cached analysis, invalidated whenever the knapped shape (the visibility mask) changes. */
    private int lastMask = Integer.MIN_VALUE;
    @Nullable private Analysis cached;

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
        for (int x = 0; x < GRID; x++)
        {
            for (int y = 0; y < GRID; y++)
            {
                final int bx = (width - getXSize()) / 2 + 12 + 16 * x;
                final int by = (height - getYSize()) / 2 + 12 + 16 * y;
                final int id = x + GRID * y;
                final KnappingButton button = new KnappingButton(id, bx, by, 16, 16,
                    buttonTexture, TFCSounds.KNAP_STONE.holder());
                gridButtons[id] = button;
                // Reuse TerraFirmaCraft's stone knapping click sound
                addRenderableWidget(button);
            }
        }
        menu.setRequiresReset(true);
        // Buttons were recreated; drop any stale cached analysis.
        lastMask = Integer.MIN_VALUE;
        cached = null;
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

        // The function-based analysis, made visible. Recomputed only when the shape actually changes.
        final Analysis analysis = analyze();

        if (analysis.success() && analysis.kind() != null)
        {
            // What the geometry has produced - previewed with the same name the finished item will carry.
            drawCenteredPill(graphics, headName(analysis.kind()), topPos + 95, 0xFF55FF55);
            // How well it was worked: a gauge plus descriptor in the free top-right corner, above the output slot.
            final String descriptor = new KnappedHead(analysis.kind(), menu.stone().material(), analysis.quality())
                .qualityDescriptor();
            drawQualityPanel(graphics, analysis.quality(), descriptor);
        }
        else if (analysis.reason() != null && TerraVeraConfig.SERVER.showKnappingFeedback.get())
        {
            // Why the current shape is not yet a usable head - the "why isn't this working" hint.
            drawCenteredPill(graphics,
                Component.translatable("terravera.shaping.feedback." + analysis.reason()),
                topPos + 95, 0xFFFF9966);
        }
    }

    /**
     * Re-run the function-based analysis against the shape currently visible on the client. The result is cached against
     * the 25-bit visibility mask so the (cheap) geometry pass only runs when a tile is struck or the grid is reset.
     */
    private Analysis analyze()
    {
        int mask = 0;
        final boolean[] cells = new boolean[CELLS];
        for (int i = 0; i < CELLS; i++)
        {
            // A visible button means the stone is still there; a clicked (hidden) one has been flaked off.
            final boolean present = gridButtons[i] != null && gridButtons[i].visible;
            cells[i] = present;
            if (present) mask |= (1 << i);
        }

        if (mask == lastMask && cached != null)
        {
            return cached;
        }
        lastMask = mask;

        final KnapGrid grid = new KnapGrid(GRID, GRID, cells);
        final List<KnapAnalysis.Ranked.Candidate> candidates = new ArrayList<>();
        for (Map.Entry<ResourceLocation, HeadProfile> entry : HeadProfile.MANAGER.getElements().entrySet())
        {
            candidates.add(new KnapAnalysis.Ranked.Candidate(entry.getValue(), entry.getKey()));
        }

        final List<KnapAnalysis.Ranked> ranked = KnapAnalysis.rank(grid, candidates);
        final KnapAnalysis.Ranked best = ranked.isEmpty() ? null : ranked.getFirst();

        final Analysis analysis;
        if (best != null && best.outcome().success())
        {
            analysis = new Analysis(true, (ResourceLocation) best.candidate().owner(), best.outcome().quality(), null);
        }
        else
        {
            analysis = new Analysis(false, null, 0f, best == null ? null : best.outcome().reason());
        }
        cached = analysis;
        return analysis;
    }

    /**
     * The materialised name of the head the current shape produces, e.g. "Igneous Intrusive Wedge" - the same text the
     * finished item shows, so the screen previews exactly what you are about to get.
     */
    private Component headName(ResourceLocation kind)
    {
        return Component.translatable("terravera.head." + kind.getPath() + ".named",
            Component.translatable("terravera.material." + menu.stone().material()));
    }

    /**
     * Draw a line of text centred on the GUI with a faint backing pill so it stays legible over the knapping background.
     */
    private void drawCenteredPill(GuiGraphics graphics, Component text, int y, int color)
    {
        final int half = font.width(text.getString()) / 2;
        final int cx = leftPos + imageWidth / 2;
        graphics.fill(cx - half - 3, y - 1, cx + half + 4, y + 10, 0x99000000);
        graphics.drawCenteredString(font, text, cx, y, color);
    }

    /**
     * A small quality gauge tucked into the free top-right corner of the panel, above the output slot, with the coarse
     * workmanship descriptor beneath it. Gives an at-a-glance sense of how comfortably the piece cleared the bar.
     */
    private void drawQualityPanel(GuiGraphics graphics, float quality, String descriptor)
    {
        final int x0 = leftPos + 128, x1 = leftPos + 168;
        final int y0 = topPos + 20, y1 = topPos + 25;
        final float clamped = Mth.clamp(quality, 0f, 1f);
        final int color = clamped >= 0.8f ? 0xFF55FF55
            : clamped >= 0.6f ? 0xFFAAFF55
            : clamped >= 0.35f ? 0xFFFFDD55
            : 0xFFFF8855;
        graphics.fill(x0 - 1, y0 - 1, x1 + 1, y1 + 1, 0xFF000000);
        graphics.fill(x0, y0, x1, y1, 0xFF3A3A3A);
        graphics.fill(x0, y0, x0 + (int) ((x1 - x0) * clamped), y1, color);
        graphics.drawString(font, Component.translatable("terravera.quality." + descriptor), x0, topPos + 29, color, false);
    }

    @Override
    public boolean mouseDragged(double x, double y, int clickType, double dragX, double dragY)
    {
        // Dragging across tiles knaps them, as in TFC
        if (clickType == 0) mouseClicked(x, y, clickType);
        return super.mouseDragged(x, y, clickType, dragX, dragY);
    }

    /**
     * A snapshot of what the function-based analysis concluded about the current shape.
     *
     * @param success whether the shape currently qualifies as a usable head
     * @param kind    the working-end kind produced (e.g. {@code terravera:wedge}), only when {@code success}
     * @param quality how comfortably it cleared the bar, in [0, 1], only meaningful when {@code success}
     * @param reason  a translation-key suffix explaining the failure, only meaningful when not {@code success}
     */
    private record Analysis(boolean success, ResourceLocation kind, float quality, String reason) {}
}
