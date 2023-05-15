package com.github.alexandergillon.wegapi.game;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface PlayerInterface extends Remote {
    public void initialize(int playerNumber) throws RemoteException;

    public void displayMessage(String message, boolean error) throws RemoteException;

    public enum CreateTilesMode {
        CREATE,
        CREATE_NEW,
        OVERWRITE_EXISTING
    }
    public void createTiles(ArrayList<GameInterface.Tile> tiles, CreateTilesMode mode) throws RemoteException;

    public void gameOver(boolean win) throws RemoteException;
}
