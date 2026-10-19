package eu.amadeos.games.maze.domain;

import eu.amadeos.gameengine.random.SeededRandom;
import eu.amadeos.gameengine.tile.Cell;
import eu.amadeos.gameengine.tile.Direction;
import eu.amadeos.gameengine.tile.TileMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * A game of Maze. Plain Java without a clock: {@link #update} advances everything by a step, with the way
 * the player wants to go in that step, and the {@link #events()} tell what happened in it.
 *
 * <p>The rules live on tiles: the maze is a {@link TileMap}, the muncher and the ghosts walk from the centre
 * of one tile to the next, and they meet when they come close. Chance (where a blue ghost turns) comes from
 * a {@link SeededRandom} made from the seed, so a test can play the same game twice.
 */
public final class Game {
    /**
     * "Ready!" before a round; playing; the muncher was caught and folds up; the maze is cleared and
     * flashes; the last life is lost.
     */
    public enum Phase { READY, PLAYING, DYING, CLEARED, OVER }

    private final long seed;
    private final SeededRandom random;
    private final Maze maze;
    private final Map<Kind, Ghost> ghosts = new EnumMap<>(Kind.class);
    private final List<Event> events = new ArrayList<>();
    private TileMap<Tile> tiles;
    private Muncher muncher;
    private Phase phase = Phase.READY;
    private double phaseTime;
    private int level = 1;
    private int score;
    private int lives = Field.START_LIVES;
    private boolean extraLifeGiven;
    private int dotsLeft;
    private int dotsEaten;
    /** Seconds since the round began: the ghosts leave the house by it. */
    private double roundTime;
    /** Where in {@link Field#SCHEDULE} the ghosts are, and for how long already. */
    private int scheduleStep;
    private double scheduleTime;
    private double frightLeft;
    private int ghostsEaten;
    /** Everything waits a moment after a ghost is eaten. */
    private double pause;
    private Fruit fruit;
    private double fruitLeft;
    private int fruitsShown;

    public Game(long seed) {
        this(seed, Maze.STANDARD);
    }

    public Game(long seed, Maze maze) {
        this.seed = seed;
        this.random = new SeededRandom(seed);
        this.maze = maze;
        for (Kind kind : Kind.values()) ghosts.put(kind, new Ghost(kind, maze));
        startLevel(true);
    }

    // --8<-- [start:update]
    /** One step of {@code seconds}; {@code wanted} is the way the player asks for, {@code null} for none. */
    public void update(double seconds, Direction wanted) {
        events.clear();
        phaseTime += seconds;
        switch (phase) {
            case READY -> {
                if (phaseTime >= readySeconds()) phase(Phase.PLAYING);
            }
            case PLAYING -> play(seconds, wanted);
            case DYING -> {
                if (phaseTime >= Field.DYING_SECONDS) loseLife();
            }
            case CLEARED -> {
                if (phaseTime >= Field.CLEARED_SECONDS) {
                    level++;
                    startLevel(false);
                }
            }
            case OVER -> {
            }
        }
    }

    private void play(double seconds, Direction wanted) {
        if (pause > 0) {
            pause -= seconds;
            return;
        }
        roundTime += seconds;
        schedule(seconds);
        muncher.move(Field.MUNCHER_SPEED * Field.pace(level) * seconds, tiles, wanted);
        eat();
        if (meet()) return;
        for (Ghost ghost : ghosts.values()) ghost.update(seconds, this);
        if (meet()) return;
        fruit(seconds);
        if (dotsLeft == 0) {
            phase(Phase.CLEARED);
            events.add(new Event.LevelCleared(level));
        }
    }
    // --8<-- [end:update]

    // --8<-- [start:schedule]
    /** Scatter and chase by turns; while the energizer works, the schedule waits. */
    private void schedule(double seconds) {
        if (frightLeft > 0) {
            frightLeft -= seconds;
            if (frightLeft <= 0) {
                frightLeft = 0;
                for (Ghost ghost : ghosts.values()) ghost.follow(scheduled());
                events.add(new Event.FrightOver());
            }
            return;
        }
        scheduleTime += seconds;
        if (scheduleStep < Field.SCHEDULE.length && scheduleTime >= Field.SCHEDULE[scheduleStep]) {
            scheduleStep++;
            scheduleTime = 0;
            for (Ghost ghost : ghosts.values()) ghost.follow(scheduled());
        }
    }

    /** What the ghosts outside should be doing now: even steps of the schedule scatter, odd ones chase. */
    Ghost.Mode scheduled() {
        return scheduleStep % 2 == 0 && scheduleStep < Field.SCHEDULE.length ? Ghost.Mode.SCATTER : Ghost.Mode.CHASE;
    }
    // --8<-- [end:schedule]

    // --8<-- [start:eat]
    /** What lies on the muncher's tile is eaten. */
    private void eat() {
        Cell at = muncher.walker().cell();
        Tile tile = tiles.get(at);
        if (tile.food()) {
            tiles.set(at.col(), at.row(), Tile.FLOOR);
            dotsLeft--;
            dotsEaten++;
            if (tile == Tile.DOT) {
                addPoints(Field.DOT_POINTS);
                events.add(new Event.DotEaten(at));
            } else {
                addPoints(Field.ENERGIZER_POINTS);
                frightLeft = Field.frightened(level);
                ghostsEaten = 0;
                for (Ghost ghost : ghosts.values()) ghost.frighten();
                events.add(new Event.EnergizerEaten(at, frightLeft));
            }
            for (int after : Field.FRUIT_AFTER) {
                if (dotsEaten == after && fruitsShown < Field.FRUIT_AFTER.length) {
                    fruitsShown++;
                    fruit = Fruit.of(level);
                    fruitLeft = Field.FRUIT_SECONDS;
                    events.add(new Event.FruitShown(fruit));
                }
            }
        }
        if (fruit != null && at.equals(maze.fruit())) {
            addPoints(fruit.points);
            events.add(new Event.FruitEaten(fruit, at, fruit.points));
            fruit = null;
        }
    }
    // --8<-- [end:eat]

    // --8<-- [start:meet]
    /**
     * The muncher meets the ghosts near it: a blue one is eaten, a hunting one catches it. Tested before and
     * after the ghosts move, so two that walk towards each other never pass through each other.
     */
    private boolean meet() {
        double mx = muncher.walker().x();
        double my = muncher.walker().y();
        for (Ghost ghost : ghosts.values()) {
            double dx = Math.abs(ghost.walker().x() - mx);
            dx = Math.min(dx, tiles.width() - dx);          // through the tunnel too
            double dy = ghost.walker().y() - my;
            if (dx * dx + dy * dy >= Field.CATCH_DISTANCE * Field.CATCH_DISTANCE) continue;
            if (ghost.mode() == Ghost.Mode.FRIGHTENED) {
                int points = Field.GHOST_POINTS << Math.min(ghostsEaten, 3);
                ghostsEaten++;
                ghost.eaten();
                addPoints(points);
                pause = Field.EATEN_PAUSE;
                events.add(new Event.GhostEaten(ghost.kind(), ghost.walker().x(), ghost.walker().y(), points));
            } else if (ghost.dangerous()) {
                phase(Phase.DYING);
                events.add(new Event.Caught(mx, my));
                return true;
            }
        }
        return false;
    }
    // --8<-- [end:meet]

    private void fruit(double seconds) {
        if (fruit == null) return;
        fruitLeft -= seconds;
        if (fruitLeft <= 0) fruit = null;
    }

    private void addPoints(int points) {
        if (phase == Phase.OVER) return;
        score += points;
        if (!extraLifeGiven && score >= Field.EXTRA_LIFE) {
            extraLifeGiven = true;
            if (lives < Field.MAX_LIVES) {
                lives++;
                events.add(new Event.ExtraLife(lives));
            }
        }
    }

    private void loseLife() {
        lives--;
        if (lives == 0) {
            phase(Phase.OVER);
            events.add(new Event.GameOver(new GameResult(score, level)));
        } else {
            startRound(false);
        }
    }

    /** A fresh maze with all its dots, and a round. */
    private void startLevel(boolean newGame) {
        tiles = maze.tiles();
        dotsLeft = tiles.count(Tile::food);
        dotsEaten = 0;
        fruitsShown = 0;
        startRound(newGame);
    }

    /** Everybody to the start, the ghosts into the house, "Ready!". */
    private void startRound(boolean newGame) {
        muncher = new Muncher(maze.muncher());
        roundTime = 0;
        scheduleStep = 0;
        scheduleTime = 0;
        frightLeft = 0;
        pause = 0;
        fruit = null;
        for (Ghost ghost : ghosts.values()) ghost.reset(Ghost.Mode.SCATTER);
        phase(Phase.READY);
        events.add(new Event.Ready(level, newGame));
    }

    private void phase(Phase phase) {
        this.phase = phase;
        this.phaseTime = 0;
    }

    private double readySeconds() {
        return level == 1 && lives == Field.START_LIVES && score == 0
                ? Field.FIRST_READY_SECONDS : Field.READY_SECONDS;
    }

    // --8<-- [start:speed]
    /** How fast a ghost walks now, in tiles per second. */
    double speed(Ghost ghost) {
        double pace = Field.pace(level);
        return switch (ghost.mode()) {
            case HOUSE, LEAVING -> Field.HOUSE_SPEED;
            case EATEN, ENTERING -> Field.EYES_SPEED;
            case FRIGHTENED -> Field.FRIGHTENED_SPEED * pace;
            case SCATTER, CHASE -> tiles.get(ghost.walker().cell()) == Tile.TUNNEL
                    ? Field.TUNNEL_SPEED * pace : Field.GHOST_SPEED * pace;
        };
    }
    // --8<-- [end:speed]

    // ---- for the ghosts, the pilot and the painter ------------------------------------------------

    public long seed() {
        return seed;
    }

    public Maze maze() {
        return maze;
    }

    /** The maze of this level, with the dots still to eat. Read only. */
    public TileMap<Tile> tiles() {
        return tiles;
    }

    public Muncher muncher() {
        return muncher;
    }

    public Ghost ghost(Kind kind) {
        return ghosts.get(kind);
    }

    public Iterable<Ghost> ghosts() {
        return Collections.unmodifiableCollection(ghosts.values());
    }

    public Phase phase() {
        return phase;
    }

    /** Seconds in the current phase. */
    public double phaseTime() {
        return phaseTime;
    }

    public int level() {
        return level;
    }

    public int score() {
        return score;
    }

    /** Lives left, counting the one playing. */
    public int lives() {
        return lives;
    }

    public int dotsLeft() {
        return dotsLeft;
    }

    double roundTime() {
        return roundTime;
    }

    SeededRandom random() {
        return random;
    }

    /** True while the energizer works. */
    public boolean frightened() {
        return frightLeft > 0;
    }

    /** Seconds the ghosts stay blue. */
    public double frightLeft() {
        return frightLeft;
    }

    /** True for a moment after a ghost is eaten: nothing moves. */
    public boolean paused() {
        return pause > 0;
    }

    /** The fruit on the field, {@code null} if none. */
    public Fruit fruit() {
        return fruit;
    }

    /** What happened in the last step. */
    public List<Event> events() {
        return Collections.unmodifiableList(events);
    }

    // ---- for tests ---------------------------------------------------------------------------------

    /** Eats all dots but the one at {@code col}, {@code row}. */
    void leaveOnly(int col, int row) {
        for (Cell cell : tiles.cells(Tile::food)) {
            if (cell.col() != col || cell.row() != row) tiles.set(cell.col(), cell.row(), Tile.FLOOR);
        }
        dotsLeft = tiles.get(col, row).food() ? 1 : 0;
    }

    /** Puts the muncher somewhere. */
    void putMuncher(int col, int row, Direction heading) {
        muncher.walker().place(col, row, heading);
    }

    /** Puts a ghost somewhere, in a mode. */
    void putGhost(Kind kind, int col, int row, Direction heading, Ghost.Mode mode) {
        ghosts.get(kind).put(col, row, heading, mode);
    }

    /** Skips "Ready!". */
    void skipReady() {
        if (phase == Phase.READY) phase(Phase.PLAYING);
    }
}
