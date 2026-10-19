package eu.amadeos.games.maze.domain;

import java.util.Map;

/** What a tile of the maze holds; {@link #LEGEND} gives the letters of {@code maze.txt}. */
public enum Tile {
    WALL, FLOOR, DOT, ENERGIZER, TUNNEL, DOOR, HOUSE;

    public static final Map<Character, Tile> LEGEND = Map.of('X', WALL, '_', FLOOR, '.', DOT, 'o', ENERGIZER,
            'T', TUNNEL, '-', DOOR, 'H', HOUSE);

    /** Where the muncher may go, and the ghosts while they roam: everywhere but walls and the house. */
    public boolean corridor() {
        return this != WALL && this != DOOR && this != HOUSE;
    }

    /** Something to eat. */
    public boolean food() {
        return this == DOT || this == ENERGIZER;
    }
}
