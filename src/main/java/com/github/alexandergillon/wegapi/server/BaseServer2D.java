package com.github.alexandergillon.wegapi.server;

import com.github.alexandergillon.wegapi.game.GameServerInterface2D;
import com.github.alexandergillon.wegapi.game.PlayerInterface;
import com.github.alexandergillon.wegapi.game.PlayerInterface2D;

import java.rmi.RemoteException;
import java.util.ArrayList;

public abstract class BaseServer2D extends BaseServer implements GameServerInterface2D {
    /**
     * Creates a BaseServer2D with the appropriate rows and cols. Must be called by subclasses in their constructor.
     *
     * @param numRows the number of rows in the game grid
     * @param numCols the number of columns in the game grid
     */
    protected BaseServer2D(int numRows, int numCols) throws RemoteException {
        super();
        this.numRows = numRows;
        this.numCols = numCols;
    }

    public final int getNumRows() {
        return numRows;
    }

    public final int getNumCols() {
        return numCols;
    }

    // BELOW THIS POINT IS IMPLEMENTATION DETAILS, AND SHOULD NOT BE USED BY SUBCLASSES

    private final int numRows;
    private final int numCols;

    /**
     * Class which wraps a PlayerInterface into a PlayerInterface2D, as all communication between the server and
     * a player only uses 1D indices and 1D tiles. Handles conversions between 2D coordinates/tiles/etc. and
     * 1D indices/tiles/etc. transparently to any users - users can use the PlayerInterface2D without needing to
     * know that it delegates to a PlayerInterface underneath. <br> <br>
     *
     * Instances of this class can then be passed to methods expecting a PlayerInterface2D, and will allow those
     * methods to communicate with the player via 2D methods (rather than 1D). <br> <br>
     *
     * Note: a PlayerInterfaceWrapper must exist in reference to some BaseServer2D, which we call its parent. This is
     * because in order to convert between 2D and 1D 'indices', we need to know how large the 2D 'board' (or grid,
     * etc.) is, which only a BaseServer2D knows. The PlayerInterfaceWrapper finds this out by querying its parent
     * BaseServer2D.
     */
    private static final class PlayerInterfaceWrapper implements PlayerInterface2D {
        private final PlayerInterface player;
        private final BaseServer2D parent;

        /**
         * Creates a new PlayerInterfaceWrapper which wraps a given 1D PlayerInterface. Also takes a parent
         * BaseServer2D so that this wrapper can find out the size of the 2D board/grid. <br> <br>
         *
         * Note: a PlayerInterfaceWrapper must exist in reference to some BaseServer2D, which we call its parent.
         * This is because in order to convert between 2D and 1D 'indices', we need to know how large the 2D 'board'
         * (or grid, etc.) is, which only a BaseServer2D knows. The PlayerInterfaceWrapper finds this out by querying
         * its parent BaseServer2D.
         *
         * @param player the PlayerInterface to wrap
         * @param parent the BaseServer2D object which created this PlayerInterfaceWrapper
         */
        private PlayerInterfaceWrapper(PlayerInterface player, BaseServer2D parent) {
            this.player = player;
            this.parent = parent;
        }

        /**
         * Static method which wraps a PlayerData into a PlayerData2D. This again allows methods expecting a
         * PlayerData2D to use 2D functions to communicate with a player (rather than 1D). <br> <br>
         *
         * Note: a PlayerInterfaceWrapper must exist in reference to some BaseServer2D, as described in
         * PlayerInterfaceWrapper documentation. A PlayerData wrapper needs to use a PlayerInterfaceWrapper under
         * the hood, so a parent BaseServer2D is also needed here.
         *
         * @param playerData the PlayerData to wrap
         * @param parent the BaseServer2D object which created this wrapper
         * @return a PlayerData2D that transparently delegates to an underlying PlayerData, performing appropriate
         *         conversions
         */
        private static PlayerData2D createPlayerDataWrapper(PlayerData playerData, BaseServer2D parent) {
            PlayerInterface2D player2D = new PlayerInterfaceWrapper(playerData.getPlayer(), parent);
            return new PlayerData2D(playerData.getPlayerNumber(), player2D);
        }

        /**
         * Converts a 2D coordinate pair to a 1D index, relative to the size of the BaseServer2D which created this
         * PlayerInterfaceWrapper.
         *
         * @param tileCoordinates the coordinates to convert to 1D indices
         * @return the converted indices, relative to the size of the BaseServer2D which created
         *         this PlayerInterfaceWrapper
         */
        private ArrayList<Integer> tileCoordinates2Dto1D(ArrayList<TileCoordinate> tileCoordinates) {
            ArrayList<Integer> indices = new ArrayList<>(tileCoordinates.size());
            for (TileCoordinate tileCoordinate : tileCoordinates) {
                indices.add(parent.coordsToIndex(tileCoordinate.getRow(), tileCoordinate.getCol()));
            }
            return indices;
        }

