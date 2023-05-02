package com.github.alexandergillon.wegapi.game_action;

public class GameOver extends GameAction {
    private final boolean win;

    public GameOver(boolean win) {
        actionType = ActionType.GAME_OVER;
        this.win = win;
    }
    
    public boolean isWon() {
        return win;
    }
}
