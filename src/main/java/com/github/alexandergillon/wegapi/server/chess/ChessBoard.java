package com.github.alexandergillon.wegapi.server.chess;

import com.github.alexandergillon.wegapi.game.PlayerInterface2D;

import java.util.ArrayList;

/**
 * Class that represents a chess board.
 */
class ChessBoard {
    static final int NUM_ROWS = 8;
    static final int NUM_COLS = 8;
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
     * @param playerColor the player color of who this tile will be shown to
     * @return a Tile2D which represents the chess piece at those coordinates, appropriate for which side of the board
     *         the player is on
     */
    private static PlayerInterface2D.Tile2D pieceToTile(int row, int col, ChessPiece chessPiece, ChessPiece.PlayerColor playerColor) {
        // rotates the piece 180 degrees if the player viewing it is playing black
        if (playerColor == ChessPiece.PlayerColor.BLACK) {
            row = (NUM_ROWS-1) - row;
            col = (NUM_COLS-1) - col;
        }

        String iconName;
        if (chessPiece == null) {
            iconName = "empty-" + coordsToTileColor(row, col);
        } else {
            iconName = chessPiece.colorToString() + "-" + chessPiece.pieceTypeToString() + "-" + coordsToTileColor(row, col);
        }
        return new PlayerInterface2D.Tile2D(row, col, iconName);
    }

    /**
     * Converts the current state of the chess board to a Tile2D representation, which can be sent to a client for
     * them to display. This method needs the player color of who this tile will be shown to, as the resulting Tile2D's
     * position depends on which side of the board the player is on. I.e. because the two players see the board
     * differently, we need to know who is going to see this tile.
     *
     * @param playerColor the player color of who this tile will be shown to
     * @return an array of Tile2Ds that can be sent to a player, and will display the current state of the board
     */
    ArrayList<PlayerInterface2D.Tile2D> toTiles(ChessPiece.PlayerColor playerColor) {
        ArrayList<PlayerInterface2D.Tile2D> tiles = new ArrayList<>();
        for (int rowIndex = 0; rowIndex < NUM_ROWS; rowIndex++) {
            for (int colIndex = 0; colIndex < NUM_COLS; colIndex++) {
                tiles.add(pieceToTile(rowIndex, colIndex, board[rowIndex][colIndex], playerColor));
            }
        }
        return tiles;
    }
}
