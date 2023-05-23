package com.github.alexandergillon.wegapi.server.chess;

/**
 * Class that represents a chess piece.
 */
class ChessPiece {
    enum PlayerColor {
        WHITE,
        BLACK
    }

    enum ChessPieceType {
        PAWN,
        ROOK,
        KNIGHT,
        BISHOP,
        QUEEN,
        KING
    }

    private final ChessPieceType pieceType;
    private final PlayerColor playerColor;

    public ChessPiece(ChessPieceType pieceType, PlayerColor playerColor) {
        this.pieceType = pieceType;
        this.playerColor = playerColor;
    }

    public ChessPieceType getPieceType() {
        return pieceType;
    }

    public PlayerColor getPlayerColor() {
        return playerColor;
    }

    /**
     * Returns a string representation of this piece's color.
     *
     * @return a string representation of this piece's color
     */
    String colorToString() {
        return colorToString(playerColor);
    }

    /**
     * Returns a string representation of this piece's piece type.
     *
     * @return a string representation of this piece's piece type
     */
    String pieceTypeToString() {
        return pieceTypeToString(pieceType);
    }

    /**
     * Given a player color, returns a string representation of it.
     *
     * @param color a player color
     * @return a string representation of that player color
     * @throws IllegalArgumentException if the color is not a valid member of PlayerColor
     */
    static String colorToString(PlayerColor color) {
        switch (color) {
            case WHITE: return "white";
            case BLACK: return "black";
            default: throw new IllegalArgumentException("Unrecognized PlayerColor in ChessPiece.colorToString().");
        }
    }

    /**
     * Given a piece type, returns a string representation of it.
     *
     * @param type a chess piece type
     * @return a string representation of that chess piece type
     * @throws IllegalArgumentException if the piece type is not a valid member of ChessPieceType
     */
    static String pieceTypeToString(ChessPieceType type) {
        switch (type) {
            case PAWN: return "pawn";
            case ROOK: return "rook";
            case KNIGHT: return "knight";
            case BISHOP: return "bishop";
            case QUEEN: return "queen";
            case KING: return "king";
            default: throw new IllegalArgumentException("Unrecognized ChessPieceType in ChessPiece.pieceTypeToString().");
        }
    }
}
