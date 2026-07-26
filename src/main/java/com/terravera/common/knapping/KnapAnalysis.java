/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.knapping;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * Runs a {@link HeadProfile} against a knapped grid in every orientation, and keeps the best fit.
 * <p>
 * A knapped stone has no "up". If you flake a point at the left hand edge of the grid, that is still a point. So each
 * candidate is measured in all four quarter turns and the highest quality passing orientation wins. Failures keep the
 * <em>closest</em> near miss, so the UI can tell you which single property let you down rather than just refusing.
 */
public final class KnapAnalysis
{
    /**
     * @param grid    the current state of the knapping grid
     * @param profile the shape being tested for
     * @return the best result over all orientations
     */
    public static Outcome analyse(KnapGrid grid, HeadProfile profile)
    {
        Outcome best = null;
        for (int turn = 0; turn < 4; turn++)
        {
            final KnapMetrics metrics = KnapMetrics.measure(grid.rotate(turn));
            if (metrics == null) continue;

            final HeadProfile.Result result = profile.test(metrics);
            final Outcome outcome = new Outcome(result, metrics, turn);
            if (best == null || outcome.betterThan(best)) best = outcome;
        }
        return best != null ? best : new Outcome(HeadProfile.Result.fail("too_little_stone"), null, 0);
    }

    /**
     * Analyse against many profiles at once and return them ranked. Used by the knapping container to work out what,
     * if anything, the player has actually made - highest priority successful profile wins.
     */
    public static List<Ranked> rank(KnapGrid grid, List<Ranked.Candidate> candidates)
    {
        final List<Ranked> results = new ArrayList<>(candidates.size());
        for (Ranked.Candidate candidate : candidates)
        {
            results.add(new Ranked(candidate, analyse(grid, candidate.profile())));
        }
        results.sort((a, b) -> {
            if (a.outcome().success() != b.outcome().success()) return a.outcome().success() ? -1 : 1;
            final int byPriority = Integer.compare(b.candidate().profile().priority(), a.candidate().profile().priority());
            if (byPriority != 0) return byPriority;
            return Float.compare(b.outcome().quality(), a.outcome().quality());
        });
        return results;
    }

    public record Ranked(Candidate candidate, Outcome outcome)
    {
        /** A profile plus whatever the caller wants to associate with it (typically a recipe). */
        public record Candidate(HeadProfile profile, Object owner) {}
    }

    /**
     * @param result   pass/fail plus quality
     * @param metrics  the measurements taken in the winning orientation, {@code null} if the grid was empty
     * @param rotation the number of clockwise quarter turns applied to reach the winning orientation
     */
    public record Outcome(HeadProfile.Result result, @Nullable KnapMetrics metrics, int rotation)
    {
        public boolean success()
        {
            return result.success();
        }

        public float quality()
        {
            return result.quality();
        }

        @Nullable
        public String reason()
        {
            return result.reason();
        }

        boolean betterThan(Outcome other)
        {
            if (success() != other.success()) return success();
            if (success()) return quality() > other.quality();
            // Both failed - prefer the one that kept more stone, i.e. the one the player is closest to fixing
            final int mass = metrics == null ? 0 : metrics.mass();
            final int otherMass = other.metrics == null ? 0 : other.metrics.mass();
            return mass > otherMass;
        }
    }

    private KnapAnalysis() {}
}
