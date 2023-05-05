package com.github.alexandergillon.wegapi.client;

import com.github.alexandergillon.wegapi.game.GameInterface;
import com.github.alexandergillon.wegapi.server.Server;
import org.apache.commons.cli.*;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.locks.ReentrantLock;

// todo: if lock is held, abort action

public class ClientDaemon extends UnicastRemoteObject implements GameInterface {
    private final Path gameDir;
    private final ReentrantLock actionLock = new ReentrantLock(true);
    private final GameInterface server;

    public ClientDaemon(String userDir) throws RemoteException, MalformedURLException {
        super(0);
        gameDir = Paths.get(userDir);

        GameInterface tempServer;
        try {
            tempServer = GameInterface.connectToServer(GameInterface.defaultIp, GameInterface.rmiRegistryPort);
        } catch (RemoteException e) {
            System.out.printf("RemoteException while connecting to server, %s%n", e.toString());
            System.exit(1);
            tempServer = null;
        } catch (NotBoundException e) {
            System.out.printf("Server is not bound while connecting, %s%n", e.toString());
            System.exit(1);
            tempServer = null;
        }
        server = tempServer;
    }

    /**
     * Prints a help message and exits.
     */
    private static void printHelpAndExit() {
        System.out.print("usage: java -cp wegapi.jar com.github.alexandergillon.wegapi.client.ClientDaemon -d <DIR>        Start the client daemon, with DIR as the game directory\n");
        System.exit(1);
    }

    /**
     * Prints an error message, followed by a help message, then exits.
     */
    private static void printHelpAndExit(String errorMessage) {
        System.out.println(errorMessage);
        printHelpAndExit();
    }

    private static String parseArgs(String[] args) {
        Options options = new Options();
        options.addOption(Option.builder("d").longOpt("dir").hasArg().required().desc("Directory to run the game in").build());
        CommandLineParser parser = new DefaultParser();

        try {
            CommandLine cmdline = parser.parse(options, args);
            return cmdline.getOptionValue("d");
        } catch (ParseException e) {
            printHelpAndExit("ParseException: " + e.toString());
        } catch (NumberFormatException e) {
            printHelpAndExit("Invalid option argument: " + e.toString());
        }
        return null;
    }

    @Override
    public void clientInit() {
        System.out.println("daemon: client init received");
        try {
            server.clientInit();
        } catch (RemoteException e) {
            System.out.printf("RemoteException while forwarding clientInit to server, %s%n", e.toString());
        }
    }

    @Override
    public void tileClicked(int tile, int player) {
        System.out.printf("daemon: tile clicked: %d by player %d%n", tile, player);
        try {
            server.tileClicked(tile, player);
        } catch (RemoteException e) {
            System.out.printf("RemoteException while forwarding tileClicked to server, %s%n", e.toString());
        }
    }

    @Override
    public void tileDragged(int fromTile, int toTile, int player) {
        System.out.printf("daemon: tile dragged: from %d to %d by player %d%n", fromTile, toTile, player);
        try {
            server.tileDragged(fromTile, toTile, player);
        } catch (RemoteException e) {
            System.out.printf("RemoteException while forwarding tileDragged to server, %s%n", e.toString());
        }
    }

    public static void main(String[] args) {
        String gameDir = parseArgs(args);
        ClientDaemon daemon = null;
        try {
            daemon = new ClientDaemon(gameDir);
        } catch (RemoteException e) {
            System.out.printf("RemoteException while instantiating daemon: %s%n", e.toString());
            System.exit(1);
        } catch (MalformedURLException e) {
            System.out.printf("Malformed URL while connecting to server: %s%n", e.toString());
            System.exit(1);
        }

        try {
            LocateRegistry.createRegistry(GameInterface.rmiRegistryPort);
        } catch (RemoteException ignore) {
            // RMI server already exists
        }

        try {
            GameInterface.launchRMI(daemon, GameInterface.defaultIp, GameInterface.rmiRegistryPort, GameInterface.defaultDaemonPath);
        } catch (RemoteException e) {
            System.out.printf("Failed to rebind daemon, %s%n", e.toString());
            System.exit(1);
        } catch (MalformedURLException e) {
            System.out.printf("Malformed URL: %s%n", e.toString());
            System.exit(1);
        }
        System.out.println("Daemon ready!");
    }
}
