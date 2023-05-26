package com.github.alexandergillon.wegapi.server.chess;

import com.github.alexandergillon.wegapi.game.Tile;
import com.github.alexandergillon.wegapi.game.Tile2D;
import com.github.alexandergillon.wegapi.game.TileCoordinate;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Class that represents a chess board.
 */
class ChessBoard {
    static final int NUM_ROWS = 8;
    static final int NUM_COLS = 8;
    static final int PAWN_ROW_WHITE = 6;
    static final int PAWN_ROW_BLACK = 1;
    /**
     * Format: rows are delineated by newlines, and tiles within rows by spaces.
     * The first letter is a color: B = black, W = white.
     * The second letter is a piece type: P = pawn, R = rook, N = knight, B = bishop, Q = queen, K = king.
     * 00 is an empty tile.
     */
    private static final String INITIAL_BOARD =
            "BR BN BB BQ BK BB BN BR\n" +
            "BP BP BP BP BP BP BP BP\n" +
            "00 00 00 00 00 00 00 00\n" +
            "00 00 00 00 00 00 00 00\n" +
            "00 00 00 00 00 00 00 00\n" +
            "00 00 00 00 00 00 00 00\n" +
            "WP WP WP WP WP WP WP WP\n" +
            "WR WN WB WQ WK WB WN WR";

    private final ChessPiece[][] board;  // null entries mean no piece is present on that tile

    /** Creates a new ChessBoard, which initializes itself to the starting setup of chess. */
    public ChessBoard() {
        board = createInitialBoard();
    }

    /**
     * Converts the text representation of a chess piece color to a ChessPiece::PlayerColor. B = black, W = white.
     *
     * @param c the character to convert
     * @return the chess piece color corresponding to the character
     * @throws IllegalArgumentException if the text does not represent a valid chess piece color
     */
    private static ChessPiece.PlayerColor textToPlayerColor(Character c) throws IllegalArgumentException {
        switch (c) {
            case 'B': return ChessPiece.PlayerColor.BLACK;
            case 'W': return ChessPiece.PlayerColor.WHITE;
            default: throw new IllegalArgumentException("Invalid chess piece color in text representation - only W (white) and B (black) are allowed.");
        }
    }

    /**
     * Converts the text representation of a chess piece type to a ChessPiece::ChessPieceType.
     * P = pawn, R = rook, N = knight, B = bishop, Q = queen, K = king.
     *
     * @param c the character to convert
     * @return the chess piece color corresponding to the character
     * @throws IllegalArgumentException if the text does not represent a valid chess piece color
     */
    private static ChessPiece.ChessPieceType textToPieceType(Character c) throws IllegalArgumentException {
        switch (c) {
            case 'P': return ChessPiece.ChessPieceType.PAWN;
            case 'R': return ChessPiece.ChessPieceType.ROOK;
            case 'N': return ChessPiece.ChessPieceType.KNIGHT;
            case 'B': return ChessPiece.ChessPieceType.BISHOP;
            case 'Q': return ChessPiece.ChessPieceType.QUEEN;
            case 'K': return ChessPiece.ChessPieceType.KING;
            default: throw new IllegalArgumentException("Invalid chess piece type in text representation - only " +
                    "P (pawn), R (rook), N (knight), B (bishop), Q (queen) and K (king) are allowed.");
        }
    }

    /**
     * Converts the text representation of a chess piece to a ChessPiece object.
     * The first letter is a color: B = black, W = white.
     * The second letter is a piece type: P = pawn, R = rook, N = knight, B = bishop, Q = queen, K = king.
     * 00 is an empty tile.
     *
     * @param text text representation of a chess piece, to convert
     * @return a chess piece corresponding to the text
     * @throws IllegalArgumentException if the text does not represent a valid chess piece
     */
    private static ChessPiece textToChessPiece(String text) throws IllegalArgumentException {
        text = text.strip();
        if (text.length() != 2) {
            throw new IllegalArgumentException("Invalid length in text representation of chess " +
                    "piece - must be of length 2.");
        }
        if (text.equals("00")) return null;

        ChessPiece.PlayerColor playerColor = textToPlayerColor(text.charAt(0));
        ChessPiece.ChessPieceType pieceType = textToPieceType(text.charAt(1));

        return new ChessPiece(pieceType, playerColor);
    }

