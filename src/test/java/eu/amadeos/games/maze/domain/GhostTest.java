package eu.amadeos.games.maze.domain;

import eu.amadeos.gameengine.tile.Cell;
import eu.amadeos.gameengine.tile.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GhostTest {
    private static final double STEP = 1.0 / 120;

    // --8<-- [start:test-targets]
    @Test
    void eachGhostAimsAtATileOfItsOwn() {
        Game game = new Game(1);
        game.putMuncher(13, 22, Direction.LEFT);
        game.putGhost(Kind.RED, 13, 11, Direction.LEFT, Ghost.Mode.CHASE);
        game.putGhost(Kind.ORANGE, 25, 1, Direction.LEFT, Ghost.Mode.CHASE);
        assertEquals(new Cell(13, 22), Kind.RED.target(game, game.ghost(Kind.RED)), "the muncher itself");
        assertEquals(new Cell(9, 22), Kind.PINK.target(game, game.ghost(Kind.PINK)), "four tiles ahead of it");
        // Two tiles ahead is (11, 22); red is 11 tiles above that, so cyan aims 11 tiles below it.
        assertEquals(new Cell(9, 33), Kind.CYAN.target(game, game.ghost(Kind.CYAN)));
        assertEquals(new Cell(13, 22), Kind.ORANGE.target(game, game.ghost(Kind.ORANGE)), "far away: hunts");
        game.putGhost(Kind.ORANGE, 13, 19, Direction.LEFT, Ghost.Mode.CHASE);
        assertEquals(new Cell(0, 31), Kind.ORANGE.target(game, game.ghost(Kind.ORANGE)), "close: off to its corner");
    }

    @Test
    void atACrossingAGhostTakesTheWayNearestItsTargetButNeverBack() {
        Game game = new Game(1);
        game.skipReady();
        game.putMuncher(6, 22, null);
        // At (6, 5) the ways lead up, down and right; left is back. The muncher is far below.
        game.putGhost(Kind.RED, 6, 5, Direction.RIGHT, Ghost.Mode.CHASE);
        game.update(STEP, null);
        assertEquals(Direction.DOWN, game.ghost(Kind.RED).walker().heading());
        // In its corner up right while scattering, the same ghost at the same crossing goes up.
        game.putGhost(Kind.RED, 6, 5, Direction.RIGHT, Ghost.Mode.SCATTER);
        game.update(STEP, null);
        assertEquals(Direction.RIGHT, game.ghost(Kind.RED).walker().heading(), "towards (24, -3): right is nearer than up");
    }
    // --8<-- [end:test-targets]
}
