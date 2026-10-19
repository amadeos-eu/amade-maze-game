package eu.amadeos.games.maze.domain;

/** The numbers of the game: speeds in tiles per second, times in seconds, points. */
public final class Field {
    /** The logical size of the screen; the maze is drawn in its middle. */
    public static final double WIDTH = 640;
    public static final double HEIGHT = 400;

    // --8<-- [start:speeds]
    /** Speeds in the first level, in tiles per second; later levels are faster. */
    public static final double MUNCHER_SPEED = 7.6;
    public static final double GHOST_SPEED = 7.2;
    /** Blue ghosts are slow, and so is everybody in the tunnel, except the muncher. */
    public static final double FRIGHTENED_SPEED = 4.4;
    public static final double TUNNEL_SPEED = 3.8;
    /** The eyes of an eaten ghost fly home fast. */
    public static final double EYES_SPEED = 14;
    /** Waiting in the house, bobbing up and down, and leaving it. */
    public static final double HOUSE_SPEED = 3.2;
    // --8<-- [end:speeds]

    // --8<-- [start:schedule]
    /**
     * Seconds of scattering and hunting, by turns: scatter 7, hunt 20, scatter 7, hunt 20, scatter 5, hunt 20,
     * scatter 5, then hunt until the end of the round. Scattering gives the player room to breathe.
     */
    static final double[] SCHEDULE = {7, 20, 7, 20, 5, 20, 5};
    // --8<-- [end:schedule]

    /** Points. */
    public static final int DOT_POINTS = 10;
    public static final int ENERGIZER_POINTS = 50;
    /** The first ghost eaten after an energizer; each further one is worth twice as much. */
    public static final int GHOST_POINTS = 200;

    public static final int START_LIVES = 3;
    public static final int MAX_LIVES = 6;
    public static final int EXTRA_LIFE = 10_000;

    /** The fruit appears when so many dots are eaten, and stays this long. */
    static final int[] FRUIT_AFTER = {70, 170};
    public static final double FRUIT_SECONDS = 9.5;

    /** "Ready!" before a round; longer at the start of a game, for the tune. */
    public static final double READY_SECONDS = 2.0;
    public static final double FIRST_READY_SECONDS = 4.2;
    /** The muncher folds up. */
    public static final double DYING_SECONDS = 1.8;
    /** The maze flashes before the next level. */
    public static final double CLEARED_SECONDS = 2.2;
    /** Everything stops for a moment when a ghost is eaten, to show its points. */
    public static final double EATEN_PAUSE = 0.6;
    /** Blue ghosts flash white during their last seconds. */
    public static final double FLASH_SECONDS = 2;
    /** The muncher and a ghost closer than this, in tiles, meet. */
    public static final double CATCH_DISTANCE = 0.6;

    private Field() {
    }

    /** Later levels are faster: 4 % per level, at most a fifth more. */
    public static double pace(int level) {
        return 1 + Math.min(level - 1, 5) * 0.04;
    }

    /** How long the ghosts stay blue after an energizer: 6 seconds in the first level, 2 at least. */
    public static double frightened(int level) {
        return Math.max(2, 7 - level);
    }

    /** The ghosts leave the house sooner in later levels. */
    public static double releaseFactor(int level) {
        return Math.max(0.2, 1 - 0.25 * (level - 1));
    }
}
