package com.github.alexandergillon.wegapi.server;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

import com.github.alexandergillon.wegapi.game_action.GameAction;

public interface ServerInterface extends Remote {
    ArrayList<GameAction> clientInit() throws RemoteException;
    ArrayList<GameAction> tileClicked(int tile, int player) throws RemoteException;
    ArrayList<GameAction> tileDragged(int fromTile, int toTile, int player) throws RemoteException;
}