package com.github.alexandergillon.wegapi.game;

import java.io.Serializable;

/**
 * Class that encapsulates information about a tile in a player's game directory. Used for creating new
 * tiles/updating old ones in a player's game directory.
 */
public class Tile implements Serializable {
    private final int index;
    private final String iconName;
    private final String tileName; // may be null - if so, this tile has no name

    /**
     * Creates a tile with the given index and icon name, and no tile name.
     *
     * @param index    the index of the tile
     * @param iconName the name of the tile's icon, with or without the .ico extension
     */
    public Tile(int index, String iconName) {
        this.index = index;
        this.iconName = iconName;
        this.tileName = null;
    }

    /**
     * Creates a tile with a given index, icon name, and tile name.
     *
     * @param index    the index of the tile
     * @param iconName the name of the tile's icon, with or without the .ico extension
     * @param tileName the name of the tile, as displayed to the player
     */
    public Tile(int index, String iconName, String tileName) {
        this.index = index;
        this.iconName = iconName;
        this.tileName = tileName;
    }

    public int getIndex() {
        return index;
    }

    public String getIconName() {
        return iconName;
    }

    public String getTileName() {
        return tileName;
    }
}
