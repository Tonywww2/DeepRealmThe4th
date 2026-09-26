package com.tony.deeprealmtheforth.astral;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** An immutable, row-major mask for the cells in an astral container. */
public final class ContainerLayout {
    public static final int BASE_WIDTH = 12;
    public static final int BASE_HEIGHT = 9;

    public static final List<String> BASE_ROWS = List.of(
            "............",
            "............",
            "...XXXXXX...",
            "...XXXXXX...",
            "...XXXXXX...",
            "...XXXXXX...",
            "...XXXXXX...",
            "............",
            "............"
    );

    private final int width;
    private final int height;
    private final boolean[] open;

    private ContainerLayout(int width, int height, boolean[] open) {
        this.width = width;
        this.height = height;
        this.open = open;
    }

    public static ContainerLayout base() {
        return parse(BASE_WIDTH, BASE_HEIGHT, BASE_ROWS);
    }

    public static ContainerLayout parse(int width, int height, List<String> rows) {
        if (width < 1 || height < 1 || width > 64 || height > 64) {
            throw new IllegalArgumentException("Container dimensions must be within 1..64");
        }
        Objects.requireNonNull(rows, "rows");
        if (rows.size() != height) {
            throw new IllegalArgumentException("Expected " + height + " layout rows, got " + rows.size());
        }

        boolean[] cells = new boolean[Math.multiplyExact(width, height)];
        for (int y = 0; y < height; y++) {
            String row = Objects.requireNonNull(rows.get(y), "layout row " + (y + 1));
            if (row.length() != width) {
                throw new IllegalArgumentException("Layout row " + (y + 1) + " must contain "
                        + width + " characters, got " + row.length());
            }
            for (int x = 0; x < width; x++) {
                char symbol = row.charAt(x);
                if (symbol != 'X' && symbol != '.') {
                    throw new IllegalArgumentException("Layout row " + (y + 1) + ", column "
                            + (x + 1) + " must be X or ., got '" + symbol + "'");
                }
                cells[y * width + x] = symbol == 'X';
            }
        }
        return new ContainerLayout(width, height, cells);
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int size() {
        return open.length;
    }

    public boolean isOpen(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return false;
        }
        return open[y * width + x];
    }

    public boolean isOpen(int index) {
        return index >= 0 && index < open.length && open[index];
    }

    public int openCount() {
        int count = 0;
        for (boolean cell : open) {
            if (cell) count++;
        }
        return count;
    }

    public List<String> rows() {
        String[] rows = new String[height];
        for (int y = 0; y < height; y++) {
            char[] row = new char[width];
            for (int x = 0; x < width; x++) {
                row[x] = isOpen(x, y) ? 'X' : '.';
            }
            rows[y] = new String(row);
        }
        return List.of(rows);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ContainerLayout layout
                && width == layout.width
                && height == layout.height
                && Arrays.equals(open, layout.open);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * width + height) + Arrays.hashCode(open);
    }
}
