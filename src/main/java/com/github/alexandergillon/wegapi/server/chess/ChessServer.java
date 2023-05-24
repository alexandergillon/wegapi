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

/** Server that runs the chess game. */
public class ChessServer extends BaseServer2D {
    /** Class that encapsulates chess-specific player data. */
    static class ChessPlayerData {
        private final int playerNumber;
        private PlayerInterface2D player;  // shouldn't change, but maybe in the future, on disconnect
        private final ChessPiece.PlayerColor playerColor;

        private TileCoordinate highlightedTile = null;

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
            return highlightedTile == null ? String.format("ChessPlayerData(player #%d, %s, with nothing selected", playerNumber, playerColor)
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

    private ArrayList<Tile2D> highlightPieceTiles(int row, int col, ChessPlayerData chessPlayerData,
                                                  boolean isFriendly) {
        System.out.println("highlightPieceTiles()");
        ChessPiece.PlayerColor playerColor = chessPlayerData.getPlayerColor();
        ChessBoard.HighlightMode brightHighlight = isFriendly ? ChessBoard.HighlightMode.FRIENDLY_BRIGHT
                : ChessBoard.HighlightMode.ENEMY_BRIGHT;
        ChessBoard.HighlightMode normalHighlight = isFriendly ? ChessBoard.HighlightMode.FRIENDLY_NORMAL
                : ChessBoard.HighlightMode.ENEMY_NORMAL;

        ArrayList<Tile2D> tiles = new ArrayList<>();
        tiles.add(chessBoard.pieceToTile(row, col, playerColor, brightHighlight));

        ArrayList<TileCoordinate> possibleMoves = chessBoard.getPossibleMoves(row, col);
        for (TileCoordinate move : possibleMoves) {
            tiles.add(chessBoard.pieceToTile(move.getRow(), move.getCol(), playerColor, normalHighlight));
        }

        return tiles;
    }

    private ArrayList<Tile2D> unhighlightPieceTiles(int row, int col, ChessPlayerData chessPlayerData) {
        System.out.println("unhighlightPieceTiles()");
        ChessPiece.PlayerColor playerColor = chessPlayerData.getPlayerColor();
        ArrayList<Tile2D> tiles = new ArrayList<>();
        tiles.add(chessBoard.pieceToTile(row, col, playerColor, ChessBoard.HighlightMode.NONE));

        ArrayList<TileCoordinate> possibleMoves = chessBoard.getPossibleMoves(row, col);
        for (TileCoordinate move : possibleMoves) {
            tiles.add(chessBoard.pieceToTile(move.getRow(), move.getCol(), playerColor, ChessBoard.HighlightMode.NONE));
        }

        return tiles;
    }

    private ArrayList<Tile2D> mergeTiles(ArrayList<Tile2D> baseLayer, ArrayList<Tile2D> aboveLayer) {
        System.out.println("mergeTiles()");
        HashSet<TileCoordinate> baseLayerSurvivingTiles = new HashSet<>();
        for (Tile2D baseTile : baseLayer) {
            baseLayerSurvivingTiles.add(new TileCoordinate(baseTile.getRow(), baseTile.getCol()));
        }

        for (Tile2D aboveTile : aboveLayer) {
            baseLayerSurvivingTiles.remove(new TileCoordinate(aboveTile.getRow(), aboveTile.getCol()));
        }

        ArrayList<Tile2D> mergedTiles = new ArrayList<>();
        for (Tile2D baseTile : baseLayer) {
            if (baseLayerSurvivingTiles.contains(new TileCoordinate(baseTile.getRow(), baseTile.getCol()))) {
                mergedTiles.add(baseTile);
            }
        }

        mergedTiles.addAll(aboveLayer);

        return mergedTiles;
    }


    private void deselectAndSelectOther(int row, int col, TileCoordinate highlightedTile, PlayerData2D playerData,
                                                          ChessPlayerData clickingPlayer, ChessPiece clickedPiece) {
        System.out.println("deselectAndSelectOther()");
        ArrayList<Tile2D> tilesToUpdate;

        ArrayList<Tile2D> tilesToUnhighlight = unhighlightPieceTiles(highlightedTile.getRow(), highlightedTile.getCol(), clickingPlayer);
        if (clickedPiece == null) {
            clickingPlayer.setHighlightedTile(null);
            tilesToUpdate = tilesToUnhighlight;
        } else {
            clickingPlayer.setHighlightedTile(new TileCoordinate(row, col));
            ArrayList<Tile2D> tilesToHighlight = highlightPieceTiles(row, col, clickingPlayer,
                    clickingPlayer.getPlayerColor() ==  clickedPiece.getPlayerColor());
            tilesToUpdate = mergeTiles(tilesToUnhighlight, tilesToHighlight);
        }

        try {
            playerData.getPlayer().createTiles(tilesToUpdate, PlayerInterface.CreateTilesMode.OVERWRITE_EXISTING);
        } catch (RemoteException e) {
            System.out.println("RemoteException in tileClickedDifferentFromEnemyHighlighted(): " + e);
        }
    }

