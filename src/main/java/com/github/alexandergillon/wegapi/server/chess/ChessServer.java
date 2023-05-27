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

        private TileCoordinate selectedTile = null;  // todo: rename to selected

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

        public boolean hasSelectedTile() {
            return selectedTile != null;
        }

        public TileCoordinate getSelectedTile() {
            return selectedTile;
        }

        public void setSelectedTile(TileCoordinate selectedTile) {
            this.selectedTile = selectedTile;
        }

        @Override
        public String toString() {
            return selectedTile == null ? String.format("ChessPlayerData(player #%d, %s, with nothing selected)", playerNumber, playerColor)
            : String.format("ChessPlayerData(player #%d, %s, with (%d, %d) selected", playerNumber, playerColor, selectedTile.getRow(), selectedTile.getCol());
        }
    }

    private final HashMap<Integer, ChessPlayerData> players = new HashMap<>(); // maps player number to chess-specific data
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

    /** Registers the player. Players are shown the default setup of chess, and the game can begin. */
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

    /**
     * Updates the other player when a player successfully made a move. This means checking whether the move interfered
     * with what the other player currently had selected, and if so, deselecting the other player's piece. For example,
     * the other player may have a piece selected that was captured in the move that just occurred. Then we need to
     * deselect the other player's piece as this piece no longer exists.
     *
     * @param fromRow the row that was moved from
     * @param fromCol the column that was moved from
     * @param toRow the row that was moved to
     * @param toCol the column that was moved to
     * @param movingPlayer the player who moved
     */
    private void updateOtherPlayer(int fromRow, int fromCol, int toRow, int toCol, ChessPlayerData movingPlayer) {
        ChessPlayerData otherPlayer = players.get(1 - movingPlayer.getPlayerNumber());
        if (!otherPlayer.hasSelectedTile()) return;

        int selectedRow = otherPlayer.getSelectedTile().getRow();
        int selectedCol = otherPlayer.getSelectedTile().getCol();

        if (selectedRow == fromRow && selectedCol == fromCol) {
            otherPlayer.setSelectedTile(null);
        } else if (selectedRow == toRow && selectedCol == toCol) {
            otherPlayer.setSelectedTile(null);
        }
    }

    /**
     * Attempts to make a move. This move must be legal, except for possibly leaving the king in check. If this move
     * does in fact leave the king in check (and is hence actually an illegal move), the move fails, and the player is
     * notified. Otherwise, the move succeeds, and the visuals of both players are updated accordingly.
     *
     * @param fromRow the row of the piece to move
     * @param fromCol the column of the piece to move
     * @param toRow the row of where to move to
     * @param toCol the column of where to move to
     * @param movingPlayer the player who made the move
     */
    private void tryMove(int fromRow, int fromCol, int toRow, int toCol, ChessPlayerData movingPlayer) {
        try {
            if (currentPlayer != movingPlayer.getPlayerColor()) {
                movingPlayer.getPlayer().displayMessage("Not your turn.", true);
                movingPlayer.setSelectedTile(null);
            } else {
                if (chessBoard.tryMove(fromRow, fromCol, toRow, toCol)) {
                    movingPlayer.setSelectedTile(null);
                    updateOtherPlayer(fromRow, fromCol, toRow, toCol, movingPlayer);

                    if (currentPlayer == ChessPiece.PlayerColor.WHITE) currentPlayer = ChessPiece.PlayerColor.BLACK;
                    else currentPlayer = ChessPiece.PlayerColor.WHITE;
                } else {
                    movingPlayer.getPlayer().displayMessage("Illegal move: leaves king in check.", true);
                    movingPlayer.setSelectedTile(null);
                }
            }
        } catch (RemoteException e) {
            System.out.println("RemoteException in tryMove(): " + e);
        }
    }

    /**
     * This function is called when a player has clicked a tile when they have a piece selected. It updates the game
     * state based on where the player clicked. <br> <br>
     *
     * If they clicked their selected piece, it deselects that piece. If they clicked a tile that their selected piece
     * might be able to move to, it tries to make that move (this could fail if the move would leave their king in
     * check). Otherwise, it deselects their currently selected piece (if they clicked an empty tile that their
     * piece cannot move to), or selects a different piece (if they clicked another piece that their piece cannot
     * move to).
     *
     * @param row the row that the player clicked
     * @param col the column that the player clicked
     * @param selectedTile the player's selected tile coordinates
     * @param clickingPlayer the player who made the click
     */
    private void tileClickedHasSelected(int row, int col, TileCoordinate selectedTile, ChessPlayerData clickingPlayer) {
        if (row == selectedTile.getRow() && col == selectedTile.getCol()) {
            // clicked their selected tile: deselect
            System.out.println("player deselcted");
            clickingPlayer.setSelectedTile(null);
        } else {
            // clicked another tile
            ChessPiece clickedPiece = chessBoard.pieceAt(row, col);
            ChessPiece selectedPiece = chessBoard.pieceAt(selectedTile.getRow(), selectedTile.getCol());

            if (clickingPlayer.getPlayerColor() != selectedPiece.getPlayerColor()) {
                // clicked another tile, and their selected piece is an enemy piece. deselect the piece,
                // and perhaps select another
                if (clickedPiece == null) {
                    clickingPlayer.setSelectedTile(null);
                } else {
                    clickingPlayer.setSelectedTile(new TileCoordinate(row, col));
                }
            } else {
                // clicked another tile, and their selected piece is friendly. check if they are trying to make a move,
                // or selecting another piece
                if (chessBoard.canMove(selectedTile.getRow(), selectedTile.getCol(), row, col)) {
                    // their selected piece can make the move. try to make it
                    tryMove(selectedTile.getRow(), selectedTile.getCol(), row, col, clickingPlayer);
                } else {
                    // not trying to make a move: deselect their piece and potentially select another
                    if (clickedPiece == null) {
                        clickingPlayer.setSelectedTile(null);
                    } else {
                        clickingPlayer.setSelectedTile(new TileCoordinate(row, col));
                    }
                }
            }
        }
    }

    /**
     * This function is called when a player has clicked a tile when they do not have a piece selected. It updates
     * the game state based on where the player clicked. Essentially, if they clicked a piece, it is selected,
     * and otherwise nothing happens.
     *
     * @param row the row that the player clicked
     * @param col the column that the player clicked
     * @param clickingPlayer the player who made the click
     */
    private void tileClickedNoSelected(int row, int col, ChessPlayerData clickingPlayer) {
        ChessPiece clickedPiece = chessBoard.pieceAt(row, col);
        if (clickedPiece == null) {
            return; // no selected piece, and the player clicked an empty square
        } else {
            clickingPlayer.setSelectedTile(new TileCoordinate(row, col));
        }
    }

    /**
     * Redraws tiles for a player, based on what changed. This function takes the state of the game before some change,
     * and the state after (both as lists of Tile2Ds). Then, any tiles that were present before and not afterwards are
     * deleted, and any tiles that have changed icons are redrawn for the user. This therefore avoids redrawing the
     * entire game for the user each time a change is made, which is faster and looks better.
     *
     * @param player the player whose game to redraw
     * @param beforeTiles the state of the game before some change
     * @param afterTiles the state of the game after some change
     */
    private void redrawPlayer(ChessPlayerData player, HashMap<TileCoordinate, Tile2D> beforeTiles,
                              HashMap<TileCoordinate, Tile2D> afterTiles) {
        // update ('create') any tiles whose icons changed
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

        // delete any tiles which were present before but not after
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

    /** Processes a player's click. Selects/deselects/moves pieces appropriately. */
    @Override
    public void tileClicked2D(int row, int col, PlayerData2D clickingPlayerData) {
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

        // make sure the remote object we have on file corresponds, as this is what we use to update the player
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

        // process the click
        if (clickingPlayer.hasSelectedTile()) {
            tileClickedHasSelected(row, col, clickingPlayer.getSelectedTile(), clickingPlayer);
        } else {
            tileClickedNoSelected(row, col, clickingPlayer);
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
