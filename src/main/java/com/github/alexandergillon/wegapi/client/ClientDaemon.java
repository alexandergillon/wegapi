package com.github.alexandergillon.wegapi.client;

import com.github.alexandergillon.wegapi.game.GameInterface;
import com.github.alexandergillon.wegapi.game.PlayerInterface;
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

public class ClientDaemon extends UnicastRemoteObject implements GameInterface, PlayerInterface {
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
     * This function is not likely to be called by Client for a while.
     */
    @Override
    public void clientInit() {
        System.out.println("daemon: client init received");
        try {
            server.clientInit();
        } catch (RemoteException e) {
            System.out.printf("RemoteException while forwarding clientInit to server, %s%n", e.toString());
        }
    }

    /**
     * This function is called when the client has double-clicked a tile. For now, forwards
     * the request to the server.
     *
     * todo: concurrency control
     *
     * @param tile the index of the tile that the player clicked
     * @param player which player clicked the tile
     */
    @Override
    public void tileClicked(int tile, int player) {
        System.out.printf("daemon: tile clicked: %d by player %d%n", tile, player);
        try {
            server.tileClicked(tile, player);
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
     * @param player the player who dragged the tile
     */
    @Override
    public void tileDragged(int fromTile, int toTile, int player) {
        System.out.printf("daemon: tile dragged: from %d to %d by player %d%n", fromTile, toTile, player);
        try {
            server.tileDragged(fromTile, toTile, player);
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
