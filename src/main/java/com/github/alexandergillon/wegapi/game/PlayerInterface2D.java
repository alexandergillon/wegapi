package com.github.alexandergillon.wegapi.game;

import java.rmi.RemoteException;
import java.util.ArrayList;

public interface PlayerInterface2D {
    public static final class TileCoordinate {
        private final int row;
        private final int col;

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

    public static class Tile2D {
        private final int row;
        private final int col;
        private final String iconName;
        private final String tileName; // may be null

        public Tile2D(int row, int col, String iconName) {
            this.row = row;
            this.col = col;
            this.iconName = iconName;
            this.tileName = null;
        }

        public Tile2D(int row, int col, String iconName, String tileName) {
            this.row = row;
            this.col = col;
            this.iconName = iconName;
            this.tileName = tileName;
        }

        public Tile2D(TileCoordinate tileCoordinate, String iconName) {
            this.row = tileCoordinate.getRow();
            this.col = tileCoordinate.getCol();
            this.iconName = iconName;
            this.tileName = null;
        }

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

    void initialize(int playerNumber) throws RemoteException;
    void displayMessage(String message, boolean error) throws RemoteException;
    void createTiles(ArrayList<Tile2D> tiles, PlayerInterface.CreateTilesMode mode) throws RemoteException;
    // todo: make this accept null
    void deleteTiles(ArrayList<TileCoordinate> tileCoordinates, PlayerInterface.DeleteTilesMode mode) throws RemoteException;

    void gameOver(boolean win) throws RemoteException;
}
