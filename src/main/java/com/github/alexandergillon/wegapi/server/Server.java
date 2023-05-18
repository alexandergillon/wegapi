// java -cp wegapi.jar com.github.alexandergillon.wegapi.server.Server
package com.github.alexandergillon.wegapi.server;

import com.github.alexandergillon.wegapi.game.GameInterface;
import com.github.alexandergillon.wegapi.game.PlayerInterface;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Server extends UnicastRemoteObject implements GameInterface {
    private final HashMap<Integer, PlayerInterface> players = new HashMap<>();
    private final AtomicInteger playerNumber = new AtomicInteger();

    public Server() throws RemoteException {
        super(0);
    }

    @Override
    public void registerPlayer(PlayerInterface player) {
        System.out.println("server: register client received");
        int thisPlayerNumber = playerNumber.getAndIncrement();
        players.put(thisPlayerNumber, player);
        try {
            player.initialize(thisPlayerNumber);
            Tile tile = new Tile(0, "black-king-cream");
            ArrayList<Tile> tiles = new ArrayList<>();
            tiles.add(tile);
            player.createTiles(tiles, PlayerInterface.CreateTilesMode.CREATE);
        } catch (RemoteException e) {
            System.out.println("server: player " + thisPlayerNumber + " not reachable while initializing," + e.toString());
        }
    }

    static boolean cream = false; // only for tileClicked
    @Override
    public void tileClicked(int tile, PlayerData player) {
        System.out.printf("server: tile clicked: %d by player %d%n", tile, player.getPlayerNumber());
        Tile t = new Tile(tile, cream ? "black-king-cream" : "black-king-olive");
        ArrayList<Tile> tiles = new ArrayList<>();
        tiles.add(t);
        try {
            player.getPlayer().createTiles(tiles, PlayerInterface.CreateTilesMode.CREATE);
        } catch (RemoteException e) {
            System.out.println("server: player " + player.getPlayerNumber() + " not reachable while responding to tileClicked");
        }
        cream = !cream;
    }

    @Override
    public void tileDragged(int fromTile, int toTile, PlayerData player) {
        System.out.printf("server: tile dragged: from %d to %d by player %d%n", fromTile, toTile, player.getPlayerNumber());
        Tile t1 = new Tile(fromTile, cream ? "black-king-cream" : "black-king-olive");
        Tile t2 = new Tile(fromTile, cream ? "black-king-cream" : "black-king-olive");
        ArrayList<Tile> tiles = new ArrayList<>();
        tiles.add(t1);
        tiles.add(t2);
        try {
            player.getPlayer().createTiles(tiles, PlayerInterface.CreateTilesMode.CREATE);
        } catch (RemoteException e) {
            System.out.println("server: player " + player.getPlayerNumber() + " not reachable while responding to tileClicked");
        }
        cream = !cream;
    }

    public static void main(String[] args) {
        try {
            LocateRegistry.createRegistry(GameInterface.rmiRegistryPort);
        } catch (RemoteException ignore) {
            // RMI server already exists
        }

        try {
            Server server = new Server();
            Naming.rebind("//" + GameInterface.defaultIp + ":" + GameInterface.rmiRegistryPort + "/" + GameInterface.defaultServerPath, server);
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