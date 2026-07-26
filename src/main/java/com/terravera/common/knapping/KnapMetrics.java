/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.knapping;

import org.jetbrains.annotations.Nullable;

/**
 * The measurements TerraVera takes of a knapped piece of stone. This is the whole point of the mod: rather than asking
 * "does this match the picture of an axe head", we ask the questions a flintknapper would actually ask -
 * <ul>
 *     <li>Is there a <strong>sturdy base</strong>? Enough contiguous, un-notched stone at one end to take the shock of
 *     a blow, or to be lashed to a haft without splitting.</li>
 *     <li>Is there a <strong>strong tip</strong>? A working end that tapers to a point or an edge, without being so
 *     thin that it snaps off on first use.</li>
 *     <li>Is the piece <strong>whole</strong>? One connected body, not two flakes sat next to each other.</li>
 * </ul>
 * All measurements are taken in the piece's own frame - the analysis is rotated four ways and the best orientation is
 * kept, so it does not matter which way up you knapped it.
 *
 * @param grid       the trimmed grid, oriented so that the base is at the bottom and the tip at the top
 * @param mass       total number of surviving squares
 * @param baseWidth  the widest run of contiguous stone in the base row
 * @param baseDepth  how many rows deep the base stays at least {@code baseWidth - 1} wide
 * @param baseSolid  fraction of the base rows that is solid stone (no internal notches)
 * @param tipWidth   width of the tip row
 * @param tipTaper   how consistently the piece narrows from base to tip, in [0, 1]
 * @param shoulder   number of rows between the base and the tip, i.e. the length of the working body
 * @param symmetry   left/right symmetry of the piece, in [0, 1]
 * @param connected  whether all remaining stone forms a single connected body
 * @param widestRun  the longest contiguous run anywhere in the piece. Compared against {@code baseWidth}, this is what
 *                   distinguishes "the base is the sturdiest part" from "the base is a spindly stalk under a big head"
 * @param edgeLength how many rows from the tip down stay 2 wide or less - the length of the worked edge. A long fine
 *                   edge is a blade or a point; a single row of it is an axe bit; none of it is a hammer
 * @param aspect     height / width of the piece
 */
public record KnapMetrics(
    KnapGrid grid,
    int mass,
    int baseWidth,
    int baseDepth,
    float baseSolid,
    int tipWidth,
    float tipTaper,
    int shoulder,
    float symmetry,
    boolean connected,
    int widestRun,
    int edgeLength,
    float aspect
) {
    /**
     * Measure a knapped grid in a single, fixed orientation (base at the bottom).
     *
     * @return the metrics, or {@code null} if nothing at all is left of the stone
     */
    @Nullable
    public static KnapMetrics measure(KnapGrid raw)
    {
        final KnapGrid grid = raw.trimmed();
        if (grid == null) return null;

        final int w = grid.width(), h = grid.height();
        final int mass = grid.mass();

        // --- Base. The bottom row is the butt of the tool: what we lash or strike. ---
        final int baseWidth = longestRun(grid, h - 1);
        if (baseWidth == 0) return null; // Nothing to build on

        int baseDepth = 0;
        int baseFilled = 0, baseCells = 0;
        for (int y = h - 1; y >= 0; y--)
        {
            // Note the max(1, ...): without it a 1-wide base would accept literally any row below it
            if (longestRun(grid, y) < Math.max(1, baseWidth - 1)) break;
            baseDepth++;
            for (int x = 0; x < w; x++)
            {
                baseCells++;
                if (grid.get(x, y)) baseFilled++;
            }
        }
        final float baseSolid = baseCells == 0 ? 0f : (float) baseFilled / baseCells;

        // --- Tip. The top row is the working end. ---
        final int tipWidth = rowWidth(grid, 0);

        // --- Taper. Reward a monotone narrowing from base to tip; punish a piece that bulges back out. ---
        float taperScore = 0f;
        int taperSteps = 0;
        int previous = rowWidth(grid, h - 1);
        for (int y = h - 2; y >= 0; y--)
        {
            final int width = rowWidth(grid, y);
            taperSteps++;
            if (width <= previous) taperScore += 1f;
            else if (width == previous + 1) taperScore += 0.5f; // A slight flare is forgivable
            previous = width;
        }
        final float tipTaper = taperSteps == 0 ? 0f : taperScore / taperSteps;

        final int shoulder = Math.max(0, h - baseDepth);

        // --- Symmetry, measured about the vertical centreline. ---
        int matched = 0, compared = 0;
        for (int y = 0; y < h; y++)
        {
            for (int x = 0; x < w; x++)
            {
                compared++;
                if (grid.get(x, y) == grid.get(w - 1 - x, y)) matched++;
            }
        }
        final float symmetry = compared == 0 ? 0f : (float) matched / compared;

        // --- Widest run, and the length of the worked edge. ---
        int widestRun = 0;
        for (int y = 0; y < h; y++) widestRun = Math.max(widestRun, longestRun(grid, y));

        int edgeLength = 0;
        for (int y = 0; y < h; y++)
        {
            if (rowWidth(grid, y) <= 2) edgeLength++;
            else break;
        }

        final float aspect = (float) h / w;

        return new KnapMetrics(grid, mass, baseWidth, baseDepth, baseSolid, tipWidth, tipTaper, shoulder, symmetry,
            isConnected(grid, mass), widestRun, edgeLength, aspect);
    }

    /** Width of the bounding span of a row, holes included. */
    private static int rowWidth(KnapGrid grid, int y)
    {
        int min = -1, max = -1;
        for (int x = 0; x < grid.width(); x++)
        {
            if (grid.get(x, y))
            {
                if (min < 0) min = x;
                max = x;
            }
        }
        return min < 0 ? 0 : max - min + 1;
    }

    /** Longest run of contiguous stone in a row. A notched row is a weak row. */
    private static int longestRun(KnapGrid grid, int y)
    {
        int best = 0, run = 0;
        for (int x = 0; x < grid.width(); x++)
        {
            run = grid.get(x, y) ? run + 1 : 0;
            best = Math.max(best, run);
        }
        return best;
    }

    /** Flood fill, 4-connected. Two flakes sat side by side are not a tool head. */
    private static boolean isConnected(KnapGrid grid, int mass)
    {
        final int w = grid.width(), h = grid.height();
        final boolean[] seen = new boolean[w * h];
        final int[] stack = new int[w * h];
        int head = 0, found = 0;

        outer:
        for (int y = 0; y < h; y++)
        {
            for (int x = 0; x < w; x++)
            {
                if (grid.get(x, y))
                {
                    stack[head++] = x + y * w;
                    seen[x + y * w] = true;
                    break outer;
                }
            }
        }
        while (head > 0)
        {
            final int index = stack[--head];
            final int x = index % w, y = index / w;
            found++;
            for (int[] d : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}})
            {
                final int nx = x + d[0], ny = y + d[1];
                if (nx >= 0 && ny >= 0 && nx < w && ny < h && grid.get(nx, ny) && !seen[nx + ny * w])
                {
                    seen[nx + ny * w] = true;
                    stack[head++] = nx + ny * w;
                }
            }
        }
        return found == mass;
    }
}
