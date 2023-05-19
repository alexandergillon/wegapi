package com.github.alexandergillon.wegapi.server;

import com.github.alexandergillon.wegapi.game.GameInterface;
import com.github.alexandergillon.wegapi.game.PlayerInterface;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public abstract class BaseServer extends UnicastRemoteObject implements GameInterface {
    public BaseServer() throws RemoteException {
        super(0);
    }

    @Override
    public abstract void registerPlayer(PlayerInterface player) throws RemoteException;
    @Override
    public abstract void tileClicked(int tileIndex, PlayerData playerData) throws RemoteException;
    @Override
    public abstract void tileDragged(int fromTileIndex, int toTileIndex, PlayerData playerData) throws RemoteException;

    // must implement a main method
}
