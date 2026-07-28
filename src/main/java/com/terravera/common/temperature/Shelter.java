/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.temperature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Reads the building the player is standing in and works out what it is doing to them.
 * <p>
 * The design goal is that <strong>the hut you actually built matters</strong>, and matters for the reasons a real hut
 * would, without asking the player to learn a hidden scoring rubric. The survey answers four questions a builder
 * already understands:
 * <ol>
 *     <li><strong>Is it enclosed?</strong> A roof and walls stop the wind and hold air still. This is by far the
 *     largest single effect, and it is the reason to build anything at all.</li>
 *     <li><strong>Is it sealed, or draughty?</strong> Every gap in the shell leaks the warm air out. A doorway you
 *     never closed is a draught, and the model says so.</li>
 *     <li><strong>What is it made of?</strong> Stone has enormous thermal mass: it lags behind the outside, so it
 *     stays cool through a hot afternoon and holds warmth into a cold evening, but it is slow and clammy. Timber and
 *     thatch insulate better per block and warm up quickly, but they do not damp the daily swing.</li>
 *     <li><strong>What is under your feet?</strong> A dirt floor conducts your body heat straight into the ground and
 *     wicks damp up into the room. A raised plank floor over an air gap does neither, which is exactly why real
 *     cold-climate buildings have one.</li>
 * </ol>
 * The survey is a bounded flood fill with a hard cap, run rarely, so a large hall costs no more than a small one.
 *
 * @param enclosure    how enclosed the space is, in {@code [0, 1]}
 * @param sealing      how few gaps the shell has, in {@code [0, 1]}
 * @param insulation   insulating quality of the shell materials, in {@code [0, 1]}
 * @param thermalMass  how strongly the structure damps the outdoor swing, in {@code [0, 1]}
 * @param floorWarmth  contribution of the floor underfoot, in {@code [-1, 1]}; negative is a cold, damp floor
 * @param heatSources  heating power of nearby fires, hearths, and stoves, in arbitrary flux-equivalent units
 */
