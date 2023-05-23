package com.github.alexandergillon.wegapi.server.chess;

import com.github.alexandergillon.wegapi.game.GameServerInterface;
import com.github.alexandergillon.wegapi.game.PlayerInterface;
import com.github.alexandergillon.wegapi.game.PlayerInterface2D;
import com.github.alexandergillon.wegapi.server.BaseServer2D;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Server that runs the chess game. */
public class ChessServer extends BaseServer2D {
    /** Class that encapsulates chess-specific player data. */
    private static class ChessPlayerData {
        private final int playerNumber;
        private PlayerInterface2D player;
        private final ChessPiece.PlayerColor playerColor;

        public ChessPlayerData(int playerNumber, PlayerInterface2D player, ChessPiece.PlayerColor playerColor) {
            this.playerNumber = playerNumber;
            this.player = player;
            this.playerColor = playerColor;
        }

        public int getPlayerNumber() {
            return playerNumber;
        }

        public PlayerInterface2D getPlayer() {
            return player;
        }

        public void setPlayer(PlayerInterface2D player) {
            this.player = player;
        }

        public ChessPiece.PlayerColor getPlayerColor() {
            return playerColor;
        }
    }

    private final HashMap<Integer, ChessPlayerData> players = new HashMap<>(); // maps player number to their data
    private final AtomicInteger playerNumber = new AtomicInteger();
    private final ChessBoard chessBoard;
    private ChessPiece.PlayerColor currentPlayer = ChessPiece.PlayerColor.WHITE;

    /** Creates a new ChessServer object, which exports itself via RMI. */
    public ChessServer() throws RemoteException {
        super(ChessBoard.NUM_ROWS, ChessBoard.NUM_COLS);
        chessBoard = new ChessBoard();  // initializes a chess board to the starting setup of chess
    }

    /**
     * Gets the player color of a player with a certain player number. For now, 0 = white, 1 = black.
     *
     * @param thisPlayerNumber the number of a player in the game
     * @return the player color of that player
     */
    private ChessPiece.PlayerColor getPlayerColor(int thisPlayerNumber) {
        if (thisPlayerNumber % 2 == 0) {
            return ChessPiece.PlayerColor.WHITE;
        } else {
            return ChessPiece.PlayerColor.BLACK;
        }
    }

    @Override
    public void registerPlayer2D(PlayerInterface2D player) {
        int thisPlayerNumber = playerNumber.getAndUpdate(x -> 1-x);
        ChessPiece.PlayerColor playerColor = getPlayerColor(thisPlayerNumber);
        players.put(thisPlayerNumber, new ChessPlayerData(thisPlayerNumber, player, playerColor));
        System.out.println("server: register client received, assigned player #" + thisPlayerNumber + " and color " + ChessPiece.colorToString(playerColor));
        try {
            player.initialize(thisPlayerNumber);
            player.deleteTiles(new ArrayList<>(), PlayerInterface.DeleteTilesMode.DELETE_ALL);
            player.createTiles(chessBoard.toTiles(playerColor), PlayerInterface.CreateTilesMode.CREATE_NEW);
        } catch (RemoteException e) {
            System.out.println("server: player " + thisPlayerNumber + " not reachable while initializing," + e);
        }
    }

    @Override
    public void tileClicked2D(int row, int col, PlayerData2D playerData) {
        System.out.println("player #" + playerData.getPlayerNumber() + " clicked on (" + row + ", " + col + ")");
    }

    @Override
    public void tileDragged2D(int fromRow, int fromCol, int toRow, int toCol, PlayerData2D playerData) {
        System.out.println("player #" + playerData.getPlayerNumber() + " dragged (" + fromRow + ", " + fromCol + ") to (" + toRow + ", " + toCol + ")");
    }

    /**
     * Main function. Starts the server and exports it via RMI.
     *
     * @param args for now, unused
     */
    public static void main(String[] args) {
        try {
            LocateRegistry.createRegistry(GameServerInterface.RMI_REGISTRY_PORT);
        } catch (RemoteException ignore) {
            // RMI server already exists
        }

        try {
            ChessServer server = new ChessServer();
            Naming.rebind("//" + GameServerInterface.DEFAULT_IP + ":" + GameServerInterface.RMI_REGISTRY_PORT + "/" + GameServerInterface.DEFAULT_SERVER_PATH, server);
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