    /**
     * Returns a ChessPiece[][] with the starting setup of chess.
     *
     * @return a ChessPiece[][] with the starting setup of chess
     * @throws IllegalArgumentException if the default initial board (INITIAL_BOARD) is ill-formed
     */
    private static ChessPiece[][] createInitialBoard() {
        String[] textRows = INITIAL_BOARD.split("\n");
        if (textRows.length != NUM_ROWS) {
            throw new IllegalArgumentException("Initial board text representation does " +
                    "not contain the correct number of rows.");
        }

        String[][] textMatrix = new String[NUM_ROWS][NUM_COLS];
        for (int rowIndex = 0; rowIndex < NUM_ROWS; rowIndex++) {
            String textRow = textRows[rowIndex];
            String[] textCols = textRow.split(" ");
            if (textCols.length != NUM_COLS) {
                throw new IllegalArgumentException("Initial board text representation does not contain the " +
                        "correct number of cols for row " + rowIndex + ".");
            }
            textMatrix[rowIndex] = textCols;
        }

        ChessPiece[][] initialBoard = new ChessPiece[NUM_ROWS][NUM_COLS];
        for (int rowIndex = 0; rowIndex < NUM_ROWS; rowIndex++) {
            for (int colIndex = 0; colIndex < NUM_COLS; colIndex++) {
                initialBoard[rowIndex][colIndex] = textToChessPiece(textMatrix[rowIndex][colIndex]);
            }
        }

        return initialBoard;
    }

    /**
     * Returns the chess piece at a tile on the board.
     *
     * @param row the row of a tile
     * @param col the column of a tile
     * @return the chess piece at that row and column, or null if none is present
     */
    ChessPiece pieceAt(int row, int col) {
        return board[row][col];
    }

    /**
     * Validates that a pair of coordinates are valid (i.e. on the board).
     *
     * @param row row to validate
     * @param col column to validate
     * @return whether the tile at that position is on the board
     */
    private boolean validateCoordinates(int row, int col) {
        if (row < 0 || row >= NUM_ROWS) return false;
        else return col >= 0 && col < NUM_COLS;
    }

    /**
     * Returns whether a pawn can move from a specified tile to another specified tile. Does not check if making
     * this move would put the king in check.
     *
     * @param fromRow the row the pawn is in
     * @param fromCol the column the pawn is in
     * @param toRow the row the pawn wants to move to
     * @param toCol the column the pawn wants to move to
     * @param thisPlayerColor the color of the pawn
     * @return whether the pawn can make the move
     */
    private boolean canMovePawn(int fromRow, int fromCol, int toRow, int toCol, ChessPiece.PlayerColor thisPlayerColor) {
        boolean isWhite = (thisPlayerColor == ChessPiece.PlayerColor.WHITE);
        boolean isBlack = !isWhite;

        int rowDiff = toRow - fromRow;
        if (isWhite && rowDiff >= 0) return false;
        else if (isBlack && rowDiff <= 0) return false;

        int rowDiffAbs = Math.abs(rowDiff);
        int colDiffAbs = Math.abs(toCol - fromCol);

        if (colDiffAbs == 0) {
            if (board[toRow][toCol] != null) return false;  // already a piece there, pawns can't capture forwards

            if (isWhite && fromRow == PAWN_ROW_WHITE && rowDiffAbs == 2) {
                // pawn which hasn't moved trying to move 2 spaces: check if the tile in between is empty
                return board[PAWN_ROW_WHITE-1][fromCol] == null;
            } else if (isWhite && fromRow == PAWN_ROW_WHITE && rowDiffAbs == 1) {
                return true;  // already checked nothing was there
            } else if (isBlack && fromRow == PAWN_ROW_BLACK && rowDiffAbs == 2) {
                // pawn which hasn't moved trying to move 2 spaces: check if the tile in between is empty
                return board[PAWN_ROW_BLACK+1][fromCol] == null;
            } else if (isBlack && fromRow == PAWN_ROW_BLACK && rowDiffAbs == 1) {
                return true;  // already checked nothing was there
            }

            // pawn is not in its starting position: it can only move 1 space. we have already checked that
            // where it wants to move is empty, so we just need to check if it wants to move 1 space away
            return rowDiffAbs == 1;
        } else if (colDiffAbs == 1) {
            // moved out of column: either capturing, or en passant
            if (rowDiffAbs != 1) return false;  // pawns can only move exactly 1 space diagonal if moving out of column

            ChessPiece pieceAtMoveTarget = board[toRow][toCol];
            if (pieceAtMoveTarget == null) return false; // todo: en passant
            // trying to capture a piece: check if it is an enemy one
            else return pieceAtMoveTarget.getPlayerColor() != thisPlayerColor;
        } else {
            // trying to move 2 or more columns away: clearly bogus
            return false;
        }
    }

