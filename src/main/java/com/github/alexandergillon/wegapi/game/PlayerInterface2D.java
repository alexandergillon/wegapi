package com.github.alexandergillon.wegapi.game;

import java.rmi.RemoteException;
import java.util.ArrayList;

/**
 * Interface that can be used to tell a player to do something, for a 2D game. For example, show certain tiles to
 * the player, or a message. <br> <br>
 *
 * For developers of 2D games based on WEGAPI: this interface is essentially all the things you can get a player
 * to 'do'. <br> <br>
 *
 * For developers of WEGAPI itself: see BaseServer2D.java for how this interface fits into the WEGAPI model. This
 * interface is actually a wrapper on underlying (1D) functions via the PlayerInterface, and so this interface
 * is not used directly for RMI communication.
 */
public interface PlayerInterface2D {
    /**
     * Represents a tile's position in the game directory, as a (row, column) pair. Rows start at the top
     * and count up downwards, and columns start at the left and count up rightwards.
     */
    final class TileCoordinate {
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
    }

    /**
     * Class that encapsulates information about a tile in a player's game directory. Used for creating new
     * tiles/updating old ones in a player's game directory. 2D version of PlayerInterface::Tile.
     */
    class Tile2D {
        private final int row;
        private final int col;
        private final String iconName;
        private final String tileName; // may be null

        /**
         * Creates a tile with the given row, column and icon name, with no tile name.
         *
         * @param row row of the tile, counting from the top downwards
         * @param col column of the tile, counting from the left rightwards
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
         * @param row row of the tile, counting from the top downwards
         * @param col column of the tile, counting from the left rightwards
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
         * @param iconName the name of the tile's icon, with or without the .ico extension
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
         * @param iconName the name of the tile's icon, with or without the .ico extension
         * @param tileName the name of the tile, as displayed to the player
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

        public int getCol(){
            return col;
        }

        public String getIconName() {
            return iconName;
        }

        public String getTileName() {
            return tileName;
        }
    }

    /**
     * Informs a newly-joined player of their player number.
     *
     * @param playerNumber the new player's player number
     */
    void initialize(int playerNumber) throws RemoteException;

    /**
     * Displays a message to a player, in a message box (todo: actually do it in a message box - for now, is printed
     * to the console). Optionally makes the message look like an error to the user.
     *
     * @param message the message to display
     * @param error whether or not this message represents an error to the user (if true, the message will have an
     *              error icon)
     */
    void displayMessage(String message, boolean error) throws RemoteException;

    /**
     * Creates tiles in the player's game directory. Uses one of the following modes: <br> <br>
     *
     *   - CREATE: creates tiles, whether or not they exist. <br>
     *   - CREATE_NEW: creates tiles only if they do not exist. If some tile already exists, the client will
     *                 abort and print an error message. <br>
     *   - OVERWRITE_EXISTING: only overwrites existing tiles. If some tile does not exist, the client will abort
     *                         and print an error message.
     *
     * @param tiles tiles to create in the player's game directory
     * @param mode mode (how to handle existing tiles, etc.)
     */
    void createTiles(ArrayList<Tile2D> tiles, PlayerInterface.CreateTilesMode mode) throws RemoteException;


    /**
     * Deletes tiles in the player's game directory, as specified by their indices. Uses one of the following modes: <br> <br>
     *
     *   - DELETE: deletes tiles, whether or not they exist. <br>
     *   - DELETE_EXISTING: deletes tiles only if they exist. If some tile does not exist, the client will abort
     *                      and print an error message.
     *   - DELETE_ALL: deletes all tiles. If this parameter is specified, tileIndices should be null.
     *
     * @param tileCoordinates coordinates of the tiles to delete - should be null if mode is DELETE_ALL (todo: actually handle null)
     * @param mode mode (how to handle non-existent tiles, etc.)
     */
    // todo: make this accept null
    void deleteTiles(ArrayList<TileCoordinate> tileCoordinates, PlayerInterface.DeleteTilesMode mode) throws RemoteException;

    /**
     * Informs the player that the game is over. They will not be able to do anything until a new game is started.
     *
     * @param win whether the player won
     */
    void gameOver(boolean win) throws RemoteException;
}
