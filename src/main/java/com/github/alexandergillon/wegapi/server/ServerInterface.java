package com.github.alexandergillon.wegapi.server;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Vector;

import com.github.alexandergillon.wegapi.game_action.GameAction;

public interface ServerInterface extends Remote {
    Vector<GameAction> clientInit() throws RemoteException;
    Vector<GameAction> tileClicked(int tile, int player) throws RemoteException;
    Vector<GameAction> tileDragged(int fromTile, int toTile, int player) throws RemoteException;
}