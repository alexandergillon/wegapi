package com.github.alexandergillon.wegapi.client;

/*
Usage:
    -c<n>: tile n clicked
    -d<n> -t<m>: tile n dragged to m
    -i: init

todo: handle multiple daemon

todo: authentication / access tokens?
 */

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.cli.*;

import com.github.alexandergillon.wegapi.game.GameInterface;

public class Client {
    /**
     * Prints a help message and exits.
     */
    private static void printHelpAndExit() {
        String helpMessage =
            "usage: java -cp wegapi.jar com.github.alexandergillon.wegapi.client.Client [options]\n" +
            "Valid options are as follows (com.github.alexandergillon.wegapi.client.Client is abbreviated to Client, for brevity):\n" +
            "  java -cp wegapi.jar Client -i             Initialize this player (register with the server, and populate game directory)\n" +
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
        options.addOption("i", "init", false, "Initialize this client");
        options.addOption("c", "clicked", true, "Index of tile clicked");
        options.addOption("d", "dragged", true, "Index of dragged tile");
        options.addOption("t", "target", true, "Index of tile that was dragged onto");
        CommandLineParser parser = new DefaultParser();

        try {
            CommandLine cmdline = parser.parse(options, args);

            if (cmdline.hasOption("i")) {
                GameInterface daemon = connectToDaemon();
                daemon.clientInit();
            } else if (cmdline.hasOption("c")) {
                int clickedIdx = Integer.parseInt(cmdline.getOptionValue("c"));
                GameInterface daemon = connectToDaemon();
                daemon.tileClicked(clickedIdx, 0);
            } else if (cmdline.hasOption("d") || cmdline.hasOption("t")) {
                if (!(cmdline.hasOption("d") && cmdline.hasOption("t"))) {
                    printHelpAndExit("One of -d or -t was specified, but not the other.");
                }
                int draggedFrom = Integer.parseInt(cmdline.getOptionValue("d"));
                int draggedTo = Integer.parseInt(cmdline.getOptionValue("t"));
                GameInterface daemon = connectToDaemon();
                daemon.tileDragged(draggedFrom, draggedTo, 0);
            } else {
                printHelpAndExit("None of -c, -d, or -t were specified (required).");
            }
        } catch (ParseException e) {
            printHelpAndExit("ParseException: " + e.toString());
        } catch (NumberFormatException e) {
            printHelpAndExit("Invalid option argument: " + e.toString());
        } catch (RemoteException e) {
            printHelpAndExit("Encountered RemoteException while parsing arguments: " + e.toString());
        }
    }

    /**
     * Connects to the client daemon, returning a remote object that exports the GameInterface interface.
     *
     * @return The client daemon, as a remote object that exports the GameInterface interface
     */
    private static GameInterface connectToDaemon() {
        try {
            return GameInterface.connectToDaemon(GameInterface.defaultIp, GameInterface.rmiRegistryPort);
        } catch (RemoteException e) {
            System.out.printf("RemoteException while connecting to daemon, %s%n", e.toString());
        } catch (NotBoundException e) {
            System.out.printf("Daemon is not bound while connecting, %s%n", e.toString());
            try {
                Arrays.asList(LocateRegistry.getRegistry().list()).forEach(System.out::println);
            } catch (RemoteException eprime) {
                eprime.printStackTrace();
                System.exit(1);
            }
        } catch (MalformedURLException e) {
            System.out.printf("Malformed URL while connecting to daemon, %s%n", e.toString());
        }
        System.exit(1);
        return null;
    }

    public static void main(String[] args) {
        parseArgsAndContactDaemon(args);
    }
}
