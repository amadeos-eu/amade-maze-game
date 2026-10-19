package eu.amadeos.games.maze.domain;

import eu.amadeos.gameengine.tile.Direction;
import eu.amadeos.gameengine.tile.TileMap;
import eu.amadeos.gameengine.tile.Walker;

/**
 * The player's muncher. It walks on as long as it can, and turns where the player wants as soon as that way
 * is open: a key pressed a little early is kept until the next crossing, so nobody has to hit the centre
 * of a tile. Back the way it came it turns at once.
 */
public final class Muncher {
    private final Walker walker;
    /** The way it looks: where it walks, or walked last. */
    private Direction facing;
    /** Tiles walked so far: the mouth opens and closes with them, and stands still with the muncher. */
    private double walked;
    private TileMap<Tile> tiles;
    private Direction wanted;
    private final Walker.Steering steering = this::choose;

    Muncher(Maze.Start start) {
        walker = new Walker(start.cell().col(), start.cell().row(), start.heading());
        facing = start.heading();
    }

    // --8<-- [start:move]
    /** One step: {@code wanted} is the way the player asks for, {@code null} for none. */
    void move(double distance, TileMap<Tile> tiles, Direction wanted) {
        this.tiles = tiles;
        this.wanted = wanted;
        if (wanted != null && walker.heading() == wanted.opposite()) walker.turnAround();
        walked += walker.walk(distance, tiles, steering);
        if (walker.heading() != null) facing = walker.heading();
    }

    /** At a centre: the wanted way if it is open, else on, else stop at the wall. */
    private Direction choose(int col, int row, Direction heading) {
        if (wanted != null && open(col, row, wanted)) return wanted;
        if (heading != null && open(col, row, heading)) return heading;
        return null;
    }

    private boolean open(int col, int row, Direction way) {
        return tiles.get(tiles.step(col, row, way)).corridor();
    }
    // --8<-- [end:move]

    public Walker walker() {
        return walker;
    }

    public Direction facing() {
        return facing;
    }

    public double walked() {
        return walked;
    }
}
