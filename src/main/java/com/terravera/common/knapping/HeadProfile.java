/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.knapping;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import net.dries007.tfc.util.data.DataManager;

import com.terravera.TerraVera;

/**
 * A shape requirement for one kind of tool head. This replaces TerraFirmaCraft's pixel-perfect knapping patterns:
 * instead of matching a fixed picture, a profile states what the head has to <em>be</em>.
 * <p>
 * The requirements are grouped the way a knapper would think about the piece - the {@link Base} it will be lashed or
 * struck at, the {@link Tip} that does the work, and the {@link Body} that connects them.
 * <p>
 * Profiles live in {@code data/<namespace>/terravera/head_profile/<name>.json}, so a modpack can retune them, or add
 * entirely new kinds of head, without touching code. The file name is the head kind, and must correspond to a
 * registered head item.
 *
 * @param base     requirements on the butt end
 * @param tip      requirements on the working end
 * @param body     requirements on the piece as a whole
 * @param priority profiles are tested in descending priority; among successes the highest priority wins. This is how
 *                 a piece that technically satisfies both "blade" and "broad" is resolved in favour of the finer,
 *                 more deliberate shape.
 */
public record HeadProfile(Base base, Tip tip, Body body, int priority)
{
    public static final Codec<HeadProfile> CODEC = RecordCodecBuilder.create(i -> i.group(
        Base.CODEC.optionalFieldOf("base", Base.ANY).forGetter(HeadProfile::base),
        Tip.CODEC.optionalFieldOf("tip", Tip.ANY).forGetter(HeadProfile::tip),
        Body.CODEC.optionalFieldOf("body", Body.ANY).forGetter(HeadProfile::body),
        Codec.INT.optionalFieldOf("priority", 0).forGetter(HeadProfile::priority)
    ).apply(i, HeadProfile::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, HeadProfile> STREAM_CODEC = StreamCodec.of(
        (buf, p) -> {
            Base.write(buf, p.base);
            Tip.write(buf, p.tip);
            Body.write(buf, p.body);
            buf.writeVarInt(p.priority);
        },
        buf -> new HeadProfile(Base.read(buf), Tip.read(buf), Body.read(buf), buf.readVarInt())
    );

    public static final DataManager<HeadProfile> MANAGER = new DataManager<>(
        TerraVera.identifier("head_profile"), CODEC, STREAM_CODEC);

    /**
     * Requirements on the butt end of the piece - what gets lashed to a haft, or struck.
     *
     * @param minWidth minimum contiguous width of the bottom row
     * @param minDepth minimum number of rows the base holds close to that width for
     * @param minSolid minimum solidity of the base region, in [0, 1]. Punishes notching out the butt
     * @param minRatio the base must be at least this fraction of the widest run in the piece. This is the real sturdy
     *                 base test - a broad head on a spindly stalk fails even though the head itself is fine
     */
    public record Base(int minWidth, int minDepth, float minSolid, float minRatio)
    {
        public static final Base ANY = new Base(1, 1, 0f, 0f);

        public static final Codec<Base> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("min_width", 1).forGetter(Base::minWidth),
            Codec.INT.optionalFieldOf("min_depth", 1).forGetter(Base::minDepth),
            Codec.FLOAT.optionalFieldOf("min_solid", 0f).forGetter(Base::minSolid),
            Codec.FLOAT.optionalFieldOf("min_ratio", 0f).forGetter(Base::minRatio)
        ).apply(i, Base::new));

        static void write(RegistryFriendlyByteBuf buf, Base b)
        {
            buf.writeVarInt(b.minWidth);
            buf.writeVarInt(b.minDepth);
            buf.writeFloat(b.minSolid);
            buf.writeFloat(b.minRatio);
        }

        static Base read(RegistryFriendlyByteBuf buf)
        {
            return new Base(buf.readVarInt(), buf.readVarInt(), buf.readFloat(), buf.readFloat());
        }
    }

    /**
     * Requirements on the working end.
     *
     * @param minWidth     minimum width of the tip row. Stops a hammer being accepted as a spear point
     * @param maxWidth     maximum width of the tip row. 1 is a point, 2-3 an edge, 4-5 effectively blunt
     * @param minEdge      minimum rows of worked edge, counted down from the tip while the row stays 2 wide or less
     * @param maxEdge      maximum rows of worked edge. A hammer has zero, a knife has many
     * @param minNarrowing how much narrower the tip must be than the widest run. 0 requires no working end at all
     * @param maxNarrowing how much narrower the tip may be. Caps a maul at "no real edge"
     * @param minTaper     minimum taper consistency, in [0, 1]. Punishes a piece that bulges back out above the base
     */
    public record Tip(int minWidth, int maxWidth, int minEdge, int maxEdge, int minNarrowing, int maxNarrowing, float minTaper)
    {
        public static final Tip ANY = new Tip(0, 5, 0, 99, 0, 99, 0f);