    /**
     * Returns whether a rook can move from a specified tile to another specified tile. Does not check if making
     * this move would put the king in check.
     *
     * @param fromRow the row the rook is in
     * @param fromCol the column the rook is in
     * @param toRow the row the rook wants to move to
     * @param toCol the column the rook wants to move to
     * @param thisPlayerColor the color of the rook
     * @return whether the rook can make the move
     */
    private boolean canMoveRook(int fromRow, int fromCol, int toRow, int toCol, ChessPiece.PlayerColor thisPlayerColor) {
        // todo: castling
        int rowDiffAbs = Math.abs(toRow - fromRow);
        int colDiffAbs = Math.abs(toCol - fromCol);

        if (rowDiffAbs == 0 && colDiffAbs == 0) return false;
        if (rowDiffAbs != 0 && colDiffAbs != 0) return false;

        if (rowDiffAbs == 0) { // moving along a row
            int row = fromRow; // = toRow
            int minCol = Math.min(fromCol, toCol);
            int maxCol = Math.max(fromCol, toCol);
            for (int col = minCol+1; // skip first column, as that's where the piece is
                 col != maxCol-1;    // skip last column, as we need special handling
                 col++) {
                if (board[row][col] != null) return false;  // something is in the way: can't make the move
            }
            ChessPiece pieceAtMoveTarget = board[toRow][toCol];
            // the end of the move can either be empty, or capturing an enemy
            return pieceAtMoveTarget == null || pieceAtMoveTarget.getPlayerColor() != thisPlayerColor;
        } else { // colDiffAbs == 0, moving along a column
            int minRow = Math.min(fromRow, toRow);
            int maxRow = Math.max(fromRow, toRow);
            int col = fromCol; // = toCol
            for (int row = minRow+1; // skip first row, as that's where the piece is
                 row != maxRow-1;    // skip last row, as we need special handling
                 row++) {
                if (board[row][col] != null) return false;  // something is in the way: can't make the move
            }
            ChessPiece pieceAtMoveTarget = board[toRow][toCol];
            // the end of the move can either be empty, or capturing an enemy
            return pieceAtMoveTarget == null || pieceAtMoveTarget.getPlayerColor() != thisPlayerColor;
        }
    }

    /**
     * Returns whether a knight can move from a specified tile to another specified tile. Does not check if making
     * this move would put the king in check.
     *
     * @param fromRow the row the knight is in
     * @param fromCol the column the knight is in
     * @param toRow the row the knight wants to move to
     * @param toCol the column the knight wants to move to
     * @param thisPlayerColor the color of the knight
     * @return whether the knight can make the move
     */
    private boolean canMoveKnight(int fromRow, int fromCol, int toRow, int toCol, ChessPiece.PlayerColor thisPlayerColor) {
        int rowDiffAbs = Math.abs(toRow - fromRow);
        int colDiffAbs = Math.abs(toCol - fromCol);
        if ((rowDiffAbs == 1 && colDiffAbs == 2) || (rowDiffAbs == 2 && colDiffAbs == 1)) {
            ChessPiece pieceAtMoveTarget = board[toRow][toCol];
            // the end of the move can either be empty, or capturing an enemy
            return pieceAtMoveTarget == null || pieceAtMoveTarget.getPlayerColor() != thisPlayerColor;
        } else {
            return false;
        }
    }

    /**
     * Returns whether a bishop can move from a specified tile to another specified tile. Does not check if making
     * this move would put the king in check.
     *
     * @param fromRow the row the bishop is in
     * @param fromCol the column the bishop is in
     * @param toRow the row the bishop wants to move to
     * @param toCol the column the bishop wants to move to
     * @param thisPlayerColor the color of the bishop
     * @return whether the bishop can make the move
     */
    private boolean canMoveBishop(int fromRow, int fromCol, int toRow, int toCol, ChessPiece.PlayerColor thisPlayerColor) {
        int rowDiffAbs = Math.abs(toRow - fromRow);
        int colDiffAbs = Math.abs(toCol - fromCol);

        if (rowDiffAbs != colDiffAbs) return false;  // not moving along a diagonal

        int dRow = Integer.signum(toRow - fromRow);
        int dCol = Integer.signum(toCol - fromCol);

        int row = fromRow + dRow;
        int col = fromCol + dCol;

        while (!(row == toRow && col == toCol)) {
            if (board[row][col] != null) return false;  // something is in the way: can't make the move
            row += dRow;
            col += dCol;
        }
        // loop exits without having checked the last tile, i.e. the tile we want to move to

        ChessPiece pieceAtMoveTarget = board[toRow][toCol];
        // the end of the move can either be empty, or capturing an enemy
        return pieceAtMoveTarget == null || pieceAtMoveTarget.getPlayerColor() != thisPlayerColor;
    }

