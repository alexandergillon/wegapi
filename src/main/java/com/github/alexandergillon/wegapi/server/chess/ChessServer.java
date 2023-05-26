package com.github.alexandergillon.wegapi.server.chess;

import com.github.alexandergillon.wegapi.game.*;
import com.github.alexandergillon.wegapi.server.BaseServer2D;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;

// todo: disconnections and before someone has joined

/** Server that runs the chess game. */
public class ChessServer extends BaseServer2D {
    /** Class that encapsulates chess-specific player data. */
    static class ChessPlayerData {
        private final int playerNumber;
        private PlayerInterface2D player;  // shouldn't change, but maybe in the future, on disconnect
        private final ChessPiece.PlayerColor playerColor;

        private TileCoordinate highlightedTile = null;  // todo: rename to selected

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

        public boolean hasHighlightedTile() {
            return highlightedTile != null;
        }

        public TileCoordinate getHighlightedTile() {
            return highlightedTile;
        }

        public void setHighlightedTile(TileCoordinate highlightedTile) {
            this.highlightedTile = highlightedTile;
        }

        @Override
        public String toString() {
            return highlightedTile == null ? String.format("ChessPlayerData(player #%d, %s, with nothing selected)", playerNumber, playerColor)
            : String.format("ChessPlayerData(player #%d, %s, with (%d, %d) selected", playerNumber, playerColor, highlightedTile.getRow(), highlightedTile.getCol());
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
        ChessPlayerData chessPlayerData = new ChessPlayerData(thisPlayerNumber, player, playerColor);
        players.put(thisPlayerNumber, chessPlayerData);
        System.out.println("server: register client received, assigned player #" + thisPlayerNumber + " and color " + ChessPiece.colorToString(playerColor));
        try {
            player.initialize(thisPlayerNumber);
            player.deleteTiles(new ArrayList<>(), PlayerInterface.DeleteTilesMode.DELETE_ALL);
            player.createTiles(chessBoard.toTiles(chessPlayerData), PlayerInterface.CreateTilesMode.CREATE_NEW);
        } catch (RemoteException e) {
            System.out.println("server: player " + thisPlayerNumber + " not reachable while initializing," + e);
        }
    }

    private void deselectAndSelectOther(int row, int col, ChessPlayerData clickingPlayer, ChessPiece clickedPiece) {
        System.out.println("deselectAndSelectOther()");

        if (clickedPiece == null) {
            clickingPlayer.setHighlightedTile(null);
        } else {
            clickingPlayer.setHighlightedTile(new TileCoordinate(row, col));
        }
    }

    private void updateOtherPlayer(int fromRow, int fromCol, int toRow, int toCol, ChessPlayerData movingPlayer) {
        ChessPlayerData otherPlayer = players.get(1 - movingPlayer.getPlayerNumber());
        if (!otherPlayer.hasHighlightedTile()) return;

        int highlightedRow = otherPlayer.getHighlightedTile().getRow();
        int highlightedCol = otherPlayer.getHighlightedTile().getCol();

        if (highlightedRow == fromRow && highlightedCol == fromCol) {
            otherPlayer.setHighlightedTile(null);
        } else if (highlightedRow == toRow && highlightedCol == toCol) {
            otherPlayer.setHighlightedTile(null);
        }
    }

    private void tryMove(int fromRow, int fromCol, int toRow, int toCol, ChessPlayerData movingPlayer) {
        System.out.println("tryMove()");
        try {
            if (currentPlayer != movingPlayer.getPlayerColor()) {
                movingPlayer.getPlayer().displayMessage("Not your turn.", true);
                movingPlayer.setHighlightedTile(null);
            } else {
                if (chessBoard.tryMove(fromRow, fromCol, toRow, toCol)) {
                    movingPlayer.setHighlightedTile(null);
                    updateOtherPlayer(fromRow, fromCol, toRow, toCol, movingPlayer);

                    if (currentPlayer == ChessPiece.PlayerColor.WHITE) currentPlayer = ChessPiece.PlayerColor.BLACK;
                    else currentPlayer = ChessPiece.PlayerColor.WHITE;
                } else {
                    movingPlayer.getPlayer().displayMessage("Illegal move: leaves king in check.", true);
                    movingPlayer.setHighlightedTile(null);
                }
            }
        } catch (RemoteException e) {
            System.out.println("RemoteException in tryMove(): " + e);
        }
    }

