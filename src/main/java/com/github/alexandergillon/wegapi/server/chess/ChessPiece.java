package com.github.alexandergillon.wegapi.server.chess;

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

    String colorToString() {
        return colorToString(playerColor);
    }

    String pieceTypeToString() {
        return pieceTypeToString(pieceType);
    }

    static String colorToString(PlayerColor color) {
        switch (color) {
            case WHITE: return "white";
            case BLACK: return "black";
            default: throw new IllegalArgumentException("Unrecognized PlayerColor in ChessPiece.colorToString().");
        }
    }

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
