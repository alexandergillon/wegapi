package com.github.alexandergillon.wegapi.game.game_action;

public class SetPlayerNumber extends GameAction {
    private final int playerNumber;

    public SetPlayerNumber(int playerNumber) {
        actionType = ActionType.SET_PLAYER_NUMBER;
        this.playerNumber = playerNumber;
    }
    
    public int getPlayerNumber() {
        return playerNumber;
    }
}
