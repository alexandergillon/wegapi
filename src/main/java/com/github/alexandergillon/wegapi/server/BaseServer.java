package com.github.alexandergillon.wegapi.server;

import com.github.alexandergillon.wegapi.game.GameServerInterface;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public abstract class BaseServer extends UnicastRemoteObject implements GameServerInterface {
    protected BaseServer() throws RemoteException {
        super(0);
    }
}
