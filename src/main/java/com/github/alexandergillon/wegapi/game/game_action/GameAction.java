package com.github.alexandergillon.wegapi.game.game_action;

import java.io.Serializable;

public abstract class GameAction implements Serializable {
    public enum ActionType {
        SET_PLAYER_NUMBER,
        DISPLAY_MESSAGE,
        CREATE_TILES,
        GAME_OVER
    }
    public ActionType actionType;
}
