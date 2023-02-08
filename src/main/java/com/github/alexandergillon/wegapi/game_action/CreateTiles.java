package com.github.alexandergillon.wegapi.game_action;

import java.util.ArrayList;

public class CreateTiles extends GameAction {
    private int numTiles;
    private ArrayList<String> imageNames;

    public CreateTiles(int numTiles, ArrayList<String> imageNames) {
        actionType = ActionType.CREATE_TILE;
        this.numTiles = numTiles;
        this.imageNames = imageNames;
    }
    
    public int getNumTiles() {
        return numTiles;
    }

    public ArrayList<String> getImageNames() {
        return imageNames;
    }
}
