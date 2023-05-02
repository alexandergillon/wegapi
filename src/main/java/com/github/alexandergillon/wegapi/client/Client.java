package com.github.alexandergillon.wegapi.client;

/*
Usage:
    -c<n>: tile n clicked
    -d<n> -t<m>: tile n dragged to m
    -i: init

todo: authentication / access tokens?
 */

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;

import org.apache.commons.cli.*;

import com.github.alexandergillon.wegapi.game.game_action.GameAction;
import com.github.alexandergillon.wegapi.game.GameInterface;

public class Client {
    private int playerNumber; // todo: set up

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
     * Parses command-line arguments, and makes appropriate requests to the server.
     *
     * @param args the arguments to parse, from main
     * @return a list of game actions to perform, based on the response from the server
     */
    private static ArrayList<GameAction> parseArgsAndContactServer(String[] args) {
        Options options = new Options();
        options.addOption("i", "init", false, "Initialize this client");
        options.addOption("c", "clicked", true, "Index of tile clicked");
        options.addOption("d", "dragged", true, "Index of dragged tile");
        options.addOption("t", "target", true, "Index of tile that was dragged onto");
        CommandLineParser parser = new DefaultParser();

        try {
            CommandLine cmdline = parser.parse(options, args);

            if (cmdline.hasOption("i")) {
                GameInterface server = connectToServer();
                return server.clientInit();
            } else if (cmdline.hasOption("c")) {
                int clickedIdx = Integer.parseInt(cmdline.getOptionValue("c"));
                GameInterface server = connectToServer();
                return server.tileClicked(clickedIdx, 0);
            } else if (cmdline.hasOption("d") || cmdline.hasOption("t")) {
                if (!(cmdline.hasOption("d") && cmdline.hasOption("t"))) {
                    printHelpAndExit("One of -d or -t was specified, but not the other.");
                }
                int draggedFrom = Integer.parseInt(cmdline.getOptionValue("d"));
                int draggedTo = Integer.parseInt(cmdline.getOptionValue("t"));
                GameInterface server = connectToServer();
                return server.tileDragged(draggedFrom, draggedTo, 0);
            } else {
                printHelpAndExit("None of -c, -d, or -t were specified (required).");
                return null;
            }
        } catch (ParseException e) {
            printHelpAndExit("ParseException: " + e.toString());
        } catch (NumberFormatException e) {
            printHelpAndExit("Invalid option argument: " + e.toString());
        } catch (RemoteException e) {
            printHelpAndExit("Encountered RemoteException while parsing arguments: " + e.toString());
        }
        return null;
    }

    /**
     * Connects to the server, returning a remote object that exports the ServerInterface interface.
     *
     * @return The server, as a remote object that exports the ServerInterface interface
     */
    private static GameInterface connectToServer() {
        try {
            return (GameInterface) Naming.lookup("//127.0.0.1:1099/gameServer");
        } catch (RemoteException e) {
            System.out.printf("RemoteException while connecting to server, %s%n", e.toString());
        } catch (NotBoundException e) {
            System.out.printf("Server is not bound while connecting to server, %s%n", e.toString());
        } catch (MalformedURLException e) {
            System.out.printf("Malformed URL while connecting to server, %s%n", e.toString());
        }
        System.exit(1);
        return null;
    }

    public static void main(String[] args) {
        ArrayList<GameAction> actions = parseArgsAndContactServer(args);
    }
}
