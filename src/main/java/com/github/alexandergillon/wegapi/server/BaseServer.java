package com.github.alexandergillon.wegapi.server;

import com.github.alexandergillon.wegapi.game.GameServerInterface;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * A base server, designed to be subclassed to make a game with WEGAPI. At present, doesn't do much on its own except
 * ensures that subclasses are UnicastRemoteObjects that implement the GameServerInterface.
 *
 * todo: use this class to transparently encrypt game traffic
 */
public abstract class BaseServer extends UnicastRemoteObject implements GameServerInterface {
    protected BaseServer() throws RemoteException {
        super(0);
    }
}
