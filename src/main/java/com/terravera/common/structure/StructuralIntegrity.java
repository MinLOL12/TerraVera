/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.structure;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import com.terravera.TerraVera;
import com.terravera.common.blocks.SupportBeamBlock;
import com.terravera.common.blocks.TerraVeraBlocks;
import com.terravera.common.skill.SkillSystem;
import com.terravera.common.skill.SkillType;
import com.terravera.config.TerraVeraConfig;

/**
 * A deliberately bounded structural model for player-built construction.
 * <p>
 * It is not a voxel-by-voxel physics solver. Instead it answers the questions builders actually need to learn:
 * heavy masonry needs a footing and a continuous compressive path; a roof needs a beam below it; a beam needs posts
 * below both ends; timber spans are short and forgiving while metal spans and posts carry far more load. Invalid work
 * is given a short grace period before failure, so a player can place a roof member and immediately brace it without
 * turning construction into a placement-order puzzle.
 * <p>
 * The persistent ledger contains blocks placed by players after TerraVera is installed. Natural cliffs and generated
 * ruins are intentionally never treated as failed player buildings. Existing worlds begin tracking new work as it is
 * altered, which prevents terrain from becoming an accidental physics puzzle.
 */
public final class StructuralIntegrity
{
    private static final int FAILURE_DELAY = 50;
    private static final int MAX_COLUMN_HEIGHT = 20;
    private static final int WOOD_SPAN = 4;
    private static final int METAL_SPAN = 8;

    private static final TagKey<Block> MASONRY = tag("structural/masonry");
    private static final TagKey<Block> WOOD = tag("structural/wood");
    private static final TagKey<Block> ROOF = tag("structural/roof");
    private static final TagKey<Block> FOUNDATION = tag("structural/foundation");

    /** Positions pending their grace-period verification, keyed by world game time. */
    private static final Map<ServerLevel, Map<Long, Long>> PENDING = new WeakHashMap<>();
    private static final String LEDGER_ID = "terravera_structural_members";

