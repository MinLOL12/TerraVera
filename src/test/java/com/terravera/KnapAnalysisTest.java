/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera;

import java.util.List;
import org.junit.jupiter.api.Test;

import com.terravera.common.knapping.HeadProfile;
import com.terravera.common.knapping.KnapAnalysis;
import com.terravera.common.knapping.KnapGrid;
import com.terravera.common.knapping.KnapMetrics;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the knapping geometry engine. These are pure - no Minecraft, no registries - which is the reason
 * {@link KnapGrid} exists as its own type rather than reusing TFC's {@code KnappingPattern}.
 * <p>
 * The profiles here mirror the ones shipped in {@code data/terravera/terravera/head_profile/}. If those are retuned,
 * these should be updated in lockstep - that is deliberate, since the classification behaviour is the mod.
 */
public class KnapAnalysisTest
{
    private static final HeadProfile BLADE = new HeadProfile(
        new HeadProfile.Base(1, 2, 0.75f, 0.55f),
        new HeadProfile.Tip(1, 2, 1, 99, 0, 99, 0.75f),
        new HeadProfile.Body(5, 14, 1.9f, 99f, 0f, true), 50);

    private static final HeadProfile POINT = new HeadProfile(
        new HeadProfile.Base(3, 2, 0.6f, 0.55f),
        new HeadProfile.Tip(1, 1, 1, 99, 0, 99, 0.7f),
        new HeadProfile.Body(8, 20, 0.8f, 1.9f, 0.6f, true), 40);

    private static final HeadProfile WEDGE = new HeadProfile(
        new HeadProfile.Base(3, 2, 0.6f, 0.55f),
        new HeadProfile.Tip(2, 3, 1, 3, 0, 99, 0.55f),
        new HeadProfile.Body(10, 22, 0.7f, 1.9f, 0f, true), 30);

    private static final HeadProfile BROAD = new HeadProfile(
        new HeadProfile.Base(4, 2, 0.8f, 0.8f),
        new HeadProfile.Tip(3, 5, 0, 1, 1, 99, 0.6f),
        new HeadProfile.Body(9, 24, 0.3f, 1.4f, 0f, true), 20);

    private static final HeadProfile MAUL = new HeadProfile(
        new HeadProfile.Base(4, 3, 0.92f, 1.0f),
        new HeadProfile.Tip(4, 5, 0, 0, 0, 0, 0f),
        new HeadProfile.Body(14, 24, 0.5f, 1.6f, 0f, true), 60);

    private static final List<KnapAnalysis.Ranked.Candidate> ALL = List.of(
        new KnapAnalysis.Ranked.Candidate(BLADE, "blade"),
        new KnapAnalysis.Ranked.Candidate(POINT, "point"),
        new KnapAnalysis.Ranked.Candidate(WEDGE, "wedge"),
        new KnapAnalysis.Ranked.Candidate(BROAD, "broad"),
        new KnapAnalysis.Ranked.Candidate(MAUL, "maul"));

    /** @return the kind produced by the given grid, or {@code null} if it is not a usable head */
    private static String classify(String... rows)
    {
        final List<KnapAnalysis.Ranked> ranked = KnapAnalysis.rank(KnapGrid.of(rows), ALL);
        final KnapAnalysis.Ranked best = ranked.getFirst();
        return best.outcome().success() ? (String) best.candidate().owner() : null;
    }

    private static String reason(String... rows)
    {
        return KnapAnalysis.rank(KnapGrid.of(rows), ALL).getFirst().outcome().reason();
    }

    // ----- The headline claim: shape class, not exact silhouette --------------------------------------------

