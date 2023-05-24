package com.github.alexandergillon.wegapi.game;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface that a game server needs to implement to accept and process user actions. <br> <br>
 *
 * For developers of games based on WEGAPI: in order to create a game, subclass BaseServer, which will require
 * you to implement the methods in this interface. WEGAPI will handle client input, displaying the game to the client,
 * etc. <br> <br>
 *
 * For developers of WEGAPI itself: see ClientDaemon.java for more information about how communication works, and
 * what the daemon achieves. This interface is used by the client daemon to contact the server.
 *
 * todo: write a tutorial about how to create a game with wegapi
 */
public interface GameServerInterface extends Remote {
    String DEFAULT_IP = "127.0.0.1";
    int RMI_REGISTRY_PORT = 1099;
    String DEFAULT_SERVER_PATH = "WEGAPI/GameServer"; // path to bind the game server to in the registry

    /**
     * Informs the server that a client has joined the game for the first time. This is when the daemon has just
     * started, and did not find any game already in progress to recover.
     */
    void registerPlayer(PlayerInterface player) throws RemoteException;

    /**
     * Informs the server that a certain player double-clicked a certain tile.
     *
     * @param tileIndex the index of the tile that the player clicked
     * @param player which player clicked the tile
     */
    void tileClicked(int tileIndex, PlayerData player) throws RemoteException;

    /**
     * Informs the server that a certain player dragged one tile to another.
     *
     * @param fromTileIndex the index of the tile that was dragged
     * @param toTileIndex the index of the tile that the tile was dragged to
     * @param player the player who dragged the tile
     */
    void tileDragged(int fromTileIndex, int toTileIndex, PlayerData player) throws RemoteException;

    static GameServerInterface connectToServer(String ip, int port) throws RemoteException, NotBoundException, MalformedURLException {
        return connect(ip, port, DEFAULT_SERVER_PATH);
    }

    static GameServerInterface connect(String ip, int port, String path) throws RemoteException, NotBoundException, MalformedURLException {
        return (GameServerInterface) Naming.lookup("//" + ip + ":" + port + "/" + path);
    }
}