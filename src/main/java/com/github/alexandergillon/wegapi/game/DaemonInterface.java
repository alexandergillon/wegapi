package com.github.alexandergillon.wegapi.game;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface DaemonInterface extends Remote {
    String defaultIp = "127.0.0.1";
    int rmiRegistryPort = 1099;
    String defaultDaemonPath = "WEGAPI/ClientDaemon"; // path to bind the daemon to in the registry

    /**
     * Informs the daemon that the player double-clicked a certain tile.
     *
     * @param tile the index of the tile that the player clicked
     */
    void tileClicked(int tile) throws RemoteException;

    /**
     * Informs the daemon that the player dragged one tile to another.
     *
     * @param fromTile the index of the tile that was dragged
     * @param toTile the index of the tile that the tile was dragged to
     */
    void tileDragged(int fromTile, int toTile) throws RemoteException;

    static DaemonInterface connectToDaemon(String ip, int port) throws RemoteException, NotBoundException, MalformedURLException {
        return connect(ip, port, defaultDaemonPath);
    }

    static DaemonInterface connect(String ip, int port, String path) throws RemoteException, NotBoundException, MalformedURLException {
        return (DaemonInterface) Naming.lookup("//" + ip + ":" + port + "/" + path);
    }
}
