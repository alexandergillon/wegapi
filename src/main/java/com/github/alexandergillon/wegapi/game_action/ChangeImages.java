package com.github.alexandergillon.wegapi.game_action;

import java.util.ArrayList;

public class ChangeImages extends GameAction {
    private ArrayList<Integer> tilesToChange;
    private ArrayList<String> imageNames;

    public ChangeImages(ArrayList<Integer> tilesToChange, ArrayList<String> imageNames) {
        actionType = ActionType.CHANGE_TILE_IMAGE;
        this.tilesToChange = tilesToChange;
        this.imageNames = imageNames;
    }

    public ArrayList<Integer> getTilesToChange() {
        return tilesToChange;
    }

    public ArrayList<String> getImageNames() {
        return imageNames;
    }
}