    /**
     * Returns whether a king can move from a specified tile to another specified tile. Does not check if making
     * this move would put the piece in check.
     *
     * @param fromRow the row the king is in
     * @param fromCol the column the king is in
     * @param toRow the row the king wants to move to
     * @param toCol the column the king wants to move to
     * @param thisPlayerColor the color of the king
     * @return whether the king can make the move
     * // todo: castling
     */
    private boolean canMoveKing(int fromRow, int fromCol, int toRow, int toCol, ChessPiece.PlayerColor thisPlayerColor) {
        int rowDiffAbs = Math.abs(toRow - fromRow);
        int colDiffAbs = Math.abs(toCol - fromCol);

        if (rowDiffAbs == 0 && colDiffAbs == 0) return false;
        else if (rowDiffAbs == 1 || colDiffAbs == 1) {
            ChessPiece pieceAtMoveTarget = board[toRow][toCol];
            // the end of the move can either be empty, or capturing an enemy
            return pieceAtMoveTarget == null || pieceAtMoveTarget.getPlayerColor() != thisPlayerColor;
        } else {
            return false;
        }
    }

    /**
     * Returns whether a piece at a given tile can move to another given tile.
     *
     * @param fromRow row of the piece
     * @param fromCol column of the piece
     * @param toRow row to move to
     * @param toCol column to move to
     * @return whether the move is legal, in chess
     */
    boolean canMove(int fromRow, int fromCol, int toRow, int toCol) {
        if (!validateCoordinates(fromRow, fromCol)) return false;
        if (!validateCoordinates(toRow, toCol)) return false;
        ChessPiece chessPiece = board[fromRow][fromCol];
        switch (chessPiece.getPieceType()) {
            case PAWN: return canMovePawn(fromRow, fromCol, toRow, toCol, chessPiece.getPlayerColor());
            case ROOK: return canMoveRook(fromRow, fromCol, toRow, toCol, chessPiece.getPlayerColor());
            case KNIGHT: return canMoveKnight(fromRow, fromCol, toRow, toCol, chessPiece.getPlayerColor());
            case BISHOP: return canMoveBishop(fromRow, fromCol, toRow, toCol, chessPiece.getPlayerColor());
            case QUEEN: return canMoveKnight(fromRow, fromCol, toRow, toCol, chessPiece.getPlayerColor())
                            || canMoveBishop(fromRow, fromCol, toRow, toCol, chessPiece.getPlayerColor());
            case KING: return canMoveKing(fromRow, fromCol, toRow, toCol, chessPiece.getPlayerColor());
            default: throw new AssertionError("Unrecognized chess piece type in ChessBoard.canMove()");
        }
    }

    /**
     * Returns the possible moves that a pawn of a certain color can make from a certain tile.
     * Make sure that a pawn of that color is actually on that tile.
     *
     * @param row row of the tile
     * @param col column of the tile
     * @param color color of the pawn
     * @return the coordinates of tiles that the pawn on that tile, with that color, can move to
     */
    private ArrayList<TileCoordinate> getPossibleMovesPawn(int row, int col, ChessPiece.PlayerColor color) {
        int dRow;
        switch (color) {
            case WHITE:
                dRow = -1;
                break;
            case BLACK:
                dRow = 1;
                break;
            default:
                throw new AssertionError("Unrecognized player color in ChessBoard.getPossibleMovesPawn()");
        }

        ArrayList<TileCoordinate> possibleMoves = new ArrayList<>();
        ArrayList<TileCoordinate> movesToTry = new ArrayList<>();

        movesToTry.add(new TileCoordinate(row + dRow, col));  // move 1 forwards
        movesToTry.add(new TileCoordinate(row + 2 * dRow, col));  // move 2 forwards
        movesToTry.add(new TileCoordinate(row + dRow, col-1));  // capture left
        movesToTry.add(new TileCoordinate(row + dRow, col+1));  // capture right

        for (TileCoordinate coordinate : movesToTry) {
            if (canMove(row, col, coordinate.getRow(), coordinate.getCol())) {
                possibleMoves.add(coordinate);
            }
        }

        return possibleMoves;
    }

