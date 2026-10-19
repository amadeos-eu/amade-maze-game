package eu.amadeos.games.maze.domain;

import eu.amadeos.gameengine.path.AStar;
import eu.amadeos.gameengine.path.DistanceMap;
import eu.amadeos.gameengine.random.SeededRandom;
import eu.amadeos.gameengine.tile.Cell;
import eu.amadeos.gameengine.tile.Direction;
import eu.amadeos.gameengine.tile.TileMap;

/**
 * The computer player of the title screen. Each time the muncher comes to a new tile, it makes a plan: the
 * nearest dot, or a blue ghost close by, is the goal, found with breadth-first search; the way there is
 * the cheapest one A* finds, where tiles near a hunting ghost cost a lot. It looks away now and then, as a
 * person does, and then it does not see the ghosts: it plays well enough to watch and loses in the end.
 */
public final class Pilot {
    /** What entering a tile near a hunting ghost costs on top of the step: right next to it, and a little further. */
    static final double NEAR = 400;
    static final double CLOSE = 60;
    /** How far ahead of a ghost, along its corridor, the pilot feels danger. */
    static final int AHEAD = 8;
    /** A blue ghost within so many steps is worth chasing, if the blue lasts. */
    static final int HUNT_RANGE = 10;
    /** How often per second the pilot looks away, and for how long. */
    static final double DISTRACTIONS = 0.1;
    static final double DISTRACTION = 1.0;

    private final SeededRandom random;
    private Cell planned;
    private Direction way;
    private double distracted;
    /** The last plan, for the screenshot of the chapter. */
    private AStar.Path path;
    private DistanceMap wave;

    public Pilot(long seed) {
        random = new SeededRandom(seed).fork();
    }

    // --8<-- [start:steer]
    /** The way the pilot wants to go in this step. */
    public Direction steer(Game game, double seconds) {
        distracted -= seconds;
        if (distracted <= 0 && random.nextDouble() < DISTRACTIONS * seconds) distracted = DISTRACTION;
        if (game.phase() != Game.Phase.PLAYING) {
            planned = null;
            return null;
        }
        Cell at = game.muncher().walker().cell();
        if (!at.equals(planned)) {
            planned = at;
            way = plan(game, at);
        }
        return way;
    }

    private Direction plan(Game game, Cell at) {
        TileMap<Tile> tiles = game.tiles();
        wave = DistanceMap.search(tiles, (col, row) -> tiles.get(col, row).corridor(), at);
        Cell goal = goal(game, wave);
        if (goal == null) return null;
        boolean careful = distracted <= 0;
        path = AStar.find(tiles, at, goal, (col, row) -> {
            if (!tiles.get(col, row).corridor()) return Double.POSITIVE_INFINITY;
            return careful ? 1 + danger(game, col, row) : 1;
        });
        return path.first();
    }
    // --8<-- [end:steer]

    // --8<-- [start:goal]
    /** A blue ghost nearby while the blue lasts; else the nearest dot, by the steps to it. */
    private static Cell goal(Game game, DistanceMap wave) {
        Cell best = null;
        int bestDistance = DistanceMap.UNREACHABLE;
        if (game.frightLeft() > 2) {
            for (Ghost ghost : game.ghosts()) {
                if (ghost.mode() != Ghost.Mode.FRIGHTENED) continue;
                Cell cell = ghost.walker().cell();
                int distance = wave.distance(cell);
                if (distance <= HUNT_RANGE && distance < bestDistance) {
                    best = cell;
                    bestDistance = distance;
                }
            }
            if (best != null) return best;
        }
        for (Cell dot : game.tiles().cells(Tile::food)) {
            int distance = wave.distance(dot);
            if (distance < bestDistance) {
                best = dot;
                bestDistance = distance;
            }
        }
        return best;
    }

    /**
     * The cost of being near a hunting ghost: high next to it, less two or three tiles away, and the
     * corridor ahead of it, where it is going, costs too.
     */
    public static double danger(Game game, int col, int row) {
        double cost = 0;
        TileMap<Tile> tiles = game.tiles();
        for (Ghost ghost : game.ghosts()) {
            if (!ghost.dangerous()) continue;
            Cell at = ghost.walker().cell();
            int dx = Math.abs(at.col() - col);
            dx = Math.min(dx, tiles.width() - dx);
            int steps = dx + Math.abs(at.row() - row);
            if (steps <= 1) cost += NEAR;
            else if (steps <= 3) cost += CLOSE;
            Direction heading = ghost.walker().heading();
            if (heading == null) continue;
            for (int i = 0; i < AHEAD && tiles.get(at = tiles.step(at.col(), at.row(), heading)).corridor(); i++) {
                if (at.col() == col && at.row() == row) cost += CLOSE;
            }
        }
        return cost;
    }
    // --8<-- [end:goal]

    /** The way of the last plan, for drawing; {@code null} before the first. */
    public AStar.Path path() {
        return path;
    }

    /** The distances of the last plan, from the muncher's tile. */
    public DistanceMap wave() {
        return wave;
    }
}
