package com.github.alexandergillon.wegapi.server;

import com.github.alexandergillon.wegapi.game.PlayerInterface;

import java.rmi.RemoteException;
import java.awt.Point;

public abstract class BaseServer2D extends BaseServer {
    private final int rows;
    private final int cols;

    public BaseServer2D(int rows, int cols) throws RemoteException {
        super();
        this.rows = rows;
        this.cols = cols;
    }

    @Override
    public abstract void registerPlayer(PlayerInterface player) throws RemoteException;
    public abstract void tileClicked2D(int row, int col, PlayerData playerData);
    public abstract void tileDragged2D(int fromRow, int fromCol, int toRow, int toCol, PlayerData playerData);
    // must implement a main method

    public final int getRows() {
        return rows;
    }

    public final int getCols() {
        return cols;
    }

    private Point indexToCoords(int index) {
        return new Point(index / cols, index % cols);
    }

    @Override
    public final void tileClicked(int tileIndex, PlayerData playerData) throws RemoteException {
        Point coords = indexToCoords(tileIndex);
        tileClicked2D(coords.x, coords.y, playerData);
    }

    @Override
    public final void tileDragged(int fromTileIndex, int toTileIndex, PlayerData playerData) throws RemoteException {
        Point fromCoords = indexToCoords(fromTileIndex);
        Point toCoords = indexToCoords(toTileIndex);
        tileDragged2D(fromCoords.x, fromCoords.y, toCoords.x, toCoords.y, playerData);
    }

}
