package com.terravera.client.paper;

import com.terravera.common.network.TerraVeraNetwork;
import com.terravera.common.paper.PaperContent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen for viewing and editing a posted paper block.
 * Allows re-drawing and re-typing, then saving back to block entity via packet.
 */
public class PostedPaperScreen extends Screen
{
    private final BlockPos pos;
    private PaperContent initial;

    private int canvasX, canvasY, canvasW, canvasH;
    private EditBox textBox;
    private final List<PaperContent.Stroke> strokes = new ArrayList<>();
    private List<PaperContent.Point> currentStrokePoints = null;
    private int currentColor = 0xFF000000;
    private float currentThickness = 2.0f;

    private final int[] palette = new int[] {
        0xFF000000, 0xFF332211, 0xFF8B0000, 0xFF000080, 0xFF006400, 0xFF555555,
    };

    private PostedPaperScreen(BlockPos pos, PaperContent content)
    {
        super(Component.translatable("terravera.posted_paper.edit_title"));
        this.pos = pos;
        this.initial = content == null ? PaperContent.EMPTY : content;
        this.strokes.addAll(this.initial.strokes());
    }

    public static void open(BlockPos pos, PaperContent content)
    {
        Minecraft.getInstance().setScreen(new PostedPaperScreen(pos, content));
    }

    @Override
    protected void init()
    {
        int margin = 20;
        canvasW = width - margin * 2 - 40;
        canvasH = height - 100;
        canvasX = margin;
        canvasY = 30;

        textBox = new EditBox(this.font, canvasX, canvasY + canvasH + 5, canvasW, 20, Component.translatable("terravera.paper.text_hint"));
        textBox.setMaxLength(1000);
        textBox.setValue(initial.text());
        addRenderableWidget(textBox);

        int rightX = canvasX + canvasW + 10;
        for (int i = 0; i < palette.length; i++)
        {
            final int col = palette[i];
            int py = canvasY + i * 24;
            addRenderableWidget(Button.builder(Component.literal("■"), b -> currentColor = col)
                .bounds(rightX, py, 20, 20).build());
        }
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
    public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick)
    {
        super.render(gfx, mouseX, mouseY, partialTick);
        gfx.fill(canvasX - 2, canvasY - 2, canvasX + canvasW + 2, canvasY + canvasH + 2, 0xFF3D2E1E);
        gfx.fill(canvasX, canvasY, canvasX + canvasW, canvasY + canvasH, 0xFFF5E9C9);

        for (PaperContent.Stroke s : strokes) drawStroke(gfx, s);
        if (currentStrokePoints != null && currentStrokePoints.size() >= 2)
        {
            drawStroke(gfx, new PaperContent.Stroke(currentColor, currentThickness, List.copyOf(currentStrokePoints)));
        }

        String txt = textBox.getValue();
        if (!txt.isBlank())
        {
            gfx.drawWordWrap(font, Component.literal(txt), canvasX + 5, canvasY + 5, canvasW - 10, 0xFF000000);
        }
        gfx.fill(canvasX + canvasW + 10, canvasY + 260, canvasX + canvasW + 30, canvasY + 280, currentColor);
    }

    private void drawStroke(GuiGraphics gfx, PaperContent.Stroke stroke)
    {
        if (stroke.points().size() < 2) return;
        for (int i = 0; i < stroke.points().size() - 1; i++)
        {
            var a = stroke.points().get(i);
            var b = stroke.points().get(i+1);
            int ax = canvasX + (int)(a.x()*canvasW);
            int ay = canvasY + (int)(a.y()*canvasH);
            int bx = canvasX + (int)(b.x()*canvasW);
            int by = canvasY + (int)(b.y()*canvasH);
            int t = Math.max(1, Math.round(stroke.thickness()));
            int dx = bx - ax, dy = by - ay;
            int steps = Math.max(Math.abs(dx), Math.abs(dy));
            if (steps == 0) { gfx.fill(ax - t/2, ay - t/2, ax + t/2, ay + t/2, stroke.color()); continue; }
            for (int s = 0; s <= steps; s++)
            {
                float tt = (float)s/steps;
                int x = (int)(ax + dx * tt);
                int y = (int)(ay + dy * tt);
                gfx.fill(x - t/2, y - t/2, x + t/2 +1, y + t/2 +1, stroke.color());
            }
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button)
    {
        if (button == 0 && mx >= canvasX && mx < canvasX+canvasW && my >= canvasY && my < canvasY+canvasH)
        {
            currentStrokePoints = new ArrayList<>();
            currentStrokePoints.add(new PaperContent.Point((float)((mx - canvasX)/canvasW), (float)((my - canvasY)/canvasH)));
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy)
    {
        if (button == 0 && currentStrokePoints != null)
        {
            float nx = (float)((mx - canvasX)/canvasW);
            float ny = (float)((my - canvasY)/canvasH);
            nx = Math.clamp(nx, 0f, 1f);
            ny = Math.clamp(ny, 0f, 1f);
            var p = new PaperContent.Point(nx, ny);
            if (currentStrokePoints.isEmpty() || dist(currentStrokePoints.get(currentStrokePoints.size()-1), p) > 0.002f)
                currentStrokePoints.add(p);
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
                strokes.add(new PaperContent.Stroke(currentColor, currentThickness, List.copyOf(currentStrokePoints)));
            currentStrokePoints = null;
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    private float dist(PaperContent.Point a, PaperContent.Point b) { float dx=a.x()-b.x(), dy=a.y()-b.y(); return (float)Math.sqrt(dx*dx+dy*dy); }

    private void saveAndClose()
    {
        PaperContent content = new PaperContent(textBox.getValue(), List.copyOf(strokes));
        try { PacketDistributor.sendToServer(new TerraVeraNetwork.SavePostedPaperPayload(pos, content)); } catch (Exception ignored) {}
        onClose();
    }
}
