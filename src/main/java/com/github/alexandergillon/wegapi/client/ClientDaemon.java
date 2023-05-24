package com.github.alexandergillon.wegapi.client;

import com.github.alexandergillon.wegapi.game.*;
import org.apache.commons.cli.*;

import java.io.*;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantLock;

// todo: if lock is held, abort action

/**
 * Class which contains a daemon program, that manages a game. This program is intended to be long-running, and
 * handles user interaction and processes commands from the server. <br> <br>
 *
 * When the user makes an action (such as double clicking a tile, or dragging one tile to another), they use the
 * Client class to contact this daemon. This daemon then forwards the request to the server, and processes any
 * commands it has in response. <br> <br>
 *
 * Communication between parties is achieved as follows: <br> <br>
 *
 *   - A Client communicates with a ClientDaemon via the DaemonInterface, over RMI on the local machine. Strictly,
 *     this does not need to be local (the daemon could run on a remote machine), but this doesn't make much sense as
 *     it would introduce unnecessary latency. <br>
 *   - A ClientDaemon communicates with a game server via the GameServerInterface, over RMI. The server may be on a
 *     local or remote machine. <br>
 *   - A game server communicates with a ClientDaemon via the PlayerInterface, over RMI. The server uses a remote
 *     object sent by the daemon on each call to respond, and these are essentially callbacks. <br> <br>
 *
 * It should hopefully be obvious that we need some sort of daemon program that is listening for changes from the
 * server. For example, in a turn-based game, some other player may make a move, and we need that change to be
 * reflected on this player. However, it might not be obvious why we want the player's tile.exe to contact this
 * daemon rather than the server directly. This is done so that we can have some control over concurrency. What should
 * happen if the user clicked a tile, but the server was already in the process of changing the game state? A 'naive'
 * Client program would forward the request regardless, but perhaps we might want to prevent that action from
 * occurring and perhaps tell the user that things were changing right as they clicked something. For certain games
 * we may indeed want to just forward the request regardless. A daemon allows us to choose which behavior we want,
 * and when.
 *
 * todo: concurrency control and options for servers to set it
 */
public class ClientDaemon extends UnicastRemoteObject implements DaemonInterface, PlayerInterface {
    private static final String PLAYER_DATA_FILENAME = "playerdata.wegapi";
    private static final String PLAYER_DATA_MAGIC = "WEGAPIPLAYERDATA";

    // 0.1.0
    private static final int MAJOR_VERSION_NUMBER = 0;
    private static final int MINOR_VERSION_NUMBER = 1;
    private static final int PATCH_VERSION_NUMBER = 0;

    private final Path gameDir;
    private int playerNumber = -1;
    private boolean gameOver = false; // todo: use

    private final ReentrantLock actionLock = new ReentrantLock(true);
    private final GameServerInterface server;

    /**
     * Creates a client daemon that uses userDir as its game directory.
     *
     * @param userDir path to a directory to run the game in
     * @throws RemoteException propagates from UnicastRemoteObject constructor
     */
    public ClientDaemon(String userDir) throws RemoteException {
        super(0);
        gameDir = Paths.get(userDir);

        GameServerInterface tempServer;
        try {
            tempServer = GameServerInterface.connectToServer(GameServerInterface.DEFAULT_IP, GameServerInterface.RMI_REGISTRY_PORT);
        } catch (RemoteException e) {
            System.out.printf("RemoteException while connecting to server, %s%n", e);
            System.exit(1);
            tempServer = null;
        } catch (NotBoundException e) {
            System.out.printf("Server is not bound while connecting, %s%n", e);
            System.exit(1);
            tempServer = null;
        } catch (MalformedURLException e) {
            System.out.printf("Malformed URL while connecting to server, %s%n", e);
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
            server.tileClicked(tile, new PlayerData(playerNumber, this));
        } catch (RemoteException e) {
            System.out.printf("RemoteException while forwarding tileClicked to server, %s%n", e);
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
            server.tileDragged(fromTile, toTile, new PlayerData(playerNumber, this));
        } catch (RemoteException e) {
            System.out.printf("RemoteException while forwarding tileDragged to server, %s%n", e);
        }
    }

