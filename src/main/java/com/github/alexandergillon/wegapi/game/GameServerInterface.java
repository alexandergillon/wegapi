package com.github.alexandergillon.wegapi.game;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface GameServerInterface extends Remote {
    String defaultIp = "127.0.0.1";
    int rmiRegistryPort = 1099;
    String defaultServerPath = "WEGAPI/GameServer"; // path to bind the game server to in the registry

    static class PlayerData implements Serializable {
        private final int playerNumber;
        private final PlayerInterface player;

        public PlayerData(int playerNumber, PlayerInterface player) {
            this.playerNumber = playerNumber;
            this.player = player;
        }

        public int getPlayerNumber() {
            return playerNumber;
        }

        public PlayerInterface getPlayer() {
            return player;
        }
    }

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
        return connect(ip, port, defaultServerPath);
    }

    static GameServerInterface connect(String ip, int port, String path) throws RemoteException, NotBoundException, MalformedURLException {
        return (GameServerInterface) Naming.lookup("//" + ip + ":" + port + "/" + path);
    }
}