    /**
     * Returns the possible moves that a rook can make from a certain tile.
     * Make sure that a rook is actually on that tile.
     *
     * @param row row of the tile
     * @param col column of the tile
     * @return the coordinates of tiles that the rook on that tile can move to
     */
    private ArrayList<TileCoordinate> getPossibleMovesRook(int row, int col) {
        ArrayList<TileCoordinate> possibleMoves = new ArrayList<>();

        // moves up, or down (depending on which side of the board you are on)
        for (int toRow = row+1; toRow < NUM_ROWS; toRow++) {
            if (board[toRow][col] == null) {
                possibleMoves.add(new TileCoordinate(toRow, col));
            } else {
                if (canMove(row, col, toRow, col)) {
                    possibleMoves.add(new TileCoordinate(toRow, col));
                }
                break;
            }
        }

        // moves down, or up (depending on which side of the board you are on - opposite to previous loop)
        for (int toRow = row-1; toRow >= 0; toRow--) {
            if (board[toRow][col] == null) {
                possibleMoves.add(new TileCoordinate(toRow, col));
            } else {
                if (canMove(row, col, toRow, col)) {
                    possibleMoves.add(new TileCoordinate(toRow, col));
                }
                break;
            }
        }

        // moves left, or right (depending on which side of the board you are on)
        for (int toCol = col+1; toCol < NUM_COLS; toCol++) {
            if (board[row][toCol] == null) {
                possibleMoves.add(new TileCoordinate(row, toCol));
            } else {
                if (canMove(row, col, row, toCol)) {
                    possibleMoves.add(new TileCoordinate(row, toCol));
                }
                break;
            }
        }

        // moves right, or left (depending on which side of the board you are on - opposite to previous loop)
        for (int toCol = col-1; toCol >= 0; toCol--) {
            if (board[row][toCol] == null) {
                possibleMoves.add(new TileCoordinate(row, toCol));
            } else {
                if (canMove(row, col, row, toCol)) {
                    possibleMoves.add(new TileCoordinate(row, toCol));
                }
                break;
            }
        }

        return possibleMoves;
    }

    /**
     * Returns the possible moves that a knight can make from a certain tile.
     * Make sure that a knight is actually on that tile.
     *
     * @param row row of the tile
     * @param col column of the tile
     * @return the coordinates of tiles that the rook on that tile can move to
     */
    private ArrayList<TileCoordinate> getPossibleMovesKnight(int row, int col) {
        ArrayList<TileCoordinate> possibleMoves = new ArrayList<>();
        ArrayList<TileCoordinate> movesToTry = new ArrayList<>();

        movesToTry.add(new TileCoordinate(row + 1, col + 2));  // +1 +2
        movesToTry.add(new TileCoordinate(row + 1, col - 2));  // +1 -2
        movesToTry.add(new TileCoordinate(row - 1, col + 2));  // -1 +2
        movesToTry.add(new TileCoordinate(row - 1, col - 2));  // -1 -2
        movesToTry.add(new TileCoordinate(row + 2, col + 1));  // +2 +1
        movesToTry.add(new TileCoordinate(row + 2, col - 1));  // +2 -1
        movesToTry.add(new TileCoordinate(row - 2, col + 1));  // -2 +1
        movesToTry.add(new TileCoordinate(row - 2, col - 1));  // -2 -1

        for (TileCoordinate coordinate : movesToTry) {
            if (canMove(row, col, coordinate.getRow(), coordinate.getCol())) {
                possibleMoves.add(coordinate);
            }
        }

        return possibleMoves;
    }

