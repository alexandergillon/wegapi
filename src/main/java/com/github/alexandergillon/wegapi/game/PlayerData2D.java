package com.github.alexandergillon.wegapi.game;

/**
 * 2D equivalent of GameServerInterface::PlayerData.
 */
// todo: make secure (player cannot forge actions from other players)
public class PlayerData2D {
    private final int playerNumber;
    private final PlayerInterface2D player;

    /**
     * Creates a PlayerData2D with the given playerNumber and remote object.
     *
     * @param playerNumber the unique identifier assigned to the player with PlayerInterface.initialize()
     * @param player       a remote object that can be used to communicate with the player via RMI
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
