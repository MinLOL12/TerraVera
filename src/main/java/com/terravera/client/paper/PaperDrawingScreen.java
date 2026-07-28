package com.terravera.client.paper;

import com.terravera.common.network.TerraVeraNetwork;
import com.terravera.common.paper.PaperContent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Real writable/drawable paper screen.
 * - Left/top: text box for typing for real.
 * - Center: canvas where mouse drag draws ink.
 * - Bottom: color palette, brush size, clear, done.
 *
 * Strokes stored normalized 0-1.
 */
public class PaperDrawingScreen extends Screen
{
    private final ItemStack paperStack;
    private final InteractionHand hand;
    private PaperContent initialContent;

    // Canvas bounds
    private int canvasX, canvasY, canvasW, canvasH;

    private EditBox textBox;

    private final List<PaperContent.Stroke> strokes = new ArrayList<>();
    private List<PaperContent.Point> currentStrokePoints = null;
    private int currentColor = 0xFF000000; // black ink
    private float currentThickness = 2.0f;

    private final int[] palette = new int[] {
        0xFF000000, // iron gall black
        0xFF332211, // brown ink (walnut)
        0xFF8B0000, // red (vermillion)
        0xFF000080, // blue (indigo)
        0xFF006400, // green
        0xFF555555, // charcoal gray
    };

    private PaperDrawingScreen(ItemStack stack, InteractionHand hand, PaperContent content)
    {
        super(Component.translatable("terravera.paper.edit_title"));
        this.paperStack = stack;
        this.hand = hand;
        this.initialContent = content == null ? PaperContent.EMPTY : content;
        this.strokes.addAll(this.initialContent.strokes());
    }

    public static void open(ItemStack stack, InteractionHand hand)
    {
        Minecraft mc = Minecraft.getInstance();
        PaperContent content = stack.get(com.terravera.common.TerraVeraDataComponents.PAPER_CONTENT.get());
        if (content == null) content = PaperContent.EMPTY;
        mc.setScreen(new PaperDrawingScreen(stack, hand, content));
    }

