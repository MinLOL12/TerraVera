package com.terravera.common.paper;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

/**
 * Real paper content: freehand strokes + typed text.
 * Stored on the ItemStack via data component, and on the posted-paper block entity.
 * Strokes are normalized 0-1 coordinates relative to the paper canvas, so they render
 * at any scale (item tooltip, screen, wall block).
 */
public record PaperContent(String text, List<Stroke> strokes)
{
    public static final PaperContent EMPTY = new PaperContent("", List.of());

    // ----- Point -----
    public record Point(float x, float y)
    {
        public static final Codec<Point> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.FLOAT.fieldOf("x").forGetter(Point::x),
            Codec.FLOAT.fieldOf("y").forGetter(Point::y)
        ).apply(i, Point::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Point> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, Point::x,
            ByteBufCodecs.FLOAT, Point::y,
            Point::new
        );
    }

    // ----- Stroke -----
    public record Stroke(int color, float thickness, List<Point> points)
    {
        public static final Codec<Stroke> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("color").forGetter(Stroke::color),
            Codec.FLOAT.fieldOf("thickness").forGetter(Stroke::thickness),
            Point.CODEC.listOf().fieldOf("points").forGetter(Stroke::points)
        ).apply(i, Stroke::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Stroke> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, Stroke::color,
            ByteBufCodecs.FLOAT, Stroke::thickness,
            Point.STREAM_CODEC.apply(ByteBufCodecs.list()), Stroke::points,
            Stroke::new
        );

        public boolean isEmpty() { return points == null || points.size() < 2; }
    }

    public static final Codec<PaperContent> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.optionalFieldOf("text", "").forGetter(PaperContent::text),
        Stroke.CODEC.listOf().optionalFieldOf("strokes", List.of()).forGetter(PaperContent::strokes)
    ).apply(i, PaperContent::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PaperContent> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, PaperContent::text,
        Stroke.STREAM_CODEC.apply(ByteBufCodecs.list()), PaperContent::strokes,
        PaperContent::new
    );

    public PaperContent
    {
        if (text == null) text = "";
        if (strokes == null) strokes = List.of();
        // defensive copy to mutable list for safety? Keep immutable but allow empty.
        strokes = List.copyOf(strokes);
    }

    public boolean isEmpty()
    {
        return (text == null || text.isBlank()) && (strokes == null || strokes.isEmpty() || strokes.stream().allMatch(Stroke::isEmpty));
    }

    public PaperContent withText(String newText)
    {
        return new PaperContent(newText, this.strokes);
    }

    public PaperContent withStrokes(List<Stroke> newStrokes)
    {
        return new PaperContent(this.text, newStrokes);
    }
}
