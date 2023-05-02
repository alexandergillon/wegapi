package com.github.alexandergillon.wegapi.game.game_action;

import com.github.alexandergillon.wegapi.game.GameInterface;

import java.util.ArrayList;

public class CreateTiles extends GameAction {
    public enum Mode {
        CREATE,
        CREATE_NEW,
        OVERWRITE_EXISTING
    }

    private final ArrayList<GameInterface.Tile> tiles;
    private final Mode mode;

    public CreateTiles(ArrayList<GameInterface.Tile> tiles, Mode mode) {
        actionType = ActionType.CREATE_TILES;
        this.tiles = tiles;
        this.mode = mode;
    }
    
    public ArrayList<GameInterface.Tile> getTiles() {
        return tiles;
    }

    public Mode getMode() {
        return mode;
    }
}
