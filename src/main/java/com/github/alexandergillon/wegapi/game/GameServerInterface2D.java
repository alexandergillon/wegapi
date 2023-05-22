package com.github.alexandergillon.wegapi.game;

public interface GameServerInterface2D {
    String defaultIp = GameServerInterface.defaultIp;
    int rmiRegistryPort = GameServerInterface.rmiRegistryPort;
    String defaultServerPath = GameServerInterface.defaultServerPath;

    static class PlayerData2D {
        private final int playerNumber;
        private final PlayerInterface2D player;

        public PlayerData2D(int playerNumber, PlayerInterface2D player) {
            this.playerNumber = playerNumber;
            this.player = player;
        }

        public int getPlayerNumber() {
            return playerNumber;
        }

        public PlayerInterface2D getPlayer() {
            return player;
        }
    }

    /**
     * Informs the server that a client has joined the game for the first time.
     */
    void registerPlayer2D(PlayerInterface2D player);

    /**
     * Informs the server that a certain player double-clicked a certain tile.
     *
     * @param row the row of the tile that the player clicked
     * @param col the column of the tile that the player clicked
     * @param playerData the player who clicked the tile
     */
    void tileClicked2D(int row, int col, PlayerData2D playerData);

    /**
     * Informs the server that a certain player dragged one tile to another.
     *
     * @param fromRow the row of the tile that was dragged
     * @param fromCol the column of the tile that was dragged
     * @param toRow the row of the tile that was dragged to
     * @param toCol the column of the tile that was dragged to
     * @param playerData the player who dragged the tile
     */
    void tileDragged2D(int fromRow, int fromCol, int toRow, int toCol, PlayerData2D playerData);
}