    @Override
    public void initialize(int playerNumber) {
        System.out.println("daemon: initializing, got player #" + playerNumber);
        this.playerNumber = playerNumber;

        Path gameDataDirPath = gameDir.resolve(GAME_DATA_DIR_NAME);
        Util.checkExists(gameDataDirPath, true);
        Path playerDataPath = gameDataDirPath.resolve(PLAYER_DATA_FILENAME);
        File playerData = new File(playerDataPath.toString());

        try (FileOutputStream playerDataStream = new FileOutputStream(playerData)) {
            byte[] magic = PLAYER_DATA_MAGIC.getBytes(StandardCharsets.US_ASCII);
            playerDataStream.write(magic);

            DataOutputStream dataOutputStream = new DataOutputStream(playerDataStream);
            dataOutputStream.writeInt(MAJOR_VERSION_NUMBER);
            dataOutputStream.writeInt(MINOR_VERSION_NUMBER);
            dataOutputStream.writeInt(PATCH_VERSION_NUMBER);

            dataOutputStream.writeInt(playerNumber);
        } catch (FileNotFoundException e) {
            System.out.printf("playerdata file could not be created, %s%n", e);
            System.exit(1);
        } catch (IOException e) {
            System.out.printf("IOException while writing/closing playerdata, %s%n", e);
            System.exit(1);
        }
    }

    @Override
    public void displayMessage(String message, boolean error) {
        if (error) {
            System.out.println("daemon: received ERROR message from server: " + message);
        } else {
            System.out.println("daemon: received message from server: " + message);
        }
    }

    private Process launchCreateTiles(Path gameDataDirPath, String tileData, CreateTilesMode mode) {
        Path createTilesExePath = gameDataDirPath.resolve("create_tiles.exe");

        try {
            switch (mode) {
                case CREATE:
                    return new ProcessBuilder(createTilesExePath.toString(), gameDir.toString(), tileData)
                            .directory(new File(gameDataDirPath.toString()))
                            .inheritIO()
                            .start();
                case CREATE_NEW:
                    return new ProcessBuilder(createTilesExePath.toString(), gameDir.toString(), tileData, "-n")
                            .directory(new File(gameDataDirPath.toString()))
                            .inheritIO()
                            .start();
                case OVERWRITE_EXISTING:
                    return new ProcessBuilder(createTilesExePath.toString(), gameDir.toString(), tileData, "-o")
                            .directory(new File(gameDataDirPath.toString()))
                            .inheritIO()
                            .start();
            }
        } catch (IOException e) {
            System.out.printf("failed to create create_tiles process, %s%n", e);
            System.exit(1);
        }

        return null; // for the compiler
    }

    @Override
    public void createTiles(ArrayList<Tile> tiles, CreateTilesMode mode) {
        System.out.println("daemon: creating tiles...");
        // todo: use installed binaries in program files
        // todo: concurrency
        Path gameDataDirPath = gameDir.resolve(GAME_DATA_DIR_NAME);
        Util.checkExists(gameDataDirPath, true);

        ArrayList<String> stringifiedTiles = new ArrayList<>();
        for (Tile tile : tiles) {
            String stringifiedTile;
            if (tile.getTileName() == null) {
                stringifiedTile = String.join(":", Integer.toString(tile.getIndex()), tile.getIconName());
            } else {
                stringifiedTile = String.join(":", Integer.toString(tile.getIndex()), tile.getIconName(), tile.getTileName());
            }
            stringifiedTiles.add(stringifiedTile);
        }
        String tileData = String.join(",", stringifiedTiles);
        System.out.println("tiledata: " + tileData);

        Process p = launchCreateTiles(gameDataDirPath, tileData, mode);

        // todo: fail more gracefully
        try {
            int exitCode = p.waitFor();
            if (exitCode != 0) {
                System.out.println("create_tiles.exe failed");
                System.exit(1);
            }
        } catch (InterruptedException e) {
            // this shouldn't happen
            System.out.printf("interrupted while waiting for create_tiles process, %s%n", e);
            System.exit(1);
        }
    }

    private Process launchDeleteTiles(Path gameDataDirPath, String tileData, DeleteTilesMode mode) {
        Path deleteTilesExePath = gameDataDirPath.resolve("delete_tiles.exe");

        try {
            switch (mode) {
                case DELETE:
                    return new ProcessBuilder(deleteTilesExePath.toString(), gameDir.toString(), tileData)
                            .directory(new File(gameDataDirPath.toString()))
                            .inheritIO()
                            .start();
                case DELETE_EXISTING:
                    return new ProcessBuilder(deleteTilesExePath.toString(), gameDir.toString(), tileData, "-e")
                            .directory(new File(gameDataDirPath.toString()))
                            .inheritIO()
                            .start();
                case DELETE_ALL:
                    // delete_tiles needs some data here to parse args correctly, but it's ignored
                    return new ProcessBuilder(deleteTilesExePath.toString(), gameDir.toString(), "0", "-a")
                            .directory(new File(gameDataDirPath.toString()))
                            .inheritIO()
                            .start();
            }
        } catch (IOException e) {
            System.out.printf("failed to create delete_tiles process, %s%n", e);
            System.exit(1);
        }

        return null; // for the compiler
    }

