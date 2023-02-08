package com.github.alexandergillon.wegapi.game_action;

import java.io.Serializable;

public abstract class GameAction implements Serializable {
    public enum ActionType {
        SET_PLAYER_NUMBER,
        DISPLAY_MESSAGE,
        CREATE_TILE,
        CHANGE_TILE_IMAGE,
        GAME_OVER
    }
    public ActionType actionType;
}
