package com.github.alexandergillon.wegapi.game;

public interface GameInterface {
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
}