    public static void onPlaced(BlockEvent.EntityPlaceEvent event)
    {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof Player player)) return;
        if (!TerraVeraConfig.SERVER.enableStructuralIntegrity.get() || player.isCreative()) return;

        final BlockPos pos = event.getPos();
        final BlockState state = level.getBlockState(pos);
        if (!isStructural(state)) return;

        final float builderKnowledge = SkillSystem.proficiency(player, SkillType.BUILDING);
        ledger(level).add(pos.asLong(), builderKnowledge);
        queue(level, pos, FAILURE_DELAY);
        queueNeighbours(level, pos, FAILURE_DELAY);
        SkillSystem.award(player, SkillType.BUILDING, isBeam(state) ? 1.0f : 0.28f);

        if (!isSound(level, pos, builderKnowledge))
        {
            player.displayClientMessage(Component.translatable("terravera.structural.warning").withStyle(ChatFormatting.GOLD), true);
        }
    }

    /** Called from the normal break event after the source position has been identified. */
    public static void onBroken(Level level, BlockPos pos)
    {
        if (!(level instanceof ServerLevel serverLevel) || !TerraVeraConfig.SERVER.enableStructuralIntegrity.get()) return;
        queueNeighbours(serverLevel, pos, 4);
    }

    /** Processes a small, time-delayed recheck queue. No chunk is loaded just to collapse it. */
    public static void onLevelTick(LevelTickEvent.Post event)
    {
        if (!(event.getLevel() instanceof ServerLevel level) || !TerraVeraConfig.SERVER.enableStructuralIntegrity.get()) return;
        final Map<Long, Long> pending = PENDING.get(level);
        if (pending == null || pending.isEmpty()) return;

        final long now = level.getGameTime();
        final ArrayDeque<Long> due = new ArrayDeque<>();
        for (var entry : pending.entrySet())
        {
            if (entry.getValue() <= now) due.add(entry.getKey());
        }

        // A hard cap keeps a deliberately bad megastructure from monopolising one server tick.
        int checked = 0;
        while (!due.isEmpty() && checked++ < 96)
        {
            final long packed = due.removeFirst();
            pending.remove(packed);
            final BlockPos pos = BlockPos.of(packed);
            if (!isBuilt(level, pos) || !level.hasChunkAt(pos)) continue;

            final BlockState state = level.getBlockState(pos);
            if (!isStructural(state))
            {
                untrack(level, pos);
                continue;
            }

            if (!isSound(level, pos, ledger(level).knowledge(packed)))
            {
                // Destroying with drops represents joints pulling apart or masonry shedding. The normal delay and
                // cascading neighbour queue give the visual/mechanical result of a failure without simulating every
                // individual stone as a falling entity.
                level.levelEvent(2001, pos, Block.getId(state));
                level.destroyBlock(pos, true);
                untrack(level, pos);
                queueNeighbours(level, pos, 3);
            }
        }
    }

    /** Exposed for tests and integration: reports whether this position has a load path at the supplied skill. */
    public static boolean isSound(Level level, BlockPos pos, float buildingKnowledge)
    {
        final BlockState state = level.getBlockState(pos);
        final Profile profile = profile(state);
        if (profile == Profile.NONE) return true;

        // A footing is the bottom of the load path, not another member that needs one. It only has to be sitting on
        // something - ground, rock, or any solid block - rather than hanging in the air. Requiring a foundation to
        // itself be "supported" was what made laid stone footings shed themselves the moment they were placed.
        if (profile == Profile.FOUNDATION) return bearsOnSomething(level, pos);

        if (isBeam(state))
        {
            return state.getValue(SupportBeamBlock.AXIS) == Axis.Y
                ? verticalBeamAnchored(level, pos)
                : horizontalBeamBraced(level, pos, profile.span(buildingKnowledge));
        }

        final BlockPos below = pos.below();
        final BlockState support = level.getBlockState(below);
        final int load = verticalLoad(level, pos);
        final int capacity = capacityOf(level, below, support, buildingKnowledge);

        // A roof has to sit on a beam. Masonry and timber walls can bear directly on a sound footing or an already
        // loaded wall below, but not hover next to one.
        if (profile.roof)
        {
            return isBeam(support) && capacity >= roofLoad(level, pos);
        }
        return capacity >= load;
    }

    /** Weight/strength profiles are data-tag driven, so packs can classify their own TFC rocks, bricks, and lumber. */
    public static Profile profile(BlockState state)
    {
        if (state.is(TerraVeraBlocks.RUBBLE_FOUNDATION.get())) return Profile.FOUNDATION;
        if (state.is(TerraVeraBlocks.WOODEN_SUPPORT_BEAM.get())) return Profile.WOOD_BEAM;
        if (state.is(TerraVeraBlocks.WROUGHT_IRON_SUPPORT_BEAM.get())) return Profile.METAL_BEAM;
        if (state.is(ROOF)) return Profile.ROOF;
        if (state.is(MASONRY)) return Profile.MASONRY;
        if (state.is(WOOD)) return Profile.TIMBER;
        return Profile.NONE;
    }

    private static int capacityOf(Level level, BlockPos pos, BlockState state, float knowledge)
    {
        final Profile profile = profile(state);
        if (profile == Profile.FOUNDATION || state.is(FOUNDATION)) return Profile.FOUNDATION.strength;
        if (profile == Profile.WOOD_BEAM || profile == Profile.METAL_BEAM)
        {
            if (state.getValue(SupportBeamBlock.AXIS) == Axis.Y)
            {
                return verticalBeamAnchored(level, pos) ? profile.strength : 0;
            }
            return horizontalBeamBraced(level, pos, profile.span(memberKnowledge(level, pos, knowledge))) ? profile.strength : 0;
        }

        // A framed wall can transfer load into an adjacent upright. This is the deliberately simple reinforcement
        // rule: a post beside a course is valuable, while a decorative beam somewhere in the room is not.
        int capacity = profile.strength;
        for (Direction direction : Direction.Plane.HORIZONTAL)
        {
            final BlockPos reinforcedBy = pos.relative(direction);
            final BlockState beam = level.getBlockState(reinforcedBy);
            if (isVerticalBeam(beam) && verticalBeamAnchored(level, reinforcedBy))
            {
                capacity = Math.max(capacity, profile(beam).strength);
            }
        }
        return capacity;
    }

    /**
     * A post is anchored when the bottom of its column lands on something that can actually take the load: a
     * foundation, tagged ground or rock, masonry, timber, or simply any block with a solid top face.
     * <p>
     * The earlier rule accepted <em>only</em> the foundation tag, which meant a perfectly sensible post standing on a
     * plank floor, a log, or a rubble course two blocks thick was treated as floating and collapsed underneath the
     * builder. Posts are the mod's answer to "how do I hold this up"; they must not be the hardest thing to place.
     */
    private static boolean verticalBeamAnchored(Level level, BlockPos pos)
    {
        BlockPos cursor = pos;
        int length = 0;
        while (length++ < MAX_COLUMN_HEIGHT)
        {
            final BlockState state = level.getBlockState(cursor);
            if (!isVerticalBeam(state)) break;
            cursor = cursor.below();
        }
        return bearing(level, cursor);
    }

    /** @return {@code true} if this block can carry a column: footing, ground, masonry, timber, or a solid face. */
    private static boolean bearing(Level level, BlockPos pos)
    {
        final BlockState state = level.getBlockState(pos);
        if (state.is(FOUNDATION)) return true;
        final Profile profile = profile(state);
        if (profile == Profile.FOUNDATION || profile == Profile.MASONRY || profile == Profile.TIMBER) return true;
        // A beam can carry another beam - a post landing on a lintel or a sill plate is ordinary framing.
        if (profile.beam) return true;
        return state.isFaceSturdy(level, pos, Direction.UP);
    }

    /** A footing only has to sit on the ground. It is the bottom of the load path, not another loaded member. */
    private static boolean bearsOnSomething(Level level, BlockPos pos)
    {
        final BlockPos below = pos.below();
        final BlockState state = level.getBlockState(below);
        if (state.isAir()) return false;
        // Anything not obviously insubstantial will do. Rubble is laid straight onto grass, sand, gravel, or rock.
        return state.is(FOUNDATION)
            || profile(state) != Profile.NONE
            || state.isFaceSturdy(level, below, Direction.UP)
            || !state.getCollisionShape(level, below).isEmpty();
    }

    /**
     * A lintel/purlin needs a support under both ends within the material's safe span.
     * <p>
     * "Support" is an anchored post, but also a wall or footing the beam is simply resting on or built into - which is
     * how a lintel over a doorway or a purlin bedded into a gable actually works. A beam resting directly on the
     * ground is likewise fine; it is a sill, not a cantilever.
     */
    private static boolean horizontalBeamBraced(Level level, BlockPos pos, int span)
    {
        if (bearing(level, pos.below())) return true; // a sill beam laid straight onto a wall head or the ground

        final Axis axis = level.getBlockState(pos).getValue(SupportBeamBlock.AXIS);
        final Direction negative = axis == Axis.X ? Direction.WEST : Direction.NORTH;
        final Direction positive = axis == Axis.X ? Direction.EAST : Direction.SOUTH;
        return reachesPost(level, pos, negative, span) && reachesPost(level, pos, positive, span);
    }

    private static boolean reachesPost(Level level, BlockPos pos, Direction direction, int span)
    {
        for (int distance = 0; distance <= span; distance++)
        {
            final BlockPos along = pos.relative(direction, distance);
            // Posts meet a horizontal beam from below, rather than occupying its same block.
            if (isVerticalBeam(level.getBlockState(along.below())) && verticalBeamAnchored(level, along.below())) return true;
            // A beam bedded into a wall, or landing on a footing, is supported at that end.
            if (distance > 0)
            {
                final Profile inline = profile(level.getBlockState(along));
                if (inline == Profile.MASONRY || inline == Profile.TIMBER || inline == Profile.FOUNDATION) return true;
                if (bearing(level, along.below())) return true;
            }
        }
        return false;
    }

    private static int verticalLoad(Level level, BlockPos base)
    {
        int total = 0;
        for (int dy = 0; dy < MAX_COLUMN_HEIGHT; dy++)
        {
            final Profile profile = profile(level.getBlockState(base.above(dy)));
            if (profile == Profile.NONE || profile.beam) break;
            total += profile.weight;
        }
        return total;
    }

    private static int roofLoad(Level level, BlockPos roof)
    {
        int total = 0;
        for (int dy = 0; dy < 4; dy++)
        {
            final Profile profile = profile(level.getBlockState(roof.above(dy)));
            if (profile == Profile.NONE) break;
            total += profile.weight;
        }
        return total;
    }

    private static boolean isStructural(BlockState state)
    {
        return profile(state) != Profile.NONE;
    }

    private static boolean isBeam(BlockState state)
    {
        return profile(state).beam;
    }

    private static boolean isVerticalBeam(BlockState state)
    {
        return isBeam(state) && state.getValue(SupportBeamBlock.AXIS) == Axis.Y;
    }

    private static void queueNeighbours(ServerLevel level, BlockPos origin, int delay)
    {
        queue(level, origin.above(), delay);
        for (int dy = 0; dy <= 10; dy++)
        {
            final BlockPos atHeight = origin.above(dy);
            queue(level, atHeight, delay);
            // Recheck the load-bearing members below a newly placed course as well. Without this, a wall could be
            // extended forever because only its newest top block would ever be inspected.
            queue(level, origin.below(dy), delay);
            for (Direction direction : Direction.Plane.HORIZONTAL)
            {
                queue(level, atHeight.relative(direction), delay);
                queue(level, origin.below(dy).relative(direction), delay);
            }
        }
    }

    private static void queue(ServerLevel level, BlockPos pos, int delay)
    {
        if (!isBuilt(level, pos)) return;
        PENDING.computeIfAbsent(level, ignored -> new HashMap<>())
            .merge(pos.asLong(), level.getGameTime() + delay, Math::min);
    }

    private static float memberKnowledge(Level level, BlockPos pos, float fallback)
    {
        return level instanceof ServerLevel serverLevel ? ledger(serverLevel).knowledge(pos.asLong()) : fallback;
    }

    private static boolean isBuilt(ServerLevel level, BlockPos pos)
    {
        return ledger(level).contains(pos.asLong());
    }

    private static void untrack(ServerLevel level, BlockPos pos)
    {
        ledger(level).remove(pos.asLong());
    }

    private static StructuralLedger ledger(ServerLevel level)
    {
        return level.getDataStorage().computeIfAbsent(StructuralLedger.FACTORY, LEDGER_ID);
    }

    private static TagKey<Block> tag(String path)
    {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(TerraVera.MOD_ID, path));
    }

    /** Compact parameters make the balance visible and testable, rather than burying it in a giant physics solver. */
    public enum Profile
    {
        NONE(0, 0, false, false, 0),
        TIMBER(5, 38, false, false, 0),
        MASONRY(13, 112, false, false, 0),
        ROOF(8, 18, false, true, 0),
        FOUNDATION(18, 320, false, false, 0),
        WOOD_BEAM(4, 58, true, false, WOOD_SPAN),
        METAL_BEAM(10, 220, true, false, METAL_SPAN);

        private final int weight;
        private final int strength;
        private final boolean beam;
        private final boolean roof;
        private final int baseSpan;

        Profile(int weight, int strength, boolean beam, boolean roof, int baseSpan)
        {
            this.weight = weight;
            this.strength = strength;
            this.beam = beam;
            this.roof = roof;
            this.baseSpan = baseSpan;
        }

        /** Builder practice improves joint/layout efficiency by at most one block, never enough to erase materials. */
        public int span(float buildingKnowledge)
        {
            return baseSpan + (buildingKnowledge >= 0.55f && baseSpan > 0 ? 1 : 0);
        }
    }

    private StructuralIntegrity() {}
}