    private void tryMove(int fromRow, int fromCol, int toRow, int toCol, TileCoordinate highlightedTile, PlayerData2D playerData, ChessPlayerData movingPlayer) {
        System.out.println("tryMove()");
        try {
            ArrayList<Tile2D> tilesToUpdate;
            if (currentPlayer != movingPlayer.getPlayerColor()) {
                playerData.getPlayer().displayMessage("Not your turn.", true);
                tilesToUpdate = unhighlightPieceTiles(highlightedTile.getRow(), highlightedTile.getCol(), movingPlayer);
                movingPlayer.setHighlightedTile(null);
            } else {
                // this must be done before the piece is moved
                ArrayList<Tile2D> tilesToUnhighlight = unhighlightPieceTiles(highlightedTile.getRow(), highlightedTile.getCol(), movingPlayer);
                if (chessBoard.tryMove(fromRow, fromCol, toRow, toCol)) {
                    ArrayList<Tile2D> tilesInvolvedInMove = new ArrayList<>();
                    tilesInvolvedInMove.add(chessBoard.pieceToTile(fromRow, fromCol, movingPlayer.getPlayerColor(), ChessBoard.HighlightMode.NONE));
                    tilesInvolvedInMove.add(chessBoard.pieceToTile(toRow, toCol, movingPlayer.getPlayerColor(), ChessBoard.HighlightMode.NONE));
                    tilesToUpdate = mergeTiles(tilesToUnhighlight, tilesInvolvedInMove);
                    movingPlayer.setHighlightedTile(null);

                    if (currentPlayer == ChessPiece.PlayerColor.WHITE) currentPlayer = ChessPiece.PlayerColor.BLACK;
                    else currentPlayer = ChessPiece.PlayerColor.WHITE;
                } else {
                    playerData.getPlayer().displayMessage("Illegal move: leaves king in check.", true);
                    tilesToUpdate = tilesToUnhighlight;
                    movingPlayer.setHighlightedTile(null);
                }
            }
            System.out.println("player made a move: updating them");
            playerData.getPlayer().createTiles(tilesToUpdate, PlayerInterface.CreateTilesMode.OVERWRITE_EXISTING);
        } catch (RemoteException e) {
            System.out.println("RemoteException in tryMove(): " + e);
        }
    }

    private void tileClickedHasHighlighted(int row, int col, TileCoordinate highlightedTile, PlayerData2D playerData,
                                           ChessPlayerData clickingPlayer) {
        System.out.println("tileClickedHasHighlighted()");
        if (row == highlightedTile.getRow() && col == highlightedTile.getCol()) {
            System.out.println("player deselcted: updating them");
            // clicked their highlighted tile: unhighlight
            ArrayList<Tile2D> tilesToUpdate = unhighlightPieceTiles(row, col, clickingPlayer);
            clickingPlayer.setHighlightedTile(null);
            try {
                playerData.getPlayer().createTiles(tilesToUpdate, PlayerInterface.CreateTilesMode.OVERWRITE_EXISTING);
            } catch (RemoteException e) {
                System.out.println("RemoteException in tileClickedNoHighlighted(): " + e);
            }
        } else {
            // clicked another tile
            ChessPiece clickedPiece = chessBoard.pieceAt(row, col);
            ChessPiece highlightedPiece = chessBoard.pieceAt(highlightedTile.getRow(), highlightedTile.getCol());

            if (clickingPlayer.getPlayerColor() != highlightedPiece.getPlayerColor()) {
                // clicked another tile, and their highlighted piece is an enemy piece. unhighlight the piece,
                // and perhaps highlight another
                deselectAndSelectOther(row, col, highlightedTile, playerData, clickingPlayer, clickedPiece);
            } else {
                // clicked another tile, and their highlighted piece is friendly. check if they are trying to make a move,
                // or selecting another piece
                if (chessBoard.canMove(highlightedTile.getRow(), highlightedTile.getCol(), row, col)) {
                    // their highlighted piece can make the move. try to make it
                    tryMove(highlightedTile.getRow(), highlightedTile.getCol(), row, col, highlightedTile, playerData, clickingPlayer); // todo
                } else {
                    deselectAndSelectOther(row, col, highlightedTile, playerData, clickingPlayer, clickedPiece);
                }
            }
        }
    }

    private void tileClickedNoHighlighted(int row, int col, PlayerData2D playerData, ChessPlayerData chessPlayerData) {
        System.out.println("tileClickedNoHighlighted()");
        ChessPiece clickedPiece = chessBoard.pieceAt(row, col);
        if (clickedPiece == null) {
            return; // no highlighted piece, and the player clicked an empty square
        } else {
            ArrayList<Tile2D> tilesToUpdate = highlightPieceTiles(row, col, chessPlayerData,
                    clickedPiece.getPlayerColor() == chessPlayerData.getPlayerColor());
            chessPlayerData.setHighlightedTile(new TileCoordinate(row, col));
            try {
                playerData.getPlayer().createTiles(tilesToUpdate, PlayerInterface.CreateTilesMode.OVERWRITE_EXISTING);
            } catch (RemoteException e) {
                System.out.println("RemoteException in tileClickedNoHighlighted(): " + e);
            }
        }
    }

    @Override
    public void tileClicked2D(int row, int col, PlayerData2D playerData) {
        System.out.println("tileClicked2D()");
        ChessPlayerData chessPlayerData = players.get(playerData.getPlayerNumber());
        if (chessPlayerData.getPlayerColor() == ChessPiece.PlayerColor.BLACK) {
            row = (ChessBoard.NUM_ROWS-1) - row;
            col = (ChessBoard.NUM_COLS-1) - col;
        }
        if (chessPlayerData.hasHighlightedTile()) {
            tileClickedHasHighlighted(row, col, chessPlayerData.getHighlightedTile(), playerData, chessPlayerData);
        } else {
            tileClickedNoHighlighted(row, col, playerData, chessPlayerData);
        }
        System.out.println("player #" + playerData.getPlayerNumber() + "(" + chessPlayerData.getPlayerColor() + ") clicked on (" + row + ", " + col + ")");
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
