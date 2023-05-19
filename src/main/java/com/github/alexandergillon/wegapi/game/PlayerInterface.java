package com.github.alexandergillon.wegapi.game;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface PlayerInterface extends Remote {
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
    void createTiles(ArrayList<GameInterface.Tile> tiles, CreateTilesMode mode) throws RemoteException;

    enum DeleteTilesMode {
        DELETE,
        DELETE_EXISTING,
        DELETE_ALL
    }
    void deleteTiles(ArrayList<Integer> tileIndices, DeleteTilesMode mode) throws RemoteException;

    void gameOver(boolean win) throws RemoteException;
}
