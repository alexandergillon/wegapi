// java -cp wegapi.jar com.github.alexandergillon.wegapi.server.Server
package com.github.alexandergillon.wegapi.server;

import com.github.alexandergillon.wegapi.game.GameInterface;

import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

public class Server extends UnicastRemoteObject implements GameInterface {
    public Server() throws RemoteException {
        super(0);
    }

    @Override
    public void clientInit() {
        System.out.println("server: client init received");
    }

    @Override
    public void tileClicked(int tile, int player) {
        System.out.printf("server: tile clicked: %d by player %d%n", tile, player);
    }

    @Override
    public void tileDragged(int fromTile, int toTile, int player) {
        System.out.printf("server: tile dragged: from %d to %d by player %d%n", fromTile, toTile, player);
    }

    public static void main(String[] args) {
        try {
            LocateRegistry.createRegistry(GameInterface.rmiRegistryPort);
        } catch (RemoteException ignore) {
            // RMI server already exists
        }

        try {
            Server server = new Server();
            GameInterface.launchRMI(server, GameInterface.defaultIp, GameInterface.rmiRegistryPort, GameInterface.defaultServerPath);
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