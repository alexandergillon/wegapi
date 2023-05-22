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

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.Arrays;

import org.apache.commons.cli.*;

import com.github.alexandergillon.wegapi.game.DaemonInterface;

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
     * Connects to the client daemon, returning a remote object that exports the DaemonInterface interface.
     *
     * @return The client daemon, as a remote object that exports the DaemonInterface interface
     */
    private static DaemonInterface connectToDaemon() {
        try {
            return DaemonInterface.connectToDaemon(DaemonInterface.defaultIp, DaemonInterface.rmiRegistryPort);
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
