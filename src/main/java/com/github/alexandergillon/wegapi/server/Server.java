// java -cp wegapi.jar com.github.alexandergillon.wegapi.server.Server
package com.github.alexandergillon.wegapi.server;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

import com.github.alexandergillon.wegapi.game_action.GameAction;

public class Server extends UnicastRemoteObject implements ServerInterface {
    public Server() throws RemoteException {
        super(0);
    }

    @Override
    public ArrayList<GameAction> clientInit() {
        System.out.println("client init received");
        return new ArrayList<>();
    }

    @Override
    public ArrayList<GameAction> tileClicked(int tile, int player) {
        System.out.printf("tile clicked: %d by player %d%n", tile, player);
        return new ArrayList<>();
    }

    @Override
    public ArrayList<GameAction> tileDragged(int fromTile, int toTile, int player) {
        System.out.printf("tile dragged: from %d to %d by player %d%n", fromTile, toTile, player);
        return new ArrayList<>();
    }

    public static void main(String[] args) {
        try {
            LocateRegistry.createRegistry(1099);
        } catch (RemoteException ignore) {
            // RMI server already exists
        }
        try {
            Server server = new Server();
            Naming.rebind("//127.0.0.1:1099/gameServer", server);
        } catch (RemoteException e) {
            System.out.printf("Failed to rebind server, %s%n", e.toString());
        } catch (MalformedURLException ignore) {

        }
        System.out.println("Server ready!");
    }
}