    /**
     * Returns the possible moves that a bishop can make from a certain tile.
     * Make sure that a bishop is actually on that tile.
     *
     * @param row row of the tile
     * @param col column of the tile
     * @return the coordinates of tiles that the bishop on that tile can move to
     */
    private ArrayList<TileCoordinate> getPossibleMovesBishop(int row, int col) {
        ArrayList<TileCoordinate> possibleMoves = new ArrayList<>();

        ArrayList<TileCoordinate> dirPairs = new ArrayList<>();
        dirPairs.add(new TileCoordinate(+1, +1));
        dirPairs.add(new TileCoordinate(+1, -1));
        dirPairs.add(new TileCoordinate(-1, +1));
        dirPairs.add(new TileCoordinate(-1, -1));

        for (TileCoordinate dirPair : dirPairs) {
            int dRow = dirPair.getRow();
            int dCol = dirPair.getCol();

            int toRow = row + dRow;
            int toCol = col + dCol;

            while (validateCoordinates(toRow, toCol)) {
                if (board[toRow][toCol] == null) {
                    possibleMoves.add(new TileCoordinate(toRow, toCol));
                } else {
                    if (canMove(row, col, toRow, toCol)) {
                        possibleMoves.add(new TileCoordinate(toRow, toCol));
                    }
                    break;
                }
                toRow += dRow;
                toCol += dCol;
            }
        }

        return possibleMoves;
    }

    /**
     * Returns the possible moves that a king can make from a certain tile.
     * Make sure that a king is actually on that tile.
     *
     * @param row row of the tile
     * @param col column of the tile
     * @return the coordinates of tiles that the king on that tile can move to
     * todo: castling
     */
    private ArrayList<TileCoordinate> getPossibleMovesKing(int row, int col) {
        ArrayList<TileCoordinate> possibleMoves = new ArrayList<>();
        ArrayList<TileCoordinate> movesToTry = new ArrayList<>();

        // corner moves
        movesToTry.add(new TileCoordinate(row + 1, col + 1));
        movesToTry.add(new TileCoordinate(row + 1, col - 1));
        movesToTry.add(new TileCoordinate(row - 1, col + 1));
        movesToTry.add(new TileCoordinate(row - 1, col - 1));
        // middle moves
        movesToTry.add(new TileCoordinate(row, col + 1));
        movesToTry.add(new TileCoordinate(row, col - 1));
        movesToTry.add(new TileCoordinate(row + 1, col));
        movesToTry.add(new TileCoordinate(row - 1, col));

        for (TileCoordinate coordinate : movesToTry) {
            if (canMove(row, col, coordinate.getRow(), coordinate.getCol())) {
                possibleMoves.add(coordinate);
            }
        }

        return possibleMoves;
    }

    /**
     * Gets all possible moves for a piece on a given tile.
     *
     * @param row row of the tile
     * @param col column of the tile
     * @return the moves that the piece at that row and column can make
     */
    ArrayList<TileCoordinate> getPossibleMoves(int row, int col) {
       ChessPiece piece = board[row][col];
       if (piece == null) throw new IllegalArgumentException("ChessBoard.getPossibleMoves() called on tile with no piece on it.");

       switch (piece.getPieceType()) {
           case PAWN: return getPossibleMovesPawn(row, col, piece.getPlayerColor());
           case ROOK: return getPossibleMovesRook(row, col);
           case KNIGHT: return getPossibleMovesKnight(row, col);
           case BISHOP: return getPossibleMovesBishop(row, col);
           case QUEEN:
               ArrayList<TileCoordinate> moves = getPossibleMovesRook(row, col);
               moves.addAll(getPossibleMovesBishop(row, col));
               return moves;
           case KING: return getPossibleMovesKing(row, col);
           default: throw new AssertionError("Unrecognized chess piece type in ChessBoard.getPossibleMoves()");
       }
    }

    private boolean isInCheck(ChessPiece.PlayerColor playerColor) {
        // not the most efficient, but reduces the number of data structures we need to maintain
        TileCoordinate kingPos = null;
        boolean kingFound = false;
        for (int row = 0; row < NUM_ROWS && !kingFound; row++) {
            for (int col = 0; col < NUM_COLS && !kingFound; col++) {
                ChessPiece piece = board[row][col];
                if (piece == null) continue;
                if (piece.getPieceType() == ChessPiece.ChessPieceType.KING && piece.getPlayerColor() == playerColor) {
                    kingPos = new TileCoordinate(row, col);
                    kingFound = true;
                }
            }
        }
        if (kingPos == null) throw new IllegalStateException("Player " + playerColor + " has no king.");

        for (int row = 0; row < NUM_ROWS; row++) {
            for (int col = 0; col < NUM_COLS; col++) {
                ChessPiece piece = board[row][col];
                if (piece == null || piece.getPlayerColor() == playerColor) continue;
                if (canMove(row, col, kingPos.getRow(), kingPos.getCol())) return true;
            }
        }

        return false;
    }

