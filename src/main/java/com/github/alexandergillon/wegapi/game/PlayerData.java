package com.github.alexandergillon.wegapi.game;

import java.io.Serializable;

/**
 * Class that bundles data about a player. Sent to the server when a player makes an action, so that the server
 * can know who took an action and act appropriately.
 */
// todo: make secure (player cannot forge actions from other players)
public class PlayerData implements Serializable {
    private final int playerNumber; // unique identifier
    private final PlayerInterface player; // remote object to communicate with the player via RMI

    /**
     * Creates a PlayerData with the given playerNumber and remote object.
     *
     * @param playerNumber the unique identifier assigned to the player with PlayerInterface.initialize()
     * @param player       a remote object that can be used to communicate with the player via RMI
     */
    public PlayerData(int playerNumber, PlayerInterface player) {
        this.playerNumber = playerNumber;
        this.player = player;
    }

    public int getPlayerNumber() {
        return playerNumber;
    }

    public PlayerInterface getPlayer() {
        return player;
    }
}
