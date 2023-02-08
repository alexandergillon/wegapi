package com.github.alexandergillon.wegapi.game_action;

public class ChangeImage extends GameAction {
    private int tileToChange;
    private String newImageName;

    public ChangeImage(int tileToChange, String newImageName) {
        actionType = ActionType.CHANGE_TILE_IMAGE;
        this.tileToChange = tileToChange;
        this.newImageName = newImageName;
    }

    public int getTileToChange() {
        return tileToChange;
    }

    public String getNewImageName() {
        return newImageName;
    }
}