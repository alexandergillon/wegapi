package com.github.alexandergillon.wegapi.game;

/**
 * Interface that a game server needs to implement to accept and process user actions, for a 2D game. <br> <br>
 *
 * For developers of games based on WEGAPI: in order to create a 2D game, subclass BaseServer2D, which will require
 * you to implement the methods in this interface. WEGAPI will handle client input, displaying the game to the client,
 * etc. <br> <br>
 *
 * For developers of WEGAPI itself: see BaseServer2D.java for how this interface fits into the WEGAPI model. This
 * interface is actually a wrapper on underlying (1D) functions via the GameServerInterface, and so this interface
 * is not used directly for RMI communication.
 *
 * todo: write a tutorial about how to create a game with wegapi
 */
public interface GameServerInterface2D {
    /** 2D equivalent of GameServerInterface::PlayerData. */
    // todo: make secure (player cannot forge actions from other players)
    class PlayerData2D {
        private final int playerNumber;
        private final PlayerInterface2D player;

        /**
         * Creates a PlayerData2D with the given playerNumber and remote object.
         *
         * @param playerNumber the unique identifier assigned to the player with PlayerInterface.initialize()
         * @param player a remote object that can be used to communicate with the player via RMI
         */
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