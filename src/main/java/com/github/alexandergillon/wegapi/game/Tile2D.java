package com.github.alexandergillon.wegapi.game;

import java.util.Objects;

/**
 * Class that encapsulates information about a tile in a player's game directory. Used for creating new
 * tiles/updating old ones in a player's game directory. 2D version of PlayerInterface::Tile.
 */
public final class Tile2D {
    private final int row;
    private final int col;
    private final String iconName;
    private final String tileName; // may be null

    /**
     * Creates a tile with the given row, column and icon name, with no tile name.
     *
     * @param row      row of the tile, counting from the top downwards
     * @param col      column of the tile, counting from the left rightwards
     * @param iconName the name of the tile's icon, with or without the .ico extension
     */
    public Tile2D(int row, int col, String iconName) {
        this.row = row;
        this.col = col;
        this.iconName = iconName;
        this.tileName = null;
    }

    /**
     * Creates a tile with a given row, column, icon name, and tile name.
     *
     * @param row      row of the tile, counting from the top downwards
     * @param col      column of the tile, counting from the left rightwards
     * @param iconName the name of the tile's icon, with or without the .ico extension
     * @param tileName the name of the tile, as displayed to the player
     */
    public Tile2D(int row, int col, String iconName, String tileName) {
        this.row = row;
        this.col = col;
        this.iconName = iconName;
        this.tileName = tileName;
    }

    /**
     * Creates a tile with the given coordinate as its position, an icon name, and no tile name.
     *
     * @param tileCoordinate the location of the tile
     * @param iconName       the name of the tile's icon, with or without the .ico extension
     */
    public Tile2D(TileCoordinate tileCoordinate, String iconName) {
        this.row = tileCoordinate.getRow();
        this.col = tileCoordinate.getCol();
        this.iconName = iconName;
        this.tileName = null;
    }

    /**
     * Creates a tile with the given coordinate as its position, an icon name, and tile name.
     *
     * @param tileCoordinate the location of the tile
     * @param iconName       the name of the tile's icon, with or without the .ico extension
     * @param tileName       the name of the tile, as displayed to the player
     */
    public Tile2D(TileCoordinate tileCoordinate, String iconName, String tileName) {
        this.row = tileCoordinate.getRow();
        this.col = tileCoordinate.getCol();
        this.iconName = iconName;
        this.tileName = tileName;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public String getIconName() {
        return iconName;
    }

    public String getTileName() {
        return tileName;
    }

    /** Tiles compare as equal if they have the same coordinates, icon and name. */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Tile2D other = (Tile2D) obj;
        return row == other.row && col == other.col && iconName.equals(other.iconName) && Objects.equals(tileName, other.tileName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, col, iconName, tileName);
    }
}
