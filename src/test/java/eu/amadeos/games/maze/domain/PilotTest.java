package eu.amadeos.games.maze.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PilotTest {
    private static final double STEP = 1.0 / 120;

    // --8<-- [start:test-demo]
    @Test
    void thePilotClearsALevelAndLosesItsLivesInTheEnd() {
        int best = 0;
        int levels = 0;
        int ended = 0;
        for (long seed = 1; seed <= 5; seed++) {
            Game game = new Game(seed);
            Pilot pilot = new Pilot(game.seed());
            for (int i = 0; i < 10 * 60 * 120 && game.phase() != Game.Phase.OVER; i++) {
                game.update(STEP, pilot.steer(game, STEP));
            }
            System.out.println("seed " + seed + ": " + game.score() + " points, level " + game.level() + ", " + game.phase());
            if (game.phase() == Game.Phase.OVER) ended++;
            best = Math.max(best, game.score());
            levels = Math.max(levels, game.level());
        }
        assertTrue(best >= 5000, "best " + best);
        assertTrue(levels >= 2, "reached level " + levels);
        assertEquals(5, ended, "every game ends within ten minutes, so the title shows the end too");
    }
    // --8<-- [end:test-demo]
}