    @Override
    protected void init()
    {
        int margin = 20;
        canvasW = width - margin * 2 - 40; // leave space for palette
        canvasH = height - 100;
        canvasX = margin;
        canvasY = 30;

        // Text box for real writing
        textBox = new EditBox(this.font, canvasX, canvasY + canvasH + 5, canvasW, 20, Component.translatable("terravera.paper.text_hint"));
        textBox.setMaxLength(1000);
        textBox.setValue(initialContent.text());
        textBox.setHint(Component.translatable("terravera.paper.text_hint"));
        addRenderableWidget(textBox);

        int btnW = 60, btnH = 20;
        int rightX = canvasX + canvasW + 10;

        for (int i = 0; i < palette.length; i++)
        {
            final int col = palette[i];
            int px = rightX;
            int py = canvasY + i * 24;
            addRenderableWidget(Button.builder(Component.literal("■"), b -> currentColor = col)
                .bounds(px, py, 20, 20)
                .build());
        }

        // Thickness buttons
        addRenderableWidget(Button.builder(Component.literal("Thin"), b -> currentThickness = 1.0f)
            .bounds(rightX, canvasY + 160, 40, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Med"), b -> currentThickness = 2.5f)
            .bounds(rightX, canvasY + 182, 40, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Thick"), b -> currentThickness = 4.5f)
            .bounds(rightX, canvasY + 204, 40, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("terravera.paper.clear"), b -> strokes.clear())
            .bounds(rightX, canvasY + 230, 40, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> saveAndClose())
            .bounds(width / 2 - 40, height - 25, 80, 20).build());

        setInitialFocus(textBox);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick)
    {
        super.render(gfx, mouseX, mouseY, partialTick);

        // Paper background
        gfx.fill(canvasX - 2, canvasY - 2, canvasX + canvasW + 2, canvasY + canvasH + 2, 0xFF3D2E1E); // border like desk
        gfx.fill(canvasX, canvasY, canvasX + canvasW, canvasY + canvasH, 0xFFF5E9C9); // paper color: warm off-white bast paper

        // Draw existing strokes
        for (PaperContent.Stroke s : strokes)
        {
            drawStroke(gfx, s);
        }
        if (currentStrokePoints != null && currentStrokePoints.size() >= 2)
        {
            PaperContent.Stroke preview = new PaperContent.Stroke(currentColor, currentThickness, List.copyOf(currentStrokePoints));
            drawStroke(gfx, preview);
        }

        // Render text preview on paper top
        String txt = textBox.getValue();
        if (!txt.isBlank())
        {
            // Wrap text
            int tw = canvasW - 10;
            int ty = canvasY + 5;
            gfx.drawWordWrap(font, Component.literal(txt), canvasX + 5, ty, tw, 0xFF000000);
        }

        // Show current brush indicator
        gfx.fill(canvasX + canvasW + 10, canvasY + 260, canvasX + canvasW + 30, canvasY + 280, currentColor);
    }

    private void drawStroke(GuiGraphics gfx, PaperContent.Stroke stroke)
    {
        if (stroke.points().size() < 2) return;
        int color = stroke.color();
        float thick = stroke.thickness();
        // Convert normalized points to screen
        for (int i = 0; i < stroke.points().size() - 1; i++)
        {
            PaperContent.Point a = stroke.points().get(i);
            PaperContent.Point b = stroke.points().get(i+1);
            int ax = canvasX + (int)(a.x() * canvasW);
            int ay = canvasY + (int)(a.y() * canvasH);
            int bx = canvasX + (int)(b.x() * canvasW);
            int by = canvasY + (int)(b.y() * canvasH);
            drawThickLine(gfx, ax, ay, bx, by, thick, color);
        }
    }

    private void drawThickLine(GuiGraphics gfx, int x1, int y1, int x2, int y2, float thickness, int color)
    {
        int t = Math.max(1, Math.round(thickness));
        // Bresenham-like thick line via filling
        int dx = x2 - x1;
        int dy = y2 - y1;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        if (steps == 0) { gfx.fill(x1 - t/2, y1 - t/2, x1 + t/2, y1 + t/2, color); return; }
        for (int i = 0; i <= steps; i++)
        {
            float tt = (float)i / steps;
            int x = (int)(x1 + dx * tt);
            int y = (int)(y1 + dy * tt);
            gfx.fill(x - t/2, y - t/2, x + t/2 + 1, y + t/2 + 1, color);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button)
    {
        if (button == 0 && isInsideCanvas(mx, my))
        {
            currentStrokePoints = new ArrayList<>();
            currentStrokePoints.add(toCanvasPoint(mx, my));
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy)
    {
        if (button == 0 && currentStrokePoints != null)
        {
            // Clamp to canvas
            if (isInsideCanvas(mx, my) || true) // allow slight outside
            {
                PaperContent.Point p = toCanvasPointClamped(mx, my);
                // Avoid duplicate very close points
                if (currentStrokePoints.isEmpty() || dist(currentStrokePoints.get(currentStrokePoints.size()-1), p) > 0.002f)
                    currentStrokePoints.add(p);
            }
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button)
    {
        if (button == 0 && currentStrokePoints != null)
        {
            if (currentStrokePoints.size() >= 2)
            {
                strokes.add(new PaperContent.Stroke(currentColor, currentThickness, List.copyOf(currentStrokePoints)));
            }
            else if (currentStrokePoints.size() == 1)
            {
                // dot
                var p = currentStrokePoints.get(0);
                var p2 = new PaperContent.Point(p.x()+0.001f, p.y()+0.001f);
                strokes.add(new PaperContent.Stroke(currentColor, currentThickness, List.of(p, p2)));
            }
            currentStrokePoints = null;
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    private boolean isInsideCanvas(double mx, double my)
    {
        return mx >= canvasX && mx < canvasX + canvasW && my >= canvasY && my < canvasY + canvasH;
    }

    private PaperContent.Point toCanvasPoint(double mx, double my)
    {
        float nx = (float)((mx - canvasX) / canvasW);
        float ny = (float)((my - canvasY) / canvasH);
        return new PaperContent.Point(nx, ny);
    }
    private PaperContent.Point toCanvasPointClamped(double mx, double my)
    {
        float nx = (float)((mx - canvasX) / canvasW);
        float ny = (float)((my - canvasY) / canvasH);
        nx = Math.clamp(nx, 0f, 1f);
        ny = Math.clamp(ny, 0f, 1f);
        return new PaperContent.Point(nx, ny);
    }

    private float dist(PaperContent.Point a, PaperContent.Point b)
    {
        float dx = a.x()-b.x();
        float dy = a.y()-b.y();
        return (float)Math.sqrt(dx*dx+dy*dy);
    }

    private void saveAndClose()
    {
        String text = textBox.getValue();
        PaperContent content = new PaperContent(text, List.copyOf(strokes));
        // Send to server
        try
        {
            PacketDistributor.sendToServer(new TerraVeraNetwork.SavePaperPayload(content, hand));
        }
        catch (Exception e)
        {
            // fallback
        }
        // Also update client stack immediately for responsiveness
        paperStack.set(com.terravera.common.TerraVeraDataComponents.PAPER_CONTENT.get(), content);
        this.onClose();
    }

    @Override
    public void onClose()
    {
        super.onClose();
    }
}
