package eu.amadeos.games.maze.domain;

import eu.amadeos.gameengine.tile.Cell;

/** Something that happened in a step; the user interface decides what it looks and sounds like. */
// --8<-- [start:event]
public sealed interface Event {
    /** A level begins, or a round after a lost life: "Ready!". */
    record Ready(int level, boolean newGame) implements Event {
    }

    record DotEaten(Cell at) implements Event {
    }

    /** The ghosts turn blue for so many seconds. */
    record EnergizerEaten(Cell at, double seconds) implements Event {
    }

    /** The ghosts are no longer blue. */
    record FrightOver() implements Event {
    }

    /** The muncher ate a blue ghost; its eyes fly home. */
    record GhostEaten(Kind kind, double x, double y, int points) implements Event {
    }

    record FruitShown(Fruit fruit) implements Event {
    }

    record FruitEaten(Fruit fruit, Cell at, int points) implements Event {
    }

    /** A ghost caught the muncher. */
    record Caught(double x, double y) implements Event {
    }

    record LevelCleared(int level) implements Event {
    }

    record ExtraLife(int lives) implements Event {
    }

    record GameOver(GameResult result) implements Event {
    }
}
// --8<-- [end:event]
