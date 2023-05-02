package com.github.alexandergillon.wegapi.game;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

import com.github.alexandergillon.wegapi.game.game_action.GameAction;

public interface GameInterface extends Remote {
    public static class Tile {
        private final int index;
        private final String imageName;

        public Tile(int index, String imageName) {
            this.index = index;
            this.imageName = imageName;
        }

        public int getIndex() {
            return index;
        }

        public String getImageName() {
            return imageName;
        }
    }

    /**
     * Informs the server that a client has joined the game for the first time.
     *
     * @return GameActions that inform the client how to set up their local directory and state
     */
    ArrayList<GameAction> clientInit() throws RemoteException;

    /**
     * Informs the server that a certain player double-clicked a certain tile.
     *
     * @param tile the index of the tile that the player clicked
     * @param player which player clicked the tile
     * @return GameActions that inform the client how to update their local state, based on this action
     */
    ArrayList<GameAction> tileClicked(int tile, int player) throws RemoteException;

    /**
     * Informs the server that a certain player dragged one tile to another.
     *
     * @param fromTile the index of the tile that was dragged
     * @param toTile the index of the tile that the tile was dragged to
     * @param player the player who dragged the tile
     * @return GameActions that inform the client how to update their local state, based on this action
     */
    ArrayList<GameAction> tileDragged(int fromTile, int toTile, int player) throws RemoteException;
}