    @Test
    public void severalDifferentShapesAllProduceAWedge()
    {
        // The whole point of the mod. None of these are the same picture, and every one of them is an axe bit:
        // a sturdy base, with mass behind a short 2-3 wide splitting edge.
        assertEquals("wedge", classify(
            " ##  ",
            "#### ",
            "#####",
            "#####"));
        assertEquals("wedge", classify(
            "  ## ",
            " ####",
            "#####",
            "#####"));
        assertEquals("wedge", classify(
            " ##  ",
            " ### ",
            "#####",
            "#####"));
        assertEquals("wedge", classify(
            "  ###",
            " ####",
            "#####",
            "#####"));
    }

    @Test
    public void terraFirmaCraftsOwnPatternsStillProduceUsableHeads()
    {
        // Players coming from TerraFirmaCraft should not be punished for muscle memory. TFC's stock patterns
        // are all still valid pieces - they simply are not the only valid pieces any more, and TFC's axe
        // pattern is fine enough at the tip that TerraVera reads it as a point rather than a wedge.
        assertNotNull(classify(
            " #   ",
            "#### ",
            "#####",
            "#### ",
            " #   "), "TFC axe head pattern");
        assertNotNull(classify(
            "###  ",
            "#### ",
            "#####",
            " ### ",
            "  #  "), "TFC javelin head pattern");
        assertEquals("blade", classify(
            "# ",
            "##",
            "##",
            "##",
            "##"), "TFC knife head pattern");
    }

    @Test
    public void orientationDoesNotMatter()
    {
        // The same piece, knapped pointing up and pointing down. A knapped stone has no inherent "up",
        // so the analysis is run in all four quarter turns and the best orientation is kept.
        assertEquals(classify(
            " ##  ",
            "#### ",
            "#####",
            "#####"), classify(
            "#####",
            "#####",
            "#### ",
            " ##  "));
    }

    // ----- Each kind is reachable and distinct --------------------------------------------------------------

    @Test
    public void eachHeadKindIsReachable()
    {
        // Every one of the five heads must be obtainable, or the profile set has a hole in it.
        assertEquals("blade", classify(
            "#  ",
            "## ",
            "## ",
            "## ",
            "## "));
        assertEquals("point", classify(
            "  #  ",
            "  #  ",
            " ### ",
            " ### ",
            " ### "));
        assertEquals("wedge", classify(
            " ##  ",
            "#### ",
            "#####",
            "#####"));
        assertEquals("broad", classify(
            "#####",
            "#####",
            "#####",
            "#####",
            " ### "));
        assertEquals("maul", classify(
            "#####",
            "#####",
            "#####",
            "#####"));
    }

    @Test
    public void aMaulIsDefinedByHavingNoWorkingEndAtAll()
    {
        // A maul must NOT taper. "No working end" is the requirement, which is what separates a hammer
        // stone from every other head in the mod - it is the one profile defined by an absence.
        assertEquals("maul", classify(
            "#####",
            "#####",
            "#####",
            "#####"));
        // Flake any kind of edge onto it and it stops being a maul.
        assertNotEquals("maul", classify(
            "  #  ",
            " ### ",
            "#####",
            "#####",
            "#####"));
    }

    // ----- The two hard requirements: a sturdy base and a strong tip ----------------------------------------

    @Test
    public void aBroadHeadOnASpindlyStalkIsRejectedAsABroadHead()
    {
        // This is the min_base_ratio rule: the base must be a real fraction of the widest part of the piece.
        // The head here is impeccable, but it balances on a single square and would snap off at the neck.
        // Asserted against the broad profile specifically, since the analysis is free to rotate the piece and
        // read the stalk as the tip of some other, perfectly valid head.
        final HeadProfile.Result result = KnapAnalysis.analyse(KnapGrid.of(
            "#####",
            "#####",
            "  #  ",
            "  #  "), BROAD).result();
        assertFalse(result.success());
        assertEquals("base_too_narrow", result.reason());
    }

    @Test
    public void aNotchedBaseIsRejected()
    {
        assertEquals("base_too_narrow", reason(
            "  #  ",
            " ### ",
            "#####",
            "# # #"));
    }

