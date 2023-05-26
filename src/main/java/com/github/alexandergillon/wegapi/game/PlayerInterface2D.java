package com.github.alexandergillon.wegapi.game;

import java.rmi.RemoteException;
import java.util.ArrayList;

/**
 * Interface that can be used to tell a player to do something, for a 2D game. For example, show certain tiles to
 * the player, or a message. <br> <br>
 *
 * For developers of 2D games based on WEGAPI: this interface is essentially all the things you can get a player
 * to 'do'. <br> <br>
 *
 * For developers of WEGAPI itself: see BaseServer2D.java for how this interface fits into the WEGAPI model. This
 * interface is actually a wrapper on underlying (1D) functions via the PlayerInterface, and so this interface
 * is not used directly for RMI communication. <br> <br>
 *
 * A note on equality of PlayerInterface2Ds (this applies to the .equals() method, not to ==): <br> <ul>
 *  PlayerInterface2Ds should compare equal when they correspond to the same underlying player remote object. In
 *  the context of WEGAPI, this means when the same client daemon program provided them (i.e. the same player,
 *  and their daemon has never restarted). <br> <br>
 * 
 *  To elaborate further, this means that if the very same daemon (i.e. the same process) calls GameServerInterface2D
 *  functions, then their PlayerData2D.getPlayer() results will compare as equal (also including the
 *  PlayerInterface2D they provided in GameServerInterface2D.registerPlayer2D()). However, if a daemon from the
 *  same player were to crash and be restarted, their PlayerInterface2D.getPlayer() would not compare as equal to
 *  any PlayerInterface2Ds or PlayerData2D.getPlayer() supplied before the crash. And certainly PlayerInterface2Ds
 *  from different players will not compare as equal. <br> <br>
 * 
 *  This level of equality ensures that calling a PlayerInterface2D method on either one of the objects that compare
 *  equal will have the same results: i.e. that the command gets though to the same player (and the same daemon
 *  being run by that player). <br> <br>
 * 
 *  This behavior is implemented by the default implementation of a PlayerInterface2D, in
 *  BaseServer2D::PlayerInterfaceWrapper. </ul>
 */
public interface PlayerInterface2D {

    /**
     * Informs a newly-joined player of their player number.
     *
     * @param playerNumber the new player's player number
     */
    void initialize(int playerNumber) throws RemoteException;

    /**
     * Displays a message to a player, in a message box (todo: actually do it in a message box - for now, is printed
     * to the console). Optionally makes the message look like an error to the user.
     *
     * @param message the message to display
     * @param error whether or not this message represents an error to the user (if true, the message will have an
     *              error icon)
     */
    void displayMessage(String message, boolean error) throws RemoteException;

    /**
     * Creates tiles in the player's game directory. Uses one of the following modes: <br> <br>
     *
     *   - CREATE: creates tiles, whether or not they exist. <br>
     *   - CREATE_NEW: creates tiles only if they do not exist. If some tile already exists, the client will
     *                 abort and print an error message. <br>
     *   - OVERWRITE_EXISTING: only overwrites existing tiles. If some tile does not exist, the client will abort
     *                         and print an error message.
     *
     * @param tiles tiles to create in the player's game directory
     * @param mode mode (how to handle existing tiles, etc.)
     */
    void createTiles(ArrayList<Tile2D> tiles, PlayerInterface.CreateTilesMode mode) throws RemoteException;


    /**
     * Deletes tiles in the player's game directory, as specified by their indices. Uses one of the following modes: <br> <br>
     *
     *   - DELETE: deletes tiles, whether or not they exist. <br>
     *   - DELETE_EXISTING: deletes tiles only if they exist. If some tile does not exist, the client will abort
     *                      and print an error message.
     *   - DELETE_ALL: deletes all tiles. If this parameter is specified, tileIndices should be null.
     *
     * @param tileCoordinates coordinates of the tiles to delete - should be null if mode is DELETE_ALL (todo: actually handle null)
     * @param mode mode (how to handle non-existent tiles, etc.)
     */
    // todo: make this accept null
    void deleteTiles(ArrayList<TileCoordinate> tileCoordinates, PlayerInterface.DeleteTilesMode mode) throws RemoteException;

    /**
     * Informs the player that the game is over. They will not be able to do anything until a new game is started.
     *
     * @param win whether the player won
     */
    void gameOver(boolean win) throws RemoteException;
}
