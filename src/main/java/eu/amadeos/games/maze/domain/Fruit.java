package eu.amadeos.games.maze.domain;

/** The bonus that appears twice a level below the house: a different fruit, worth more, in later levels. */
public enum Fruit {
    CHERRY(100), STRAWBERRY(300), ORANGE(500), APPLE(700), GRAPES(1000);

    public final int points;

    Fruit(int points) {
        this.points = points;
    }

    /** The fruit of a level: cherries in the first, then one kind per level, grapes from the fifth on. */
    public static Fruit of(int level) {
        return values()[Math.min(level, values().length) - 1];
    }
}
