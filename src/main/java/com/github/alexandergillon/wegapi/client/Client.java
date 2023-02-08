package com.github.alexandergillon.wegapi.client;

import com.github.alexandergillon.wegapi.server.Server;
import com.github.alexandergillon.wegapi.server.ServerInterface;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public class Client {
    private int playerNumber;

    private static ServerInterface connectToServer() {
        try {
            return (ServerInterface) Naming.lookup("//127.0.0.1:1099/gameServer");
        } catch (RemoteException e) {
            System.out.printf("RemoteException while connecting to server, %s%n", e.toString());
        } catch (NotBoundException e) {
            System.out.printf("Server is not bound on  while connecting to server, %s%n", e.toString());
        } catch (MalformedURLException ignore) {

        }
        System.exit(1);
        return null;
    }

    public static void main(String[] args) {
        Options options = new Options();
        options.addOption("i", "init", false, "Initialize client.");
        options.addOption("c", "clicked", true, "Index of tile clicked.");
        options.addOption("d", "dragged", true, "Index of dragged tile.");
        options.addOption("t", "target", true, "Index of tile that was dragged onto.");

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption("i")) {
                ServerInterface server = connectToServer();
                server.clientInit();
                System.exit(0);
            }
            else if (cmd.hasOption("c")) {
                int clickedIdx = Integer.parseInt(cmd.getOptionValue("c"));
                ServerInterface server = connectToServer();
                server.tileClicked(clickedIdx, 0);
                System.exit(0);
            }
            else if (cmd.hasOption("d") || cmd.hasOption("t")) {
                if (!(cmd.hasOption("d") && cmd.hasOption("t"))) {
                    System.out.println("One of -d or -t was specified, but not the other.");
                    System.exit(1);
                }
                int draggedFrom = Integer.parseInt(cmd.getOptionValue("d"));
                int draggedTo = Integer.parseInt(cmd.getOptionValue("t"));
                ServerInterface server = connectToServer();
                server.tileDragged(draggedFrom, draggedTo, 0);
                System.exit(0);
            }
            System.out.println("None of -c, -d, or -t were specified (required).");
            System.exit(1);
        } catch (ParseException e) {
            System.out.printf("ParseException, %s%n", e.toString());
        } catch (NumberFormatException e) {
            System.out.printf("Invalid option argument, %s%n", e.toString());
        } catch (RemoteException e) {
            System.out.printf("Encountered RemoteException, %s%n", e.toString());
        }
    }
}
