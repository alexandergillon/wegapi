package com.github.alexandergillon.wegapi.game_action;

public class CreateTile extends GameAction {
    private int tileToCreate;
    private String newImageName;

    public CreateTile(int tileToCreate, String newImageName) {
        actionType = ActionType.CREATE_TILE;
        this.tileToCreate = tileToCreate;
        this.newImageName = newImageName;
    }
    
    public int getTileToCreate() {
        return tileToCreate;
    }

    public String getNewImageName() {
        return newImageName;
    }
}
