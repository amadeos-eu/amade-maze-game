package eu.amadeos.games.maze.domain;

import eu.amadeos.gameengine.ai.StateMachine;
import eu.amadeos.gameengine.tile.Cell;
import eu.amadeos.gameengine.tile.Direction;
import eu.amadeos.gameengine.tile.Walker;

/**
 * A ghost. What it does depends on its {@link Mode}, a {@link StateMachine} with a table of the changes
 * that may happen; how it chooses its way at a crossing depends on the mode too.
 */
public final class Ghost {
    // --8<-- [start:modes]
    /** What a ghost is doing. */
    public enum Mode {
        /** Waiting in the house, bobbing up and down. */
        HOUSE,
        /** On its way out of the house, to the tile in front of the door. */
        LEAVING,
        /** Off to its own corner of the maze. */
        SCATTER,
        /** Hunting the muncher, each ghost in its own way. */
        CHASE,
        /** Blue after an energizer: slow, aimless, and good to eat. */
        FRIGHTENED,
        /** Eaten: only the eyes are left, and they fly home. */
        EATEN,
        /** The eyes go through the door into the house, to come back to life. */
        ENTERING
    }

    private final StateMachine<Mode> mode = new StateMachine<>(Mode.HOUSE)
            .allow(Mode.HOUSE, Mode.LEAVING)
            .allow(Mode.LEAVING, Mode.SCATTER, Mode.CHASE, Mode.FRIGHTENED)
            .allow(Mode.SCATTER, Mode.CHASE, Mode.FRIGHTENED)
            .allow(Mode.CHASE, Mode.SCATTER, Mode.FRIGHTENED)
            .allow(Mode.FRIGHTENED, Mode.SCATTER, Mode.CHASE, Mode.EATEN)
            .allow(Mode.EATEN, Mode.ENTERING)
            .allow(Mode.ENTERING, Mode.LEAVING)
            .onChange(this::changed);
    // --8<-- [end:modes]

    private final Kind kind;
    private final Maze maze;
    private final Walker walker;
    private final Walker.Steering steering = this::choose;
    /** The game of the current step, for the steering. */
    private Game game;

    Ghost(Kind kind, Maze maze) {
        this.kind = kind;
        this.maze = maze;
        Maze.Start start = maze.ghost(kind);
        this.walker = new Walker(start.cell().col(), start.cell().row(), start.heading());
        reset(Mode.SCATTER);
    }

    /** Back to the start of a round: in the house, or, if it starts outside, in {@code outside}. */
    void reset(Mode outside) {
        Maze.Start start = maze.ghost(kind);
        walker.place(start.cell().col(), start.cell().row(), start.heading());
        boolean inHouse = maze.tile(start.cell().col(), start.cell().row()) == Tile.HOUSE;
        mode.reset(inHouse ? Mode.HOUSE : outside);
    }

    /** For tests: somewhere, in a mode. */
    void put(int col, int row, Direction heading, Mode mode) {
        walker.place(col, row, heading);
        this.mode.reset(mode);
    }

    // --8<-- [start:turn]
    /** A hunting ghost that changes its mind turns round at once: the player sees the change coming. */
    private void changed(Mode from, Mode to) {
        boolean roaming = from == Mode.SCATTER || from == Mode.CHASE;
        if (roaming && to != Mode.EATEN) walker.turnAround();
    }
    // --8<-- [end:turn]

    void update(double seconds, Game game) {
        this.game = game;
        mode.update(seconds);
        if (mode.is(Mode.HOUSE) && game.roundTime() >= maze.release(kind) * Field.releaseFactor(game.level())) {
            mode.to(Mode.LEAVING);
        }
        walker.walk(game.speed(this) * seconds, game.tiles(), steering);
    }

    // --8<-- [start:choose]
    /** At the centre of a tile: which way now? */
    private Direction choose(int col, int row, Direction heading) {
        Cell here = new Cell(col, row);
        return switch (mode.state()) {
            case HOUSE -> bob(col, row, heading);
            case LEAVING -> {
                if (!here.equals(maze.doorstep())) yield maze.toDoorstep().downhill(col, row);
                // Out of the house while the others are blue: blue too. Back to life after being eaten: not.
                boolean blue = game.frightened() && mode.previous() == Mode.HOUSE;
                mode.to(blue ? Mode.FRIGHTENED : game.scheduled());
                yield Direction.LEFT;
            }
            case EATEN -> {
                if (!here.equals(maze.doorstep())) yield maze.toDoorstep().downhill(col, row);
                mode.to(Mode.ENTERING);
                yield Direction.DOWN;
            }
            case ENTERING -> {
                if (!here.equals(maze.house())) yield maze.toHouse().downhill(col, row);
                mode.to(Mode.LEAVING);
                yield maze.toDoorstep().downhill(col, row);
            }
            case FRIGHTENED -> wander(col, row, heading);
            case SCATTER -> towards(col, row, heading, corner());
            case CHASE -> towards(col, row, heading, kind.target(game, this));
        };
    }

    /**
     * The rule of the arcade: never back the way it came; of the other open ways, the one whose next tile is
     * nearest to the target, as the crow flies. Of equally near ones, the first of up, left, down, right.
     */
    private Direction towards(int col, int row, Direction heading, Cell target) {
        Direction best = null;
        long bestDistance = Long.MAX_VALUE;
        for (Direction d : Direction.values()) {
            if (heading != null && d == heading.opposite()) continue;
            Cell next = game.tiles().step(col, row, d);
            if (!game.tiles().get(next).corridor()) continue;
            long distance = distanceSquared(next, target);
            if (distance < bestDistance) {
                best = d;
                bestDistance = distance;
            }
        }
        return best != null ? best : heading.opposite();     // a dead end: the only way is back
    }
    // --8<-- [end:choose]

    /** Blue: a random open way, but not back. */
    private Direction wander(int col, int row, Direction heading) {
        Direction[] open = new Direction[4];
        int n = 0;
        for (Direction d : Direction.values()) {
            if (heading != null && d == heading.opposite()) continue;
            if (game.tiles().get(game.tiles().step(col, row, d)).corridor()) open[n++] = d;
        }
        return n == 0 ? heading.opposite() : open[game.random().nextInt(n)];
    }

    /** In the house: up and down between its walls. */
    private Direction bob(int col, int row, Direction heading) {
        Direction way = heading == null ? Direction.UP : heading;
        if (way.horizontal()) way = Direction.UP;
        boolean open = maze.tile(col + way.dx, row + way.dy) == Tile.HOUSE;
        return open ? way : way.opposite();
    }

    static long distanceSquared(Cell a, Cell b) {
        long dx = a.col() - b.col();
        long dy = a.row() - b.row();
        return dx * dx + dy * dy;
    }

    /** Eaten while blue: the eyes fly home. */
    void eaten() {
        mode.to(Mode.EATEN);
    }

    /** The energizer's spell: blue now, or back to the schedule. */
    void frighten() {
        if (mode.is(Mode.SCATTER) || mode.is(Mode.CHASE)) mode.to(Mode.FRIGHTENED);
    }

    void follow(Mode scheduled) {
        if ((mode.is(Mode.SCATTER) || mode.is(Mode.CHASE) || mode.is(Mode.FRIGHTENED)) && !mode.is(scheduled)) {
            mode.to(scheduled);
        }
    }

    public Kind kind() {
        return kind;
    }

    public Walker walker() {
        return walker;
    }

    public Mode mode() {
        return mode.state();
    }

    /** True if touching it costs the muncher a life. */
    public boolean dangerous() {
        return mode.is(Mode.SCATTER) || mode.is(Mode.CHASE);
    }

    /** True for the eyes on their way home and into the house. */
    public boolean eyes() {
        return mode.is(Mode.EATEN) || mode.is(Mode.ENTERING);
    }

    public Cell corner() {
        return maze.corner(kind);
    }
}
