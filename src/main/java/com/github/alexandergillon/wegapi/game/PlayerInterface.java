package com.github.alexandergillon.wegapi.game;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface PlayerInterface extends Remote {
    class Tile implements Serializable {
        private final int index;
        private final String iconName;
        private final String tileName; // may be null

        public Tile(int index, String iconName) {
            this.index = index;
            this.iconName = iconName;
            this.tileName = null;
        }

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

    /**
     * Informs a newly-joined client of their player number.
     *
     * @param playerNumber the new client's player number
     */
    void initialize(int playerNumber) throws RemoteException;

    void displayMessage(String message, boolean error) throws RemoteException;

    enum CreateTilesMode {
        CREATE,
        CREATE_NEW,
        OVERWRITE_EXISTING
    }
    void createTiles(ArrayList<Tile> tiles, CreateTilesMode mode) throws RemoteException;

    enum DeleteTilesMode {
        DELETE,
        DELETE_EXISTING,
        DELETE_ALL
    }
    void deleteTiles(ArrayList<Integer> tileIndices, DeleteTilesMode mode) throws RemoteException;

    void gameOver(boolean win) throws RemoteException;
}