    /**
     * Moves a piece from a given tile to another given tile. Does not check if the move is legal.
     *
     * @param fromRow row of the piece
     * @param fromCol column of the piece
     * @param toRow row to move to
     * @param toCol column to move to
     */
    void move(int fromRow, int fromCol, int toRow, int toCol) {
        board[toRow][toCol] = board[fromRow][fromCol];
        board[fromRow][fromCol] = null;
    }

    boolean tryMove(int fromRow, int fromCol, int toRow, int toCol) {
        ChessPiece chessPiece = board[fromRow][fromCol];
        move(fromRow, fromCol, toRow, toCol);
        if (isInCheck(chessPiece.getPlayerColor())) {
            move(toRow, toCol, fromRow, fromCol);
            return false;
        } else {
            return true;
        }
    }

    /**
     * Enum to control how a tile is highlighted. The selected piece is brighter than the tiles of possible moves it
     * can make.
     */
    enum HighlightMode {
        NONE,   // no highlighting
        FRIENDLY_NORMAL,  // highlight with a friendly color
        FRIENDLY_BRIGHT,  // highlight with a bright friendly color
        ENEMY_NORMAL,     // highlight with an enemy color
        ENEMY_BRIGHT      // highlight with a bright enemy color
    }

    /**
     * Converts a pair of coordinates into the color of the board at that tile.
     *
     * @param row the row of the tile
     * @param col the column of the tile
     * @return the color of the board at that tile
     */
    private static String coordsToTileColor(int row, int col) {
        if (row % 2 == 0) {
            // even row: starts with light and alternates
            if (col % 2 == 0) {
                return "light";
            } else {
                return "dark";
            }
        } else {
            // odd row: starts with dark and alternates
            if (col % 2 == 0) {
                return "dark";
            } else {
                return "light";
            }
        }
    }

    /**
    * Converts a chess piece at a given row and column into a Tile2D, to send to a client for them to display. This
    * method needs the player color of who this tile will be shown to, as the resulting Tile2D's position depends
    * on which side of the board the player is on. I.e. because the two players see the board differently, we need
    * to know who is going to see this tile.
    *
    * @param row the row of the chess piece
    * @param col the column of the chess piece
    * @param chessPiece the chess piece
    * @param viewingPlayer the player color of who this tile will be shown to
    * @param highlightMode how the tile should be highlighted
    * @return a Tile2D which represents the chess piece at those coordinates, appropriate for which side of the board
    *         the player is on
    */
    private static Tile2D pieceToTile(int row, int col, ChessPiece chessPiece,
                                   ChessPiece.PlayerColor viewingPlayer,
                                   HighlightMode highlightMode) {
        // rotates the piece 180 degrees if the player viewing it is playing black
        if (viewingPlayer == ChessPiece.PlayerColor.BLACK) {
            row = (NUM_ROWS-1) - row;
            col = (NUM_COLS-1) - col;
        }

        StringBuilder iconName = new StringBuilder();
        if (chessPiece == null) {
            iconName.append("empty-").append(coordsToTileColor(row, col));
        } else {
            iconName.append(chessPiece.colorToString())
                    .append("-").append(chessPiece.pieceTypeToString())
                    .append("-").append(coordsToTileColor(row, col));
        }
        switch (highlightMode) {
            case NONE:
                break;
            case FRIENDLY_NORMAL:
                iconName.append("-highlighted");
                break;
            case ENEMY_NORMAL:
                iconName.append("-highlighted-enemy");
                break;
            case FRIENDLY_BRIGHT:
                iconName.append("-highlighted-bright");
                break;
            case ENEMY_BRIGHT:
                iconName.append("-highlighted-bright-enemy");
                break;
            default:
                throw new AssertionError("Unrecognized player color in ChessBoard.pieceToTile()");
        }

        return new Tile2D(row, col, iconName.toString());
    }

