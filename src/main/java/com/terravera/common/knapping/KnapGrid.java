/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera.common.knapping;

import java.util.Arrays;

/**
 * An immutable snapshot of a knapping grid - {@code true} means "stone is still there", {@code false} means the flake
 * has been struck off. This is deliberately decoupled from TerraFirmaCraft's {@code KnappingPattern} so that the
 * geometry code in {@link KnapAnalysis} is pure, side effect free, and unit testable without a running game.
 */
public final class KnapGrid
{
    public static KnapGrid of(String... rows)
    {
        final int height = rows.length;
        final int width = rows[0].length();
        final boolean[] cells = new boolean[width * height];
        for (int y = 0; y < height; y++)
        {
            if (rows[y].length() != width) throw new IllegalArgumentException("Ragged knapping grid");
            for (int x = 0; x < width; x++)
            {
                cells[x + y * width] = rows[y].charAt(x) != ' ';
            }
        }
        return new KnapGrid(width, height, cells);
    }

    private final int width;
    private final int height;
    private final boolean[] cells;

    public KnapGrid(int width, int height, boolean[] cells)
    {
        this.width = width;
        this.height = height;
        this.cells = cells;
    }

    public int width()
    {
        return width;
    }

    public int height()
    {
        return height;
    }

    public boolean get(int x, int y)
    {
        return x >= 0 && y >= 0 && x < width && y < height && cells[x + y * width];
    }

    public int mass()
    {
        int count = 0;
        for (boolean cell : cells) if (cell) count++;
        return count;
    }

    /**
     * @return This grid, rotated clockwise by {@code 90 * quarterTurns} degrees. Rotations matter because a knapped
     * head has no inherent "up" - a spear point knapped sideways is still a spear point.
     */
    public KnapGrid rotate(int quarterTurns)
    {
        KnapGrid grid = this;
        for (int i = 0; i < Math.floorMod(quarterTurns, 4); i++)
        {
            final boolean[] next = new boolean[grid.width * grid.height];
            final int w = grid.height, h = grid.width;
            for (int y = 0; y < grid.height; y++)
            {
                for (int x = 0; x < grid.width; x++)
                {
                    // (x, y) -> (h - 1 - y, x) in the rotated frame
                    next[(w - 1 - y) + x * w] = grid.get(x, y);
                }
            }
            grid = new KnapGrid(w, h, next);
        }
        return grid;
    }

    /**
     * @return This grid cropped to the bounding box of its remaining stone, or {@code null} if nothing is left.
     */
    public KnapGrid trimmed()
    {
        int minX = width, minY = height, maxX = -1, maxY = -1;
        for (int y = 0; y < height; y++)
        {
            for (int x = 0; x < width; x++)
            {
                if (get(x, y))
                {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        if (maxX < 0) return null;

        final int w = maxX - minX + 1, h = maxY - minY + 1;
        final boolean[] next = new boolean[w * h];
        for (int y = 0; y < h; y++)
        {
            for (int x = 0; x < w; x++)
            {
                next[x + y * w] = get(x + minX, y + minY);
            }
        }
        return new KnapGrid(w, h, next);
    }

    @Override
    public boolean equals(Object other)
    {
        return other instanceof KnapGrid g && g.width == width && g.height == height && Arrays.equals(g.cells, cells);
    }

    @Override
    public int hashCode()
    {
        return 31 * (31 * width + height) + Arrays.hashCode(cells);
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        for (int y = 0; y < height; y++)
        {
            for (int x = 0; x < width; x++) sb.append(get(x, y) ? '#' : ' ');
            sb.append('\n');
        }
        return sb.toString();
    }
}
