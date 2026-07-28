package com.terravera.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.terravera.common.blocks.PostedPaperBlock;
import com.terravera.common.blocks.PostedPaperBlockEntity;
import com.terravera.common.paper.PaperContent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;

/**
 * Renders the posted paper sheet as a thin quad on the wall, with freehand strokes and text.
 * Uses the same coordinate system as the editing screen (normalized 0-1).
 */
public class PostedPaperRenderer implements BlockEntityRenderer<PostedPaperBlockEntity>
{
    public PostedPaperRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(PostedPaperBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay)
    {
        PaperContent content = be.getContent();
        if (content == null) return;

        var state = be.getBlockState();
        if (!state.hasProperty(PostedPaperBlock.FACING)) return;
        Direction facing = state.getValue(PostedPaperBlock.FACING);

        pose.pushPose();

        // Move to center of block face
        float inset = 0.001f;
        // Position paper slightly off wall to avoid z-fighting
        switch (facing)
        {
            case NORTH -> { pose.translate(0, 0, 1 - inset); }
            case SOUTH -> { pose.translate(1, 0, inset); pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180)); }
            case WEST -> { pose.translate(1 - inset, 0, 1); pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90)); }
            case EAST -> { pose.translate(inset, 0, 0); pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-90)); }
            default -> {}
        }

        // Paper is 12/16 wide = 0.75 from 0.125 to 0.875 in both X/Y (matching block shape)
        // Map normalized paper coords to world: paper quad at X 0.125-0.875, Y 0.125-0.875 (inverted Y)
        // Now pose is at local origin for the face. We need to render a quad facing us.
        // After translation/rotation, local X is along wall width, local Y is vertical, Z is depth out.
        // We'll render using manual vertices.

        VertexConsumer vcPaper = buffers.getBuffer(RenderType.solid());
        Matrix4f mat = pose.last().pose();

        // Paper quad vertices (counter-clockwise)
        float minX = 0.125f;
        float maxX = 0.875f;
        float minY = 0.125f;
        float maxY = 0.875f;
        // Paper color: warm off-white, convert to RGB float 0xF5E9C9 -> 245,233,201
        float r = 245/255f, g = 233/255f, b = 201/255f;

        // Front face
        // Use light
        // Order: bottom-left, bottom-right, top-right, top-left – but in our local space Y up
        // For south after rotation, same.
        addQuad(vcPaper, mat, minX, minY, maxX, maxY, r, g, b, 1f, light);

        // Render tape corners if we want – two small strips top corners yellowish
        VertexConsumer vcTape = buffers.getBuffer(RenderType.solid());
        float tapeR = 0.85f, tapeG = 0.8f, tapeB = 0.6f; // masking tape beige
        float tapeSize = 0.12f;
        // top-left tape
        addQuad(vcTape, mat, minX - 0.02f, maxY - tapeSize, minX + tapeSize, maxY + 0.02f, tapeR, tapeG, tapeB, 0.9f, light);
        // top-right tape
        addQuad(vcTape, mat, maxX - tapeSize, maxY - tapeSize, maxX + 0.02f, maxY + 0.02f, tapeR, tapeG, tapeB, 0.9f, light);

        // Render strokes – we draw them as small quads on top of paper, slightly offset forward
        pose.translate(0,0,0.001f);
        mat = pose.last().pose();
        VertexConsumer vcInk = buffers.getBuffer(RenderType.solid());

        if (content.strokes() != null)
        {
            for (PaperContent.Stroke stroke : content.strokes())
            {
                if (stroke.points().size() < 2) continue;
                int argb = stroke.color();
                float sr = ((argb >> 16) & 0xFF)/255f;
                float sg = ((argb >> 8) & 0xFF)/255f;
                float sb = (argb & 0xFF)/255f;
                float thick = stroke.thickness() * 0.005f; // convert pixel thickness to world units
                for (int i = 0; i < stroke.points().size()-1; i++)
                {
                    var a = stroke.points().get(i);
                    var bb = stroke.points().get(i+1);
                    float ax = lerp(minX, maxX, a.x());
                    float ay = lerp(minY, maxY, 1f - a.y()); // invert Y because screen Y down, world Y up
                    float bx = lerp(minX, maxX, bb.x());
                    float by = lerp(minY, maxY, 1f - bb.y());
                    // draw thick line as quad
                    drawLineQuad(vcInk, mat, ax, ay, bx, by, thick, sr, sg, sb, 1f, light);
                }
            }
        }

        pose.popPose();

        // Render text as floating text slightly in front of paper – use Font renderer for crispness
        if (content.text() != null && !content.text().isBlank())
        {
            pose.pushPose();
            switch (facing)
            {
                case NORTH -> pose.translate(0.5, 0.6, 1 - inset - 0.002);
                case SOUTH -> { pose.translate(0.5, 0.6, inset + 0.002); pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180)); }
                case WEST -> { pose.translate(1 - inset - 0.002, 0.6, 0.5); pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90)); }
                case EAST -> { pose.translate(inset + 0.002, 0.6, 0.5); pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-90)); }
                default -> {}
            }
            pose.scale(0.007f, -0.007f, 0.007f); // small text
            // Center text
            Font font = Minecraft.getInstance().font;
            String truncated = content.text().length() > 100 ? content.text().substring(0, 100) + "..." : content.text();
            // Word wrap manually: split into lines ~20 chars
            var lines = font.split(Component.literal(truncated), 80);
            int y = 0;
            for (var line : lines)
            {
                font.drawInBatch(line, -40, y, 0xFF000000, false, pose.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, light);
                y += 10;
            }
            pose.popPose();
        }
    }

    private static void addQuad(VertexConsumer vc, Matrix4f mat, float x0, float y0, float x1, float y1, float r, float g, float b, float a, int light)
    {
        // z=0 quad in local XY plane, normal facing +Z (out from wall)
        vc.addVertex(mat, x0, y0, 0).setColor(r,g,b,a).setUv(0,0).setOverlay(0).setLight(light).setNormal(0,0,1);
        vc.addVertex(mat, x1, y0, 0).setColor(r,g,b,a).setUv(1,0).setOverlay(0).setLight(light).setNormal(0,0,1);
        vc.addVertex(mat, x1, y1, 0).setColor(r,g,b,a).setUv(1,1).setOverlay(0).setLight(light).setNormal(0,0,1);
        vc.addVertex(mat, x0, y1, 0).setColor(r,g,b,a).setUv(0,1).setOverlay(0).setLight(light).setNormal(0,0,1);
    }

    private static void drawLineQuad(VertexConsumer vc, Matrix4f mat, float x0, float y0, float x1, float y1, float thick, float r, float g, float b, float a, int light)
    {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float len = (float)Math.sqrt(dx*dx + dy*dy);
        if (len < 0.0001f) return;
        float nx = -dy / len * thick;
        float ny = dx / len * thick;

        vc.addVertex(mat, x0 + nx, y0 + ny, 0).setColor(r,g,b,a).setUv(0,0).setOverlay(0).setLight(light).setNormal(0,0,1);
        vc.addVertex(mat, x0 - nx, y0 - ny, 0).setColor(r,g,b,a).setUv(0,0).setOverlay(0).setLight(light).setNormal(0,0,1);
        vc.addVertex(mat, x1 - nx, y1 - ny, 0).setColor(r,g,b,a).setUv(0,0).setOverlay(0).setLight(light).setNormal(0,0,1);
        vc.addVertex(mat, x1 + nx, y1 + ny, 0).setColor(r,g,b,a).setUv(0,0).setOverlay(0).setLight(light).setNormal(0,0,1);
    }

    private static float lerp(float a, float b, float t) { return a + (b-a)*t; }
}