        public static final Codec<Tip> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("min_width", 0).forGetter(Tip::minWidth),
            Codec.INT.optionalFieldOf("max_width", 5).forGetter(Tip::maxWidth),
            Codec.INT.optionalFieldOf("min_edge_length", 0).forGetter(Tip::minEdge),
            Codec.INT.optionalFieldOf("max_edge_length", 99).forGetter(Tip::maxEdge),
            Codec.INT.optionalFieldOf("min_narrowing", 0).forGetter(Tip::minNarrowing),
            Codec.INT.optionalFieldOf("max_narrowing", 99).forGetter(Tip::maxNarrowing),
            Codec.FLOAT.optionalFieldOf("min_taper", 0f).forGetter(Tip::minTaper)
        ).apply(i, Tip::new));

        static void write(RegistryFriendlyByteBuf buf, Tip t)
        {
            buf.writeVarInt(t.minWidth);
            buf.writeVarInt(t.maxWidth);
            buf.writeVarInt(t.minEdge);
            buf.writeVarInt(t.maxEdge);
            buf.writeVarInt(t.minNarrowing);
            buf.writeVarInt(t.maxNarrowing);
            buf.writeFloat(t.minTaper);
        }

        static Tip read(RegistryFriendlyByteBuf buf)
        {
            return new Tip(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                buf.readVarInt(), buf.readVarInt(), buf.readFloat());
        }
    }

    /**
     * Requirements on the piece as a whole.
     *
     * @param minMass     minimum surviving squares, so a tool head cannot be a single chip
     * @param maxMass     maximum surviving squares, so an untouched cobble is not already an axe
     * @param minAspect   minimum height / width
     * @param maxAspect   maximum height / width
     * @param minSymmetry minimum left/right symmetry, in [0, 1]. Only really matters for thrown weapons
     * @param requireConnected whether the piece must be one connected body. Two flakes side by side are not a tool
     */
    public record Body(int minMass, int maxMass, float minAspect, float maxAspect, float minSymmetry, boolean requireConnected)
    {
        public static final Body ANY = new Body(1, 25, 0f, 99f, 0f, true);

        public static final Codec<Body> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("min_mass", 1).forGetter(Body::minMass),
            Codec.INT.optionalFieldOf("max_mass", 25).forGetter(Body::maxMass),
            Codec.FLOAT.optionalFieldOf("min_aspect", 0f).forGetter(Body::minAspect),
            Codec.FLOAT.optionalFieldOf("max_aspect", 99f).forGetter(Body::maxAspect),
            Codec.FLOAT.optionalFieldOf("min_symmetry", 0f).forGetter(Body::minSymmetry),
            Codec.BOOL.optionalFieldOf("require_connected", true).forGetter(Body::requireConnected)
        ).apply(i, Body::new));

        static void write(RegistryFriendlyByteBuf buf, Body b)
        {
            buf.writeVarInt(b.minMass);
            buf.writeVarInt(b.maxMass);
            buf.writeFloat(b.minAspect);
            buf.writeFloat(b.maxAspect);
            buf.writeFloat(b.minSymmetry);
            buf.writeBoolean(b.requireConnected);
        }

        static Body read(RegistryFriendlyByteBuf buf)
        {
            return new Body(buf.readVarInt(), buf.readVarInt(), buf.readFloat(), buf.readFloat(),
                buf.readFloat(), buf.readBoolean());
        }
    }

    /**
     * Test a set of measurements against this profile. The order of the checks is deliberate: it runs from the most
     * fundamental failure (the piece fell apart) to the most cosmetic (it is a bit lopsided), so that the reason
     * reported back to the player is the most useful one.
     */
    public Result test(KnapMetrics m)
    {
        if (body.requireConnected && !m.connected()) return Result.fail("shattered");
        if (m.mass() < body.minMass) return Result.fail("too_little_stone");
        if (m.mass() > body.maxMass) return Result.fail("too_much_stone");

        if (m.baseWidth() < base.minWidth) return Result.fail("base_too_narrow");
        if (m.baseWidth() < base.minRatio * m.widestRun()) return Result.fail("base_too_narrow");
        if (m.baseDepth() < base.minDepth) return Result.fail("base_too_shallow");
        if (m.baseSolid() < base.minSolid) return Result.fail("base_too_notched");

        if (m.tipWidth() > tip.maxWidth) return Result.fail("tip_too_blunt");
        if (m.tipWidth() < tip.minWidth) return Result.fail("tip_too_fine");
        if (m.tipTaper() < tip.minTaper) return Result.fail("no_taper");

        final int narrowing = m.widestRun() - m.tipWidth();
        if (narrowing < tip.minNarrowing) return Result.fail("tip_too_blunt");
        if (narrowing > tip.maxNarrowing) return Result.fail("no_working_end");
        if (m.edgeLength() < tip.minEdge) return Result.fail("tip_too_blunt");
        if (m.edgeLength() > tip.maxEdge) return Result.fail("tip_too_fine");

        if (m.aspect() < body.minAspect) return Result.fail("too_stubby");
        if (m.aspect() > body.maxAspect) return Result.fail("too_slender");
        if (m.symmetry() < body.minSymmetry) return Result.fail("lopsided");

        // Quality: how comfortably the piece clears the bar, in [0, 1]. Scales the durability of the finished tool.
        final float quality =
            clamp01((m.baseWidth() - base.minWidth + 1f) / 3f) * 0.4f
                + clamp01(m.baseSolid()) * 0.2f
                + clamp01((tip.maxWidth - m.tipWidth() + 1f) / 3f) * 0.2f
                + clamp01(m.tipTaper()) * 0.1f
                + clamp01(m.symmetry()) * 0.1f;
        return Result.pass(clamp01(quality));
    }

    private static float clamp01(float value)
    {
        return value < 0f ? 0f : Math.min(value, 1f);
    }

    /**
     * @param quality in [0, 1], only meaningful when {@link #success()}
     * @param reason  a translation key suffix explaining the failure, only meaningful when it failed
     */
    public record Result(boolean success, float quality, @Nullable String reason)
    {
        public static Result pass(float quality)
        {
            return new Result(true, quality, null);
        }

        public static Result fail(String reason)
        {
            return new Result(false, 0f, reason);
        }
    }
}
