package com.github.alexandergillon.wegapi.game;

import java.util.Objects;

/**
 * Represents a tile's position in the game directory, as a (row, column) pair. Rows start at the top
 * and count up downwards, and columns start at the left and count up rightwards.
 */
public final class TileCoordinate {
    private final int row;
    private final int col;

    /**
     * Constructs a TileCoordinate with a given row and column.
     *
     * @param row row of the coordinate, counting from the top downwards
     * @param col column of the coordinate, counting from the left rightwards
     */
    public TileCoordinate(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TileCoordinate other = (TileCoordinate) obj;
        return row == other.row && col == other.col;
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, col);
    }
}
