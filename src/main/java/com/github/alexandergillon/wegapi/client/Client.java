package com.github.alexandergillon.wegapi.client;

/*
Usage:
    -c<n>: tile n clicked
    -d<n> -t<m>: tile n dragged to m
    -i: init

todo: handle multiple daemon

todo: authentication / access tokens?

todo: ensure tile program has finished before doing anything?
 */

import com.github.alexandergillon.wegapi.game.DaemonInterface;
import org.apache.commons.cli.*;

import java.io.*;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.Arrays;

/**
 * Class which contains a program that contacts the Client daemon, to inform it that the user has made an action.
 * Invoked by C++ code in the tile.exe executable, by running Java at the command line. See ClientDaemon.java
 * for more information about how communication works, and what the daemon achieves.
 */
public class Client {
    /**
     * Prints a help message and exits.
     */
    private static void printHelpAndExit() {
        String helpMessage =
            "usage: java -cp wegapi.jar com.github.alexandergillon.wegapi.client.Client [options]\n" +
            "Valid options are as follows (com.github.alexandergillon.wegapi.client.Client is abbreviated to Client, for brevity):\n" +
            "  java -cp wegapi.jar Client -c<n>          This player double-clicked on tile n\n" +
            "  java -cp wegapi.jar Client -d<n> -t<m>    This player dragged tile n to tile m\n";

        System.out.print(helpMessage);
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
     * Parses command-line arguments, and makes appropriate requests to the client daemon.
     *
     * @param args the arguments to parse, from main
     */
    private static void parseArgsAndContactDaemon(String[] args) {
        Options options = new Options();
        options.addOption("c", "clicked", true, "Index of tile clicked");
        options.addOption("d", "dragged", true, "Index of dragged tile");
        options.addOption("t", "target", true, "Index of tile that was dragged onto");
        CommandLineParser parser = new DefaultParser();

        try {
            CommandLine cmdline = parser.parse(options, args);

            if (cmdline.hasOption("c")) {
                int clickedIdx = Integer.parseInt(cmdline.getOptionValue("c"));
                DaemonInterface daemon = connectToDaemon();
                daemon.tileClicked(clickedIdx);
            } else if (cmdline.hasOption("d") || cmdline.hasOption("t")) {
                if (!(cmdline.hasOption("d") && cmdline.hasOption("t"))) {
                    printHelpAndExit("One of -d or -t was specified, but not the other.");
                }
                int draggedFrom = Integer.parseInt(cmdline.getOptionValue("d"));
                int draggedTo = Integer.parseInt(cmdline.getOptionValue("t"));
                DaemonInterface daemon = connectToDaemon();
                daemon.tileDragged(draggedFrom, draggedTo);
            } else {
                printHelpAndExit("None of -c, -d, or -t were specified (required).");
            }
        } catch (ParseException e) {
            printHelpAndExit("ParseException: " + e);
        } catch (NumberFormatException e) {
            printHelpAndExit("Invalid option argument: " + e);
        } catch (RemoteException e) {
            printHelpAndExit("Encountered RemoteException while parsing arguments: " + e);
        }
    }

    /**
     * Reads the daemon number from a file ([current directory]/GAME_DATA_DIR_NAME/DAEMON_NUMBER_FILENAME). This
     * tells the client which daemon to connect to, as there may be more than one running on the machine. <br> <br>
     *
     * The format of this file is a magic string, found at DaemonInterface.DAEMON_NUMBER_MAGIC, followed by an integer
     * (as raw binary, not ASCII), that was written by a DataOutputStream and can be read by a DataInputStream.
     *
     * @return The daemon number of the daemon watching over this game.
     */
    private static int readDaemonNumber() {
        Path gameDataDirPath = Paths.get(".").toAbsolutePath().normalize().resolve(DaemonInterface.GAME_DATA_DIR_NAME);  // todo: pass path from c++
        Util.checkExists(gameDataDirPath, true);
        Path daemonNumberPath = gameDataDirPath.resolve(DaemonInterface.DAEMON_NUMBER_FILENAME);
        File daemonNumberFile = new File(daemonNumberPath.toString());

        try (FileInputStream daemonNumberStream = new FileInputStream(daemonNumberFile)) {
            byte[] magic = DaemonInterface.DAEMON_NUMBER_MAGIC.getBytes(StandardCharsets.US_ASCII);
            byte[] magicRead = daemonNumberStream.readNBytes(magic.length);
            if (!Arrays.equals(magicRead, magic)) {
                System.out.println("Error: magic at the start of daemonnumber.wegapi is not as expected");
                System.exit(1);
            }

            DataInputStream dataInputStream = new DataInputStream(daemonNumberStream);
            return dataInputStream.readInt();
        } catch (FileNotFoundException e) {
            System.out.printf("daemonnumber file could not be found, %s%n", e);
            System.exit(1);
        } catch (IOException e) {
            System.out.printf("IOException while reading/closing daemonnumber, %s%n", e);
            System.exit(1);
        }
        return -1;  // for the compiler
    }

    /**
     * Connects to the client daemon, returning a remote object that exports the DaemonInterface interface.
     *
     * @return The client daemon, as a remote object that exports the DaemonInterface interface
     */
    private static DaemonInterface connectToDaemon() {
        try {
            int daemonNumber = readDaemonNumber();
            System.out.println("got daemon number: " + daemonNumber);
            return DaemonInterface.connectToDaemon(DaemonInterface.DEFAULT_IP, DaemonInterface.RMI_REGISTRY_PORT, daemonNumber);
        } catch (RemoteException e) {
            System.out.printf("RemoteException while connecting to daemon, %s%n", e);
        } catch (NotBoundException e) {
            System.out.printf("Daemon is not bound while connecting, %s%n", e);
            try {
                Arrays.asList(LocateRegistry.getRegistry().list()).forEach(System.out::println);
            } catch (RemoteException eprime) {
                eprime.printStackTrace();
                System.exit(1);
            }
        } catch (MalformedURLException e) {
            System.out.printf("Malformed URL while connecting to daemon, %s%n", e);
        }
        System.exit(1);
        return null;
    }

    public static void main(String[] args) {
        parseArgsAndContactDaemon(args);
    }
}
