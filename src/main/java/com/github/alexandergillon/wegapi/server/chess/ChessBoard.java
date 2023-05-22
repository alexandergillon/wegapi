package com.github.alexandergillon.wegapi.server.chess;

import com.github.alexandergillon.wegapi.game.PlayerInterface2D;
import com.github.alexandergillon.wegapi.server.BaseServer2D;

import java.util.ArrayList;

class ChessBoard {
    static final int numRows = 8;
    static final int numCols = 8;

    private static String initialBoard =
            "BR BN BB BQ BK BB BN BR\n" +
            "BP BP BP BP BP BP BP BP\n" +
            "00 00 00 00 00 00 00 00\n" +
            "00 00 00 00 00 00 00 00\n" +
            "00 00 00 00 00 00 00 00\n" +
            "00 00 00 00 00 00 00 00\n" +
            "WP WP WP WP WP WP WP WP\n" +
            "WR WN WB WQ WK WB WN WR";

    private final ChessPiece[][] board;

    public ChessBoard() {
        board = createInitialBoard();
    }

    private static ChessPiece textToChessPiece(String text) throws IllegalArgumentException {
        text = text.strip();
        if (text.length() != 2) {
            throw new IllegalArgumentException("Invalid length in text representation of chess piece - must be of length 2.");
        }
        if (text.equals("00")) return null;

        char colorChar = text.charAt(0);
        char pieceChar = text.charAt(1);

        ChessPiece.PlayerColor playerColor;
        switch (colorChar) {
            case 'B':
                playerColor = ChessPiece.PlayerColor.BLACK;
                break;
            case 'W':
                playerColor = ChessPiece.PlayerColor.WHITE;
                break;
            default:
                throw new IllegalArgumentException("Invalid chess piece color in text representation - only W (white) and B (black) are allowed.");
        }

        ChessPiece.ChessPieceType pieceType;
        switch (pieceChar) {
            case 'P':
                pieceType = ChessPiece.ChessPieceType.PAWN;
                break;
            case 'R':
                pieceType = ChessPiece.ChessPieceType.ROOK;
                break;
            case 'N':
                pieceType = ChessPiece.ChessPieceType.KNIGHT;
                break;
            case 'B':
                pieceType = ChessPiece.ChessPieceType.BISHOP;
                break;
            case 'Q':
                pieceType = ChessPiece.ChessPieceType.QUEEN;
                break;
            case 'K':
                pieceType = ChessPiece.ChessPieceType.KING;
                break;
            default:
                throw new IllegalArgumentException("Invalid chess piece type in text representation - only P (pawn), R (rook), N (knight), B (bishop), Q (queen) and K (king) are allowed.");
        }

        return new ChessPiece(pieceType, playerColor);
    }

    private static ChessPiece[][] createInitialBoard() {
        String[] textRows = initialBoard.split("\n");
        if (textRows.length != numRows) {
            throw new IllegalStateException("Initial board text representation does not contain the correct number of rows.");
        }

        String[][] textMatrix = new String[numRows][numCols];
        for (int rowIndex = 0; rowIndex < numRows; rowIndex++) {
            String textRow = textRows[rowIndex];
            String[] textCols = textRow.split(" ");
            if (textCols.length != numCols) {
                throw new IllegalStateException("Initial board text representation does not contain the correct number of cols for some row.");
            }
            textMatrix[rowIndex] = textCols;
        }

        ChessPiece[][] initialBoard = new ChessPiece[numRows][numCols];
        for (int rowIndex = 0; rowIndex < numRows; rowIndex++) {
            for (int colIndex = 0; colIndex < numCols; colIndex++) {
                initialBoard[rowIndex][colIndex] = textToChessPiece(textMatrix[rowIndex][colIndex]);
            }
        }

        return initialBoard;
    }

    private static String coordsToTileColor(int row, int col) {
        if (row % 2 == 0) {
            if (col % 2 == 0) {
                return "light";
            } else {
                return "dark";
            }
        } else {
            if (col % 2 == 0) {
                return "dark";
            } else {
                return "light";
            }
        }
    }

    private static PlayerInterface2D.Tile2D pieceToTile(int row, int col, ChessPiece chessPiece, ChessPiece.PlayerColor playerColor) {
        // rotates the board 180 degrees if the player viewing it is playing black
        if (playerColor == ChessPiece.PlayerColor.BLACK) {
            row = (numRows-1) - row;
            col = (numCols-1) - col;
        }

        String iconName;
        if (chessPiece == null) {
            iconName = "empty-" + coordsToTileColor(row, col);
        } else {
            iconName = chessPiece.colorToString() + "-" + chessPiece.pieceTypeToString() + "-" + coordsToTileColor(row, col);
        }
        return new PlayerInterface2D.Tile2D(row, col, iconName);
    }

    ArrayList<PlayerInterface2D.Tile2D> toTiles(ChessPiece.PlayerColor playerColor) {
        ArrayList<PlayerInterface2D.Tile2D> tiles = new ArrayList<>();
        for (int rowIndex = 0; rowIndex < numRows; rowIndex++) {
            for (int colIndex = 0; colIndex < numCols; colIndex++) {
                tiles.add(pieceToTile(rowIndex, colIndex, board[rowIndex][colIndex], playerColor));
            }
        }
        return tiles;
    }

}