public record Shelter(
    float enclosure,
    float sealing,
    float insulation,
    float thermalMass,
    float floorWarmth,
    float heatSources
) {
    /** Standing in the open. No shell, no floor benefit, no fire. */
    public static final Shelter OUTDOORS = new Shelter(0f, 0f, 0f, 0f, 0f, 0f);

    /** Primitive layered membranes that stop weather despite not occupying a full cubic metre. */
    private static final TagKey<Block> BARK_INSULATION = TagKey.create(Registries.BLOCK,
        com.terravera.TerraVera.identifier("insulation/bark"));

    /** How many blocks the interior flood fill will visit before giving up and calling the space "outdoors". */
    private static final int MAX_INTERIOR = 220;
    /** How far a fire can be felt. */
    private static final int HEAT_RADIUS = 6;

    /**
     * Surveys the space around {@code pos}.
     * <p>
     * The flood fill runs through air and other passable blocks. If it escapes to open sky, or runs past its budget,
     * the player is outdoors and everything else is skipped - which is the common case and therefore the cheap one.
     */
    public static Shelter survey(Level level, BlockPos pos)
    {
        final java.util.Set<Long> interior = new java.util.HashSet<>();
        final java.util.ArrayDeque<BlockPos> frontier = new java.util.ArrayDeque<>();
        final java.util.List<BlockPos> shell = new java.util.ArrayList<>();

        frontier.add(pos);
        interior.add(pos.asLong());

        boolean escaped = false;
        while (!frontier.isEmpty() && interior.size() <= MAX_INTERIOR)
        {
            final BlockPos current = frontier.removeFirst();
            for (Direction direction : Direction.values())
            {
                final BlockPos next = current.relative(direction);
                if (interior.contains(next.asLong())) continue;
                if (!level.hasChunkAt(next)) continue;

                final BlockState state = level.getBlockState(next);
                if (isShell(level, next, state))
                {
                    shell.add(next.immutable());
                    continue;
                }

                // Open sky directly above an interior block means this is not an interior at all.
                if (direction == Direction.UP && level.canSeeSky(next))
                {
                    escaped = true;
                }
                interior.add(next.asLong());
                frontier.add(next.immutable());
            }
            if (escaped) break;
        }

        if (escaped || interior.size() > MAX_INTERIOR)
        {
            // Not a room. It may still have a fire in it, though - a campfire under the stars is the whole early
            // game, and refusing to count it because there are no walls would be perverse.
            return new Shelter(0f, 0f, 0f, 0f, floorWarmth(level, pos), heatSources(level, pos));
        }

        // A well-built room's shell is large relative to its volume and made of good material. These ratios are the
        // "did you actually finish the building" measure.
        final int volume = interior.size();
        final float expectedShell = 2f * (float) Math.pow(volume, 2f / 3f) * 3f + 6f;
        final float sealing = Mth.clamp(shell.size() / Math.max(1f, expectedShell), 0f, 1f);
        final float enclosure = Mth.clamp(0.45f + 0.55f * sealing, 0f, 1f);

        float insulationSum = 0f;
        float massSum = 0f;
        for (BlockPos wall : shell)
        {
            final BlockState state = level.getBlockState(wall);
            insulationSum += shellInsulation(state);
            massSum += shellMass(state);
        }
        final float shellCount = Math.max(1f, shell.size());

        return new Shelter(
            enclosure,
            sealing,
            Mth.clamp(insulationSum / shellCount, 0f, 1f),
            Mth.clamp(massSum / shellCount, 0f, 1f),
            floorWarmth(level, pos),
            heatSources(level, pos)
        );
    }

    /** @return {@code true} if this block is part of the building's shell rather than part of its interior. */
    private static boolean isShell(Level level, BlockPos pos, BlockState state)
    {
        if (state.isAir()) return false;
        // A closed door is a wall. An open one is a hole, and the sealing term should notice: leaving the door open
        // in a blizzard ought to cost you, and it does, because the fill escapes straight through it.
        if (state.is(BlockTags.DOORS) || state.is(BlockTags.TRAPDOORS) || state.is(BlockTags.FENCE_GATES))
        {
            return state.hasProperty(BlockStateProperties.OPEN) && !state.getValue(BlockStateProperties.OPEN);
        }
        // Layered bark is a thin membrane but still stops rain and air movement; requiring a full cube here would make
        // a visibly complete primitive roof behave like open sky.
        if (state.is(BARK_INSULATION)) return true;
        return state.isFaceSturdy(level, pos, Direction.UP) || state.isCollisionShapeFullBlock(level, pos);
    }

    /**
     * How well one shell block insulates. Thatch and wool are excellent, timber is good, stone is poor - which is the
     * inverse of how strong they are, and is exactly the tension that makes building interesting.
     */
    private static float shellInsulation(BlockState state)
    {
        if (state.is(BARK_INSULATION)) return 0.8f;
        if (state.is(BlockTags.WOOL) || state.is(BlockTags.WOOL_CARPETS)) return 1.0f;
        if (state.is(BlockTags.LEAVES)) return 0.85f;
        if (state.is(BlockTags.PLANKS) || state.is(BlockTags.LOGS)) return 0.6f;
        if (state.is(BlockTags.DIRT) || state.is(Blocks.PACKED_MUD) || state.is(Blocks.MUD_BRICKS)) return 0.5f;
        if (state.is(BlockTags.WOODEN_DOORS) || state.is(BlockTags.WOODEN_TRAPDOORS)) return 0.5f;
        if (state.is(BlockTags.SAND) || state.is(Blocks.SNOW_BLOCK)) return 0.55f;
        if (state.is(BlockTags.IMPERMEABLE)) return 0.05f;   // glass: a window is a hole you can see through
        if (state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.STONE_BRICKS)) return 0.2f;
        return 0.3f;
    }

    /**
     * Thermal mass: how much this material lags behind the outside air. Stone and earth are heavy and slow, timber
     * and thatch are light and fast.
     */
    private static float shellMass(BlockState state)
    {
        if (state.is(BARK_INSULATION)) return 0.1f;
        if (state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.STONE_BRICKS)) return 1.0f;
        if (state.is(Blocks.PACKED_MUD) || state.is(Blocks.MUD_BRICKS) || state.is(BlockTags.DIRT)) return 0.85f;
        if (state.is(BlockTags.LOGS)) return 0.45f;
        if (state.is(BlockTags.PLANKS)) return 0.3f;
        if (state.is(BlockTags.WOOL) || state.is(BlockTags.LEAVES)) return 0.1f;
        return 0.5f;
    }

    /**
     * The floor. Bare earth conducts your heat away and stays damp; a raised plank floor with a void under it does
     * neither. This is a small number by design - it is the difference between a good hut and a great one, not
     * between life and death.
     */
    private static float floorWarmth(Level level, BlockPos pos)
    {
        final BlockPos below = pos.below();
        final BlockState floor = level.getBlockState(below);
        if (floor.isAir()) return 0f;

        float warmth;
        if (floor.is(BlockTags.WOOL_CARPETS) || floor.is(BlockTags.WOOL)) warmth = 0.9f;
        else if (floor.is(BlockTags.PLANKS) || floor.is(BlockTags.WOODEN_SLABS)) warmth = 0.55f;
        else if (floor.is(BlockTags.LOGS)) warmth = 0.4f;
        else if (floor.is(BlockTags.BASE_STONE_OVERWORLD) || floor.is(BlockTags.STONE_BRICKS)) warmth = -0.25f;
        else if (floor.is(BlockTags.DIRT) || floor.is(Blocks.MUD) || floor.is(Blocks.CLAY)) warmth = -0.6f;
        else if (floor.is(BlockTags.SAND) || floor.is(BlockTags.SNOW)) warmth = -0.5f;
        else warmth = 0f;

        // A raised floor with an air gap under it is dramatically better than the same planks laid on the soil,
        // because it is the ground contact, not the timber, that costs you the heat.
        if (warmth > 0f && level.getBlockState(below.below()).isAir())
        {
            warmth = Math.min(1f, warmth + 0.35f);
        }
        return warmth;
    }

    /**
     * Fires, hearths, and stoves. Heat falls off with distance and is blocked by line of sight, so a stove in the next
     * room does not warm this one and standing right beside the fire is meaningfully better than standing across the
     * hall from it.
     */
    private static float heatSources(Level level, BlockPos origin)
    {
        float total = 0f;
        final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int dx = -HEAT_RADIUS; dx <= HEAT_RADIUS; dx++)
        {
            for (int dy = -2; dy <= 3; dy++)
            {
                for (int dz = -HEAT_RADIUS; dz <= HEAT_RADIUS; dz++)
                {
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (!level.hasChunkAt(cursor)) continue;

                    final float output = heatOutput(level.getBlockState(cursor));
                    if (output <= 0f) continue;

                    final float distanceSq = dx * dx + dy * dy + dz * dz;
                    // Inverse-square, floored so that standing in the fire is not infinitely warm.
                    total += output / Math.max(1.6f, distanceSq * 0.25f);
                }
            }
        }
        return total;
    }

    /** Heat output of a single block. Bigger, enclosed fires are worth more than a torch. */
    private static float heatOutput(BlockState state)
    {
        final Block block = state.getBlock();
        if (state.is(Blocks.LAVA)) return 26f;
        if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) return 9f;
        if (state.is(Blocks.MAGMA_BLOCK)) return 5f;

        // Lit-ness is a common blockstate on campfires, furnaces, and (via TFC) forges, firepits, and charcoal
        // forges. Reading the property by name keeps this working for modded heat blocks without hard-coding them.
        final boolean lit = state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT);
        if (!lit)
        {
            return state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH) ? 0.8f : 0f;
        }

        final String name = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).getPath();
        if (name.contains("forge") || name.contains("bloomery") || name.contains("blast")) return 20f;
        if (name.contains("firepit") || name.contains("campfire") || name.contains("hearth")) return 12f;
        if (name.contains("furnace") || name.contains("stove") || name.contains("kiln") || name.contains("oven")) return 10f;
        if (name.contains("candle") || name.contains("lamp") || name.contains("lantern")) return 1.2f;
        return 6f;
    }

    // ----- What the shelter does to the player -------------------------------------------------------------------

    /**
     * Pulls the felt temperature towards a comfortable indoor value.
     * <p>
     * Thermal mass makes the building lag the outdoors rather than track it, which is what makes a stone house cool
     * in summer and a timber one quick to heat in winter. Insulation and enclosure decide how much of that lag the
     * player actually feels.
     *
     * @param outdoorC     the felt temperature outside
     * @param averageC     the local annual average, which is what a heavy structure settles towards
     * @return the felt temperature inside
     */
    public float moderate(float outdoorC, float averageC)
    {
        if (enclosure <= 0f) return outdoorC;

        // Heavy structures drift towards the annual mean instead of the current air temperature.
        final float lagged = Mth.lerp(thermalMass * enclosure * 0.75f, outdoorC, averageC);

        // Enclosure plus insulation is what keeps body heat and fire heat in the room.
        final float retention = enclosure * (0.35f + 0.65f * insulation) * sealing;
        float inside = Mth.lerp(retention * 0.6f, lagged, ThermalModel.NEUTRAL_CORE - 15f);

        // Fires warm the room they are in far more effectively than they warm open ground.
        inside += heatSources * (0.35f + 0.65f * enclosure * sealing) * 0.5f;

        // A cold floor is a real, constant heat drain, and a warm one is a real comfort.
        inside += floorWarmth * 2.2f;
        return inside;
    }

    /** Fires warm you directly even with no building around them, but far less efficiently. */
    public float openFireWarmth()
    {
        return heatSources * 0.5f;
    }

    /** How much of the wind this shelter keeps off you. */
    public float windShelter()
    {
        return Mth.clamp(enclosure * sealing, 0f, 1f);
    }

    /** How draughty the building is, for the tooltip and the field notes. */
    public boolean isDraughty()
    {
        return enclosure > 0f && sealing < 0.65f;
    }

    public boolean isIndoors()
    {
        return enclosure > 0.25f;
    }

    public boolean hasFire()
    {
        return heatSources > 1.5f;
    }
}