    @Test
    public void aBroadHeadWillNotAcceptANeedleTip()
    {
        // A shovel edge has to be wide. A single-square tip would shear off on the first spadeful,
        // so the broad profile rejects it even though the base is impeccable.
        final HeadProfile.Result result = KnapAnalysis.analyse(KnapGrid.of(
            "  #  ",
            " ### ",
            " ### ",
            " ### "), BROAD).result();
        assertFalse(result.success());
    }

    // ----- Degenerate inputs --------------------------------------------------------------------------------

    @Test
    public void anUntouchedCobbleIsNotATool()
    {
        assertNull(classify(
            "#####",
            "#####",
            "#####",
            "#####",
            "#####"));
        assertEquals("too_much_stone", reason(
            "#####",
            "#####",
            "#####",
            "#####",
            "#####"));
    }

    @Test
    public void aSingleChipIsNotATool()
    {
        assertEquals("too_little_stone", reason("#"));
    }

    @Test
    public void twoSeparateFlakesAreNotOneHead()
    {
        assertEquals("shattered", reason(
            "## ##",
            "## ##",
            "     ",
            "     ",
            "     "));
    }

    @Test
    public void anEmptyGridProducesNothing()
    {
        assertNull(classify(
            "     ",
            "     ",
            "     ",
            "     ",
            "     "));
    }

    // ----- Quality ------------------------------------------------------------------------------------------

    @Test
    public void aBetterWorkedPieceScoresHigherQuality()
    {
        // Quality feeds straight into the durability of the finished tool, so it has to actually track
        // how well the piece was worked rather than just being a pass/fail rebadged.
        final float sloppy = KnapAnalysis.analyse(KnapGrid.of(
            " ##  ",
            "#### ",
            "#### ",
            "#####"), WEDGE).quality();
        final float clean = KnapAnalysis.analyse(KnapGrid.of(
            " ##  ",
            "#### ",
            "#####",
            "#####"), WEDGE).quality();
        assertTrue(clean > sloppy, "a more solid base should score better: " + clean + " vs " + sloppy);
        assertTrue(sloppy >= 0f);
        assertTrue(clean <= 1f);
    }

    // ----- Metrics ------------------------------------------------------------------------------------------

    @Test
    public void metricsMeasureTheObviousThings()
    {
        final KnapMetrics m = KnapMetrics.measure(KnapGrid.of(
            "  #  ",
            " ### ",
            "#####"));
        assertNotNull(m);
        assertEquals(9, m.mass());
        assertEquals(5, m.baseWidth());
        assertEquals(1, m.tipWidth());
        assertEquals(5, m.widestRun());
        assertTrue(m.connected());
        assertEquals(1f, m.symmetry(), 0.001f);
    }

    @Test
    public void aOneWideBaseDoesNotSwallowTheWholePiece()
    {
        // Regression: with a base 1 wide, an unguarded "width - 1" threshold makes every row qualify,
        // reporting a 5-deep base for a piece that balances on a single square.
        final KnapMetrics m = KnapMetrics.measure(KnapGrid.of(
            "#####",
            "#####",
            "  #  "));
        assertNotNull(m);
        assertEquals(1, m.baseWidth());
        assertEquals(1, m.baseDepth(), "a 1-wide base is 1 deep, not the height of the piece");
    }

    @Test
    public void trimmingRemovesEmptyMargins()
    {
        final KnapGrid trimmed = KnapGrid.of(
            "     ",
            " ##  ",
            " ##  ",
            "     ").trimmed();
        assertNotNull(trimmed);
        assertEquals(2, trimmed.width());
        assertEquals(2, trimmed.height());
    }

    @Test
    public void rotatingFourTimesReturnsTheOriginal()
    {
        final KnapGrid grid = KnapGrid.of(
            "##  ",
            "#   ",
            "#   ");
        assertEquals(grid, grid.rotate(4));
    }
}