    @Override
    public void deleteTiles(ArrayList<Integer> tileIndices, DeleteTilesMode mode) {
        // todo: handle null tileindices
        System.out.println("daemon: deleting tiles...");
        Path gameDataDirPath = gameDir.resolve(GAME_DATA_DIR_NAME);
        Util.checkExists(gameDataDirPath, true);

        // tileIndices converted to strings
        String[] indexStrings = tileIndices.stream().map(x -> Integer.toString(x)).toArray(String[]::new);
        String tileData = String.join(",", indexStrings);
        System.out.println("tiledata: " + tileData);

        Process p = launchDeleteTiles(gameDataDirPath, tileData, mode);

        // todo: fail more gracefully
        try {
            int exitCode = p.waitFor();
            if (exitCode != 0) {
                System.out.println("delete_tiles.exe failed");
                System.exit(1);
            }
        } catch (InterruptedException e) {
            // this shouldn't happen
            System.out.printf("interrupted while waiting for delete_tiles process, %s%n", e);
            System.exit(1);
        }
    }

    @Override
    public void gameOver(boolean win) {
        gameOver = true;
    }

    /**
     * Registers this player with the server.
     */
    private void registerWithServer() {
        try {
            server.registerPlayer(this);
        } catch (RemoteException e) {
            System.out.printf("RemoteException while registering with server, %s%n", e);
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
     *
     * @param errorMessage error message to print
     */
    private static void printHelpAndExit(String errorMessage) {
        System.out.println(errorMessage);
        printHelpAndExit();
    }

    /**
     * Parses command line args for the directory to run the game in, and returns its path. <br> <br>
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
            printHelpAndExit("ParseException: " + e);
        } catch (NumberFormatException e) {
            printHelpAndExit("Invalid option argument: " + e);
        }
        return null;
    }

    private static int getDaemonNumber() {
        while (true) {
            int numberToTry = ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE);
            String url = Util.buildDaemonRMIPath(numberToTry);
            try {
                Naming.lookup(url);
            } catch (NotBoundException expected) {
                // this url is not in use: we can use it
                return numberToTry;
            } catch (RemoteException e) {
                System.out.printf("RemoteException while looking for daemon url, %s%n", e);
            } catch (MalformedURLException e) {
                System.out.printf("Malformed URL while looking for daemon url, %s%n", e);
            }
        }
    }

    private void writeDaemonNumber(int daemonNumber) {
        System.out.println("writing daemon number: " + daemonNumber);
        Path gameDataDirPath = gameDir.resolve(GAME_DATA_DIR_NAME);
        Util.checkExists(gameDataDirPath, true);
        Path daemonNumberPath = gameDataDirPath.resolve(DAEMON_NUMBER_FILENAME);
        File daemonNumberFile = new File(daemonNumberPath.toString());

        try (FileOutputStream daemonNumberStream = new FileOutputStream(daemonNumberFile)) {
            byte[] magic = DAEMON_NUMBER_MAGIC.getBytes(StandardCharsets.US_ASCII);
            daemonNumberStream.write(magic);

            DataOutputStream dataOutputStream = new DataOutputStream(daemonNumberStream);
            dataOutputStream.writeInt(daemonNumber);
        } catch (FileNotFoundException e) {
            System.out.printf("daemonnumber file could not be created, %s%n", e);
            System.exit(1);
        } catch (IOException e) {
            System.out.printf("IOException while writing/closing daemonnumber, %s%n", e);
            System.exit(1);
        }
    }

    /**
     * Main function. Parses command-line arguments and launches a daemon RMI service
     * running in the directory specified by the user.
     */
    public static void main(String[] args) {
        // todo: search for in progress game
        String gameDir = parseArgs(args);
        ClientDaemon daemon = null;
        try {
            daemon = new ClientDaemon(gameDir);
        } catch (RemoteException e) {
            System.out.printf("RemoteException while instantiating daemon: %s%n", e);
            System.exit(1);
        }

        try {
            LocateRegistry.createRegistry(DaemonInterface.RMI_REGISTRY_PORT);
        } catch (RemoteException ignore) {
            // RMI registry already exists
        }

        try {
            int daemonNumber = getDaemonNumber();
            daemon.writeDaemonNumber(daemonNumber);
            Naming.bind(Util.buildDaemonRMIPath(daemonNumber), daemon);
        } catch (RemoteException e) {
            System.out.printf("Failed to bind daemon, %s%n", e);
            System.exit(1);
        } catch (MalformedURLException e) {
            System.out.printf("Malformed URL: %s%n", e);
            System.exit(1);
        } catch (AlreadyBoundException e) {
            System.out.printf("Daemon URL already bound: %s%n", e);
            System.exit(1);
        }
        daemon.registerWithServer();
        System.out.println("Daemon ready!");
    }
}