    /**
     * Converts a tile at a given row and column into a Tile2D, to send to a client for them to display. This
     * method needs the player color of who this tile will be shown to, as the resulting Tile2D's position depends
     * on which side of the board the player is on. I.e. because the two players see the board differently, we need
     * to know who is going to see this tile.
     *
     * @param row the row of the chess piece
     * @param col the column of the chess piece
     * @param viewingPlayer the player color of who this tile will be shown to
     * @param highlightMode how the tile should be highlighted
     * @return a Tile2D which represents the chess piece at those coordinates, appropriate for which side of the board
     *         the player is on
     */
    Tile2D pieceToTile(int row, int col, ChessPiece.PlayerColor viewingPlayer,
                    HighlightMode highlightMode) {
        return pieceToTile(row, col, board[row][col], viewingPlayer, highlightMode);
    }

    /**
     * Get a player's highlighted tiles, as Tile2Ds.
     *
     * @param player the player, whose highlighted tiles to get
     * @return the highlighted tiles of that player
     */
    private ArrayList<Tile2D> getHighlightedTiles(ChessServer.ChessPlayerData player) {
        if (!player.hasHighlightedTile()) return new ArrayList<>();
        int row = player.getHighlightedTile().getRow();  // tile of the player's highlighted piece
        int col = player.getHighlightedTile().getCol();  // tile of

        ChessPiece.PlayerColor playerColor = player.getPlayerColor();
        boolean isFriendly = playerColor == board[row][col].getPlayerColor();

        ChessBoard.HighlightMode brightHighlight = isFriendly ? ChessBoard.HighlightMode.FRIENDLY_BRIGHT
                : ChessBoard.HighlightMode.ENEMY_BRIGHT;
        ChessBoard.HighlightMode normalHighlight = isFriendly ? ChessBoard.HighlightMode.FRIENDLY_NORMAL
                : ChessBoard.HighlightMode.ENEMY_NORMAL;

        ArrayList<Tile2D> tiles = new ArrayList<>();
        tiles.add(pieceToTile(row, col, playerColor, brightHighlight));

        ArrayList<TileCoordinate> possibleMoves = getPossibleMoves(row, col);
        for (TileCoordinate move : possibleMoves) {
            tiles.add(pieceToTile(move.getRow(), move.getCol(), playerColor, normalHighlight));
        }

        return tiles;
    }

    /**
     * Converts the current state of the chess board to a map from tile coordinates to Tile2Ds representing that tile.
     * This method needs the player color of who this tile will be shown to, as the resulting Tile2D's
     * position depends on which side of the board the player is on. I.e. because the two players see the board
     * differently, we need to know who is going to see this tile.
     *
     * @param viewingPlayer the player data of who this tile will be shown to
     * @return a mapping from coordinates to Tile2Ds, representing the board from that player's perspective
     */
    HashMap<TileCoordinate, Tile2D> getCoordinatesToTiles(ChessServer.ChessPlayerData viewingPlayer) {
        HashMap<TileCoordinate, Tile2D> coordinatesToTiles = new HashMap<>();

        // first, add the highlighted tiles
        ArrayList<Tile2D> highlightedTiles = getHighlightedTiles(viewingPlayer);
        for (Tile2D tile : highlightedTiles) {
            coordinatesToTiles.put(new TileCoordinate(tile.getRow(), tile.getCol()), tile);
        }

        // then, add all other tiles
        for (int row = 0; row < NUM_ROWS; row++) {
            for (int col = 0; col < NUM_COLS; col++) {
                Tile2D tile = pieceToTile(row, col, viewingPlayer.getPlayerColor(), HighlightMode.NONE);
                // important to use tile.getRow/getCol as they may be different from row/col
                // this is why we create the tile first, as it allows us to avoid player color conversions here
                // with the small overhead of creating some tiles that may be thrown away
                if (!coordinatesToTiles.containsKey(new TileCoordinate(tile.getRow(), tile.getCol()))) {
                    coordinatesToTiles.put(new TileCoordinate(tile.getRow(), tile.getCol()), tile);
                }
            }
        }

        return coordinatesToTiles;
    }

    /**
     * Converts the current state of the chess board to a Tile2D representation, which can be sent to a client for
     * them to display. This method needs the player color of who this tile will be shown to, as the resulting Tile2D's
     * position depends on which side of the board the player is on. I.e. because the two players see the board
     * differently, we need to know who is going to see this tile.
     *
     * @param viewingPlayer the player data of who this tile will be shown to
     * @return an array of Tile2Ds that can be sent to a player, and will display the current state of the board
     */
    ArrayList<Tile2D> toTiles(ChessServer.ChessPlayerData viewingPlayer) {
        return new ArrayList<>(getCoordinatesToTiles(viewingPlayer).values());
    }




}