        /**
         * Converts a 2D tile to a 1D tile, relative to the size of the BaseServer2D which created this
         * PlayerInterfaceWrapper.
         *
         * @param tile the 2D tile to convert to a 1D tile
         * @return the converted tile, relative to the size of the BaseServer2D which created this
         *         PlayerInterfaceWrapper
         */
        private PlayerInterface.Tile tile2Dto1D(Tile2D tile) {
            return new PlayerInterface.Tile(parent.coordsToIndex(tile.getRow(), tile.getCol()),
                    tile.getIconName(), tile.getTileName());
        }

        /**
         * Converts a list of 2D tiles to a list of 1D tiles, relative to the size of the BaseServer2D which created
         * this PlayerInterfaceWrapper.
         *
         * @param tiles the 2D tiles to convert to 1D tiles
         * @return the converted tiles, relative to the size of the BaseServer2D which created this
         *         PlayerInterfaceWrapper
         */
        private ArrayList<PlayerInterface.Tile> tiles2Dto1D(ArrayList<Tile2D> tiles) {
            ArrayList<PlayerInterface.Tile> tiles1D = new ArrayList<>(tiles.size());
            for (Tile2D tile : tiles) {
                tiles1D.add(tile2Dto1D(tile));
            }
            return tiles1D;
        }

        /** Passes through the initialize call, as initialize is not dimensional. */
        @Override
        public void initialize(int playerNumber) throws RemoteException {
            player.initialize(playerNumber);
        }

        /** Passes through the displayMessage call, as displayMessage is not dimensional. */
        @Override
        public void displayMessage(String message, boolean error) throws RemoteException {
            player.displayMessage(message, error);
        }

        /** Wraps the createTiles call, converting 2D tiles to 1D tiles to be sent to the player. */
        @Override
        public void createTiles(ArrayList<Tile2D> tiles, PlayerInterface.CreateTilesMode mode) throws RemoteException {
            player.createTiles(tiles2Dto1D(tiles), mode);
        }

        /** Wraps the deleteTiles call, converting 2D coordinates to 1D indices to be sent to the player. */
        @Override
        public void deleteTiles(ArrayList<TileCoordinate> tileCoordinates, PlayerInterface.DeleteTilesMode mode) throws RemoteException {
            player.deleteTiles(tileCoordinates2Dto1D(tileCoordinates), mode);
        }

        /** Passes through the gameOver call, as gameOver is not dimensional. */
        @Override
        public void gameOver(boolean win) throws RemoteException {
            player.gameOver(win);
        }
    }

    /**
     * Converts a 2D pair of coordinates to a 1D index, relative to the size of this BaseServer2D.
     *
     * @param row row to convert
     * @param col column to convert
     * @return the index corresponding to the tile with coordinates (row, col)
     */
    private int coordsToIndex(int row, int col) {
        return row * numCols + col;
    }

    /**
     * Converts a 1D index to a 2D pair of coordinates, relative to the size of this BaseServer2D.
     *
     * @param index the index to convert
     * @return coordinates corresponding to the tile with that index
     */
    private PlayerInterface2D.TileCoordinate indexToCoords(int index) {
        return new PlayerInterface2D.TileCoordinate(index / numCols, index % numCols);
    }

    /**
     * Wraps the 1D registerPlayer and calls registerPlayer2D, which will be implemented by a subclass. This allows
     * subclasses to only implement 2D methods, and not worry about the needed conversions to 1D (as the remote
     * objects of players only accept 1D parameters).
     */
    @Override
    public final void registerPlayer(PlayerInterface player) {
        registerPlayer2D(new PlayerInterfaceWrapper(player, this));
    }

    /**
     * Wraps the 1D tileClicked and calls tileClicked2D, which will be implemented by a subclass. This allows
     * subclasses to only implement 2D methods, and not worry about the needed conversions to 1D (as the remote
     * objects of players only accept 1D parameters).
     */
    @Override
    public final void tileClicked(int tileIndex, PlayerData playerData) throws RemoteException {
        PlayerInterface2D.TileCoordinate coords = indexToCoords(tileIndex);
        PlayerData2D playerData2D = PlayerInterfaceWrapper.createPlayerDataWrapper(playerData, this);
        tileClicked2D(coords.getRow(), coords.getCol(), playerData2D);
    }

    /**
     * Wraps the 1D tileDragged and calls tileDragged2D, which will be implemented by a subclass. This allows
     * subclasses to only implement 2D methods, and not worry about the needed conversions to 1D (as the remote
     * objects of players only accept 1D parameters).
     */
    @Override
    public final void tileDragged(int fromTileIndex, int toTileIndex, PlayerData playerData) throws RemoteException {
        PlayerInterface2D.TileCoordinate fromCoords = indexToCoords(fromTileIndex);
        PlayerInterface2D.TileCoordinate toCoords = indexToCoords(toTileIndex);
        PlayerData2D playerData2D = PlayerInterfaceWrapper.createPlayerDataWrapper(playerData, this);
        tileDragged2D(fromCoords.getRow(), fromCoords.getCol(), toCoords.getRow(), toCoords.getCol(), playerData2D);
    }
}