    private void tileClickedHasHighlighted(int row, int col, TileCoordinate highlightedTile, ChessPlayerData clickingPlayer) {
        System.out.println("tileClickedHasHighlighted()");
        if (row == highlightedTile.getRow() && col == highlightedTile.getCol()) {
            // clicked their highlighted tile: unhighlight
            System.out.println("player deselcted");
            clickingPlayer.setHighlightedTile(null);
        } else {
            // clicked another tile
            ChessPiece clickedPiece = chessBoard.pieceAt(row, col);
            ChessPiece highlightedPiece = chessBoard.pieceAt(highlightedTile.getRow(), highlightedTile.getCol());

            if (clickingPlayer.getPlayerColor() != highlightedPiece.getPlayerColor()) {
                // clicked another tile, and their highlighted piece is an enemy piece. unhighlight the piece,
                // and perhaps highlight another
                deselectAndSelectOther(row, col, clickingPlayer, clickedPiece);
            } else {
                // clicked another tile, and their highlighted piece is friendly. check if they are trying to make a move,
                // or selecting another piece
                if (chessBoard.canMove(highlightedTile.getRow(), highlightedTile.getCol(), row, col)) {
                    // their highlighted piece can make the move. try to make it
                    tryMove(highlightedTile.getRow(), highlightedTile.getCol(), row, col, clickingPlayer);
                } else {
                    deselectAndSelectOther(row, col, clickingPlayer, clickedPiece);
                }
            }
        }
    }

    private void tileClickedNoHighlighted(int row, int col, ChessPlayerData chessPlayerData) {
        System.out.println("tileClickedNoHighlighted()");
        ChessPiece clickedPiece = chessBoard.pieceAt(row, col);
        if (clickedPiece == null) {
            return; // no highlighted piece, and the player clicked an empty square
        } else {
            chessPlayerData.setHighlightedTile(new TileCoordinate(row, col));
        }
    }

    private void redrawPlayer(ChessPlayerData player, HashMap<TileCoordinate, Tile2D> beforeTiles,
                              HashMap<TileCoordinate, Tile2D> afterTiles) {
        ArrayList<Tile2D> tilesToCreate = new ArrayList<>();
        for (TileCoordinate coords : afterTiles.keySet()) {
            if (!beforeTiles.containsKey(coords)) {
                tilesToCreate.add(afterTiles.get(coords));
            } else {
                Tile2D beforeTile = beforeTiles.get(coords);
                Tile2D afterTile = afterTiles.get(coords);
                if (!beforeTile.equals(afterTile)) {
                    tilesToCreate.add(afterTile);
                }
            }
        }

        HashSet<TileCoordinate> deletedCoordinates = new HashSet<>(beforeTiles.keySet());
        deletedCoordinates.removeAll(afterTiles.keySet());
        ArrayList<TileCoordinate> tilesToDelete = new ArrayList<>(deletedCoordinates);

        try {
            if (tilesToDelete.size() != 0) {
                player.getPlayer().deleteTiles(tilesToDelete, PlayerInterface.DeleteTilesMode.DELETE_EXISTING);
            }
            if (tilesToCreate.size() != 0) {
                player.getPlayer().createTiles(tilesToCreate, PlayerInterface.CreateTilesMode.CREATE);
            }
        } catch (RemoteException e) {
            System.out.println("RemoteException in redrawPlayer(): " + e);
        }
    }

    @Override
    public void tileClicked2D(int row, int col, PlayerData2D clickingPlayerData) {
        System.out.println("tileClicked2D()");

        ChessPlayerData clickingPlayer = players.get(clickingPlayerData.getPlayerNumber());
        ChessPlayerData otherPlayer = players.get(1 - clickingPlayerData.getPlayerNumber());
        if (otherPlayer == null) {
            try {
                clickingPlayer.getPlayer().displayMessage("The other player has not yet joined.", true);
                return;
            } catch (RemoteException e) {
                System.out.println("RemoteException in tileClicked2D(): " + e);
            }
        }

        if (!clickingPlayerData.getPlayer().equals(clickingPlayer.getPlayer())) {
            System.out.println("Player remote objects do not agree, exiting.");
            System.exit(1);
        }

        HashMap<TileCoordinate, Tile2D> beforeTilesClickingPlayer = chessBoard.getCoordinatesToTiles(clickingPlayer);
        HashMap<TileCoordinate, Tile2D> beforeTilesOtherPlayer = chessBoard.getCoordinatesToTiles(otherPlayer);

        // black sees the board 180 degrees rotated
        if (clickingPlayer.getPlayerColor() == ChessPiece.PlayerColor.BLACK) {
            row = (ChessBoard.NUM_ROWS-1) - row;
            col = (ChessBoard.NUM_COLS-1) - col;
        }

        if (clickingPlayer.hasHighlightedTile()) {
            tileClickedHasHighlighted(row, col, clickingPlayer.getHighlightedTile(), clickingPlayer);
        } else {
            tileClickedNoHighlighted(row, col, clickingPlayer);
        }

        HashMap<TileCoordinate, Tile2D> afterTilesClickingPlayer = chessBoard.getCoordinatesToTiles(clickingPlayer);
        HashMap<TileCoordinate, Tile2D> afterTilesOtherPlayer = chessBoard.getCoordinatesToTiles(otherPlayer);

        redrawPlayer(clickingPlayer, beforeTilesClickingPlayer, afterTilesClickingPlayer);
        redrawPlayer(otherPlayer, beforeTilesOtherPlayer, afterTilesOtherPlayer);

        System.out.println("player #" + clickingPlayerData.getPlayerNumber() + "(" + clickingPlayer.getPlayerColor() + ") clicked on (" + row + ", " + col + ")");
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
