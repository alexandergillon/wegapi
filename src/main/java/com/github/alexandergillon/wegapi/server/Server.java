// java -cp wegapi.jar com.github.alexandergillon.wegapi.server.Server
package com.github.alexandergillon.wegapi.server;

import com.github.alexandergillon.wegapi.game.GameServerInterface;
import com.github.alexandergillon.wegapi.game.PlayerInterface;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Server extends BaseServer implements GameServerInterface {
    private final HashMap<Integer, PlayerInterface> players = new HashMap<>();
    private final AtomicInteger playerNumber = new AtomicInteger();

    public Server() throws RemoteException {
        super();
    }

    @Override
    public void registerPlayer(PlayerInterface player) {
        System.out.println("server: register client received");
        int thisPlayerNumber = playerNumber.getAndIncrement();
        players.put(thisPlayerNumber, player);
        try {
            player.initialize(thisPlayerNumber);
            PlayerInterface.Tile tile1 = new PlayerInterface.Tile(0, "black-king-cream");
            PlayerInterface.Tile tile2 = new PlayerInterface.Tile(1, "black-king-olive");
            ArrayList<PlayerInterface.Tile> tiles = new ArrayList<>();
            tiles.add(tile1);
            tiles.add(tile2);
            player.deleteTiles(new ArrayList<>(), PlayerInterface.DeleteTilesMode.DELETE_ALL);
            player.createTiles(tiles, PlayerInterface.CreateTilesMode.CREATE);
        } catch (RemoteException e) {
            System.out.println("server: player " + thisPlayerNumber + " not reachable while initializing," + e.toString());
        }
    }

    static boolean cream = false; // only for tileClicked and tileDragged
    @Override
    public void tileClicked(int tileIndex, PlayerData playerData) {
        System.out.printf("server: tile clicked: %d by player %d%n", tileIndex, playerData.getPlayerNumber());
        PlayerInterface.Tile t1 = new PlayerInterface.Tile(tileIndex, cream ? "black-king-cream" : "black-king-olive");
        PlayerInterface.Tile t2 = new PlayerInterface.Tile(tileIndex + 1, cream ? "black-king-cream" : "black-king-olive");
        ArrayList<PlayerInterface.Tile> tiles = new ArrayList<>();
        tiles.add(t1);
        tiles.add(t2);
        try {
            playerData.getPlayer().createTiles(tiles, PlayerInterface.CreateTilesMode.CREATE);
        } catch (RemoteException e) {
            System.out.println("server: player " + playerData.getPlayerNumber() + " not reachable while responding to tileClicked" + e.toString());
        }
        cream = !cream;
    }

    @Override
    public void tileDragged(int fromTileIndex, int toTileIndex, PlayerData playerData) {
        System.out.printf("server: tile dragged: from %d to %d by player %d%n", fromTileIndex, toTileIndex, playerData.getPlayerNumber());
        ArrayList<Integer> toDelete = new ArrayList<>();
        toDelete.add(toTileIndex);
        try {
            playerData.getPlayer().deleteTiles(toDelete, PlayerInterface.DeleteTilesMode.DELETE_EXISTING);
        } catch (RemoteException e) {
            System.out.println("server: player " + playerData.getPlayerNumber() + " not reachable while responding to tileClicked" + e.toString());
        }
        cream = !cream;
    }

    public static void main(String[] args) {
        try {
            LocateRegistry.createRegistry(GameServerInterface.rmiRegistryPort);
        } catch (RemoteException ignore) {
            // RMI server already exists
        }

        try {
            Server server = new Server();
            Naming.rebind("//" + GameServerInterface.defaultIp + ":" + GameServerInterface.rmiRegistryPort + "/" + GameServerInterface.defaultServerPath, server);
        } catch (RemoteException e) {
            System.out.printf("Failed to rebind server, %s%n", e);
            System.exit(1);
        } catch (MalformedURLException e) {
            System.out.printf("Malformed URL: %s%n", e);
            System.exit(1);
        }
        System.out.println("Server ready!");
    }
}