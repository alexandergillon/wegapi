package com.github.alexandergillon.wegapi.game;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public interface GameInterface extends Remote {
    String defaultIp = "127.0.0.1";
    int rmiRegistryPort = 1099;
    String defaultServerPath = "WEGAPI/GameServer";
    String defaultDaemonPath = "WEGAPI/ClientDaemon";

    class Tile {
        private final int index;
        private final String imageName;

        public Tile(int index, String imageName) {
            this.index = index;
            this.imageName = imageName;
        }

        public int getIndex() {
            return index;
        }

        public String getImageName() {
            return imageName;
        }
    }

    /**
     * Informs the server that a client has joined the game for the first time.
     */
    void clientInit() throws RemoteException;

    /**
     * Informs the server that a certain player double-clicked a certain tile.
     *
     * @param tile the index of the tile that the player clicked
     * @param player which player clicked the tile
     */
    void tileClicked(int tile, int player) throws RemoteException;

    /**
     * Informs the server that a certain player dragged one tile to another.
     *
     * @param fromTile the index of the tile that was dragged
     * @param toTile the index of the tile that the tile was dragged to
     * @param player the player who dragged the tile
     */
    void tileDragged(int fromTile, int toTile, int player) throws RemoteException;

    static GameInterface connectToDaemon(String ip, int port) throws RemoteException, NotBoundException, MalformedURLException {
        return connect(ip, port, defaultDaemonPath);
    }

    static GameInterface connectToServer(String ip, int port) throws RemoteException, NotBoundException, MalformedURLException {
        return connect(ip, port, defaultServerPath);
    }

    static GameInterface connect(String ip, int port, String path) throws RemoteException, NotBoundException, MalformedURLException {
        return (GameInterface) Naming.lookup("//" + ip + ":" + port + "/" + path);
    }

    static void launchRMI(UnicastRemoteObject rmiObject, String ip, int port, String path) throws RemoteException, MalformedURLException {
        try {
            Naming.rebind("//" + ip + ":" + port + "/" + path, rmiObject);
        } catch (RemoteException e) {
            System.out.printf("Failed to rebind server, %s%n", e.toString());
            System.exit(1);
        }
    }
}