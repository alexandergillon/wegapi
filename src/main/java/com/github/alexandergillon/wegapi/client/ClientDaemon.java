package com.github.alexandergillon.wegapi.client;

import com.github.alexandergillon.wegapi.game.DaemonInterface;
import com.github.alexandergillon.wegapi.game.GameInterface;
import com.github.alexandergillon.wegapi.game.PlayerInterface;
import com.github.alexandergillon.wegapi.server.Server;
import org.apache.commons.cli.*;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.locks.ReentrantLock;

// todo: if lock is held, abort action

public class ClientDaemon extends UnicastRemoteObject implements DaemonInterface {
    private final Path gameDir;
    private final ReentrantLock actionLock = new ReentrantLock(true);
    private final GameInterface server;

    /**
     * Creates a client daemon that uses userDir as its game directory.
     *
     * @param userDir path to a directory to run the game in
     * @throws RemoteException propagates from UnicastRemoteObject constructor
     */
    public ClientDaemon(String userDir) throws RemoteException {
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
        } catch (MalformedURLException e) {
            System.out.printf("Malformed URL while connecting to server, %s%n", e.toString());
            System.exit(1);
            tempServer = null;
        }
        server = tempServer;
    }

    /**
     * This function is called when the client has double-clicked a tile. For now, forwards
     * the request to the server.
     *
     * todo: concurrency control
     *
     * @param tile the index of the tile that the player clicked
     */
    @Override
    public void tileClicked(int tile) {
        System.out.printf("daemon: tile clicked: %d%n", tile);
        try {
            server.tileClicked(tile, 0);
        } catch (RemoteException e) {
            System.out.printf("RemoteException while forwarding tileClicked to server, %s%n", e.toString());
        }
    }

    /**
     * This function is called when the client drags one tile to another. For now, forwards
     * the request to the server.
     *
     * todo: concurrency control
     *
     * @param fromTile the index of the tile that was dragged
     * @param toTile the index of the tile that the tile was dragged to
     */
    @Override
    public void tileDragged(int fromTile, int toTile) {
        System.out.printf("daemon: tile dragged: from %d to %d%n", fromTile, toTile);
        try {
            server.tileDragged(fromTile, toTile, 0);
        } catch (RemoteException e) {
            System.out.printf("RemoteException while forwarding tileDragged to server, %s%n", e.toString());
        }
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

    /**
     * Parses command line args for the directory to run the game in, and returns its path. \n \n
     *
     * On error, prints a message and exits.
     *
     * @param args the args parameter that was passed to main()
     * @return the path to a directory, supplied on the command line, to run the game in
     */
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

    /**
     * Main function. Parses command-line arguments and launches a daemon RMI service
     * running in the directory specified by the user.
     */
    public static void main(String[] args) {
        String gameDir = parseArgs(args);
        ClientDaemon daemon = null;
        try {
            daemon = new ClientDaemon(gameDir);
        } catch (RemoteException e) {
            System.out.printf("RemoteException while instantiating daemon: %s%n", e.toString());
            System.exit(1);
        }

        try {
            LocateRegistry.createRegistry(DaemonInterface.rmiRegistryPort);
        } catch (RemoteException ignore) {
            // RMI server already exists
        }

        try {
            Naming.rebind("//" + DaemonInterface.defaultIp + ":" + DaemonInterface.rmiRegistryPort + "/" + DaemonInterface.defaultDaemonPath, daemon);
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
