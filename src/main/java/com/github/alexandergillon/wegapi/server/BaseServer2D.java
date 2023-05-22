package com.github.alexandergillon.wegapi.server;

import com.github.alexandergillon.wegapi.game.GameServerInterface2D;
import com.github.alexandergillon.wegapi.game.PlayerInterface;
import com.github.alexandergillon.wegapi.game.PlayerInterface2D;

import java.rmi.RemoteException;
import java.util.ArrayList;

public abstract class BaseServer2D extends BaseServer implements GameServerInterface2D {
    private final int rows;
    private final int cols;

    public BaseServer2D(int rows, int cols) throws RemoteException {
        super();
        this.rows = rows;
        this.cols = cols;
    }

    // must implement a main method

    public final int getRows() {
        return rows;
    }

    public final int getCols() {
        return cols;
    }

    private int coordsToIndex(int row, int col) {
        return row * cols + col;
    }

    private PlayerInterface2D.TileCoordinate indexToCoords(int index) {
        return new PlayerInterface2D.TileCoordinate(index / cols, index % cols);
    }

    @Override
    public final void registerPlayer(PlayerInterface player) {
        registerPlayer2D(new PlayerInterfaceWrapper(player, this));
    }

    @Override
    public final void tileClicked(int tileIndex, PlayerData playerData) throws RemoteException {
        PlayerInterface2D.TileCoordinate coords = indexToCoords(tileIndex);
        PlayerData2D playerData2D = PlayerInterfaceWrapper.createPlayerDataWrapper(playerData, this);
        tileClicked2D(coords.getRow(), coords.getCol(), playerData2D);
    }

    @Override
    public final void tileDragged(int fromTileIndex, int toTileIndex, PlayerData playerData) throws RemoteException {
        PlayerInterface2D.TileCoordinate fromCoords = indexToCoords(fromTileIndex);
        PlayerInterface2D.TileCoordinate toCoords = indexToCoords(toTileIndex);
        PlayerData2D playerData2D = PlayerInterfaceWrapper.createPlayerDataWrapper(playerData, this);
        tileDragged2D(fromCoords.getRow(), fromCoords.getCol(), toCoords.getRow(), toCoords.getCol(), playerData2D);
    }

    private static final class PlayerInterfaceWrapper implements PlayerInterface2D {
        private final PlayerInterface player;
        private final BaseServer2D parent;

        public PlayerInterfaceWrapper(PlayerInterface player, BaseServer2D parent) {
            this.player = player;
            this.parent = parent;
        }

        public static PlayerData2D createPlayerDataWrapper(PlayerData playerData, BaseServer2D parent) {
            PlayerInterface2D player2D = new PlayerInterfaceWrapper(playerData.getPlayer(), parent);
            return new PlayerData2D(playerData.getPlayerNumber(), player2D);
        }

        @Override
        public void initialize(int playerNumber) throws RemoteException {
            player.initialize(playerNumber);
        }

        @Override
        public void displayMessage(String message, boolean error) throws RemoteException {
            player.displayMessage(message, error);
        }

        private PlayerInterface.Tile tile2Dto1D(Tile2D tile) {
            return new PlayerInterface.Tile(parent.coordsToIndex(tile.getRow(), tile.getCol()), tile.getIconName(), tile.getTileName());
        }

        private ArrayList<PlayerInterface.Tile> tiles2Dto1D(ArrayList<Tile2D> tiles) {
            ArrayList<PlayerInterface.Tile> tiles1D = new ArrayList<>(tiles.size());
            for (Tile2D tile : tiles) {
                tiles1D.add(tile2Dto1D(tile));
            }
            return tiles1D;
        }

        private ArrayList<Integer> tileCoordinates2Dto1D(ArrayList<TileCoordinate> tileCoordinates) {
            ArrayList<Integer> indices = new ArrayList<>(tileCoordinates.size());
            for (TileCoordinate tileCoordinate : tileCoordinates) {
                indices.add(parent.coordsToIndex(tileCoordinate.getRow(), tileCoordinate.getCol()));
            }
            return indices;
        }

        @Override
        public void createTiles(ArrayList<Tile2D> tiles, PlayerInterface.CreateTilesMode mode) throws RemoteException {
            player.createTiles(tiles2Dto1D(tiles), mode);
        }

        @Override
        public void deleteTiles(ArrayList<TileCoordinate> tileCoordinates, PlayerInterface.DeleteTilesMode mode) throws RemoteException {
            player.deleteTiles(tileCoordinates2Dto1D(tileCoordinates), mode);
        }

        @Override
        public void gameOver(boolean win) throws RemoteException {
            player.gameOver(win);
        }
    }

}
