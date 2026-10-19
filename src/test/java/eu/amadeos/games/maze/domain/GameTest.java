package eu.amadeos.games.maze.domain;

import eu.amadeos.gameengine.tile.Cell;
import eu.amadeos.gameengine.tile.Direction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameTest {
    private static final double STEP = 1.0 / 120;

    private Game game;
    private final List<Event> events = new ArrayList<>();

    @BeforeEach
    void create() {
        game = new Game(25);
        game.skipReady();
    }

    private void steps(int n, Direction wanted) {
        for (int i = 0; i < n; i++) {
            game.update(STEP, wanted);
            events.addAll(game.events());
        }
    }

    private void seconds(double seconds, Direction wanted) {
        steps((int) Math.round(seconds / STEP), wanted);
    }

    private <E extends Event> List<E> events(Class<E> type) {
        return events.stream().filter(type::isInstance).map(type::cast).toList();
    }

    // --8<-- [start:test-eat]
    @Test
    void theMuncherEatsTheDotsItPasses() {
        seconds(0.25, null);
        assertEquals(List.of(new Event.DotEaten(new Cell(12, 22)), new Event.DotEaten(new Cell(11, 22))),
                events(Event.DotEaten.class), "left from the start, one dot per tile");
        assertEquals(2 * Field.DOT_POINTS, game.score());
        assertEquals(Tile.FLOOR, game.tiles().get(12, 22));
        assertEquals(238, game.dotsLeft());
    }

    @Test
    void aKeyPressedEarlyIsKeptUntilTheWayIsOpen() {
        // At the start, up is a wall; one tile to the left, the way up opens, and the muncher takes it.
        seconds(0.1, Direction.UP);
        assertEquals(Direction.LEFT, game.muncher().walker().heading());
        seconds(0.3, Direction.UP);
        assertEquals(12, game.muncher().walker().col());
        assertEquals(Direction.UP, game.muncher().walker().heading());
    }
    // --8<-- [end:test-eat]

    // --8<-- [start:test-energizer]
    @Test
    void anEnergizerTurnsTheHuntersBlueAndTheyAreWorthMoreAndMore() {
        game.putMuncher(3, 22, Direction.LEFT);
        game.putGhost(Kind.RED, 20, 28, Direction.LEFT, Ghost.Mode.CHASE);
        seconds(0.3, null);
        assertEquals(List.of(new Event.EnergizerEaten(new Cell(1, 22), 6)), events(Event.EnergizerEaten.class));
        assertEquals(Ghost.Mode.FRIGHTENED, game.ghost(Kind.RED).mode());
        assertEquals(Direction.RIGHT, game.ghost(Kind.RED).walker().heading(), "turned round at once");
        assertEquals(Ghost.Mode.HOUSE, game.ghost(Kind.ORANGE).mode(), "still waiting in the house");
        // Two blue ghosts walk into the muncher.
        game.putMuncher(4, 28, Direction.RIGHT);
        game.putGhost(Kind.RED, 7, 28, Direction.LEFT, Ghost.Mode.FRIGHTENED);
        game.putGhost(Kind.PINK, 10, 28, Direction.LEFT, Ghost.Mode.FRIGHTENED);
        seconds(1.5, null);
        assertEquals(List.of(200, 400), events(Event.GhostEaten.class).stream().map(Event.GhostEaten::points).toList());
        assertTrue(game.ghost(Kind.RED).eyes());
    }

    @Test
    void theEyesFlyHomeAndTheGhostComesOutAgain() {
        game.putMuncher(1, 25, Direction.RIGHT);
        game.putGhost(Kind.RED, 25, 28, Direction.LEFT, Ghost.Mode.FRIGHTENED);
        game.ghost(Kind.RED).eaten();
        List<Ghost.Mode> modes = new ArrayList<>();
        for (int i = 0; i < 12 * 120 && game.ghost(Kind.RED).mode() != Ghost.Mode.SCATTER; i++) {
            steps(1, null);
            Ghost.Mode mode = game.ghost(Kind.RED).mode();
            if (modes.isEmpty() || modes.getLast() != mode) modes.add(mode);
        }
        assertEquals(List.of(Ghost.Mode.EATEN, Ghost.Mode.ENTERING, Ghost.Mode.LEAVING, Ghost.Mode.SCATTER), modes);
        assertEquals(new Cell(13, 11), game.ghost(Kind.RED).walker().cell(), "out of the door");
    }
    // --8<-- [end:test-energizer]

    // --8<-- [start:test-caught]
    @Test
    void aHuntingGhostCatchesTheMuncherAndTheRoundStartsAgain() {
        game.putMuncher(4, 28, Direction.RIGHT);
        game.putGhost(Kind.RED, 7, 28, Direction.LEFT, Ghost.Mode.CHASE);
        seconds(0.5, null);
        assertEquals(1, events(Event.Caught.class).size());
        assertEquals(Game.Phase.DYING, game.phase());
        seconds(Field.DYING_SECONDS + 0.1, null);
        assertEquals(Game.Phase.READY, game.phase());
        assertEquals(Field.START_LIVES - 1, game.lives());
        assertEquals(new Cell(13, 22), game.muncher().walker().cell(), "back at the start");
        assertEquals(Ghost.Mode.HOUSE, game.ghost(Kind.PINK).mode());
    }
    // --8<-- [end:test-caught]

    // --8<-- [start:test-schedule]
    @Test
    void theGhostsLeaveTheHouseOneAfterTheOtherAndScatterThenHunt() {
        seconds(0.5, null);
        assertEquals(Ghost.Mode.LEAVING, game.ghost(Kind.PINK).mode(), "pink at once");
        assertEquals(Ghost.Mode.HOUSE, game.ghost(Kind.CYAN).mode());
        seconds(5, null);
        assertEquals(Ghost.Mode.LEAVING, game.ghost(Kind.CYAN).mode(), "cyan after five seconds");
        assertEquals(Ghost.Mode.SCATTER, game.ghost(Kind.RED).mode());
        game.putMuncher(13, 22, null);            // out of harm's way, for the test
        for (int i = 0; i < 2 * 120 && game.ghost(Kind.RED).mode() == Ghost.Mode.SCATTER; i++) {
            Direction before = game.ghost(Kind.RED).walker().heading();
            steps(1, null);
            if (game.ghost(Kind.RED).mode() == Ghost.Mode.CHASE) {
                assertEquals(before.opposite(), game.ghost(Kind.RED).walker().heading(), "the turn shows the change");
            }
        }
        assertEquals(Ghost.Mode.CHASE, game.ghost(Kind.RED).mode(), "after seven seconds the hunt begins");
    }
    // --8<-- [end:test-schedule]

    @Test
    void theLastDotClearsTheLevelAndTheNextIsFasterWithAllDots() {
        game.leaveOnly(12, 22);
        seconds(0.3, null);
        assertEquals(List.of(new Event.LevelCleared(1)), events(Event.LevelCleared.class));
        assertEquals(Game.Phase.CLEARED, game.phase());
        seconds(Field.CLEARED_SECONDS + 0.1, null);
        assertEquals(2, game.level());
        assertEquals(240, game.dotsLeft());
        assertTrue(Field.pace(2) > Field.pace(1));
    }

    @Test
    void theFruitComesAfterSeventyDots() {
        for (int i = 0; i < 60 * 120 && events(Event.FruitShown.class).isEmpty() && game.phase() != Game.Phase.OVER; i++) {
            game.update(STEP, null);
            events.addAll(game.events());
            if (game.phase() == Game.Phase.DYING || game.phase() == Game.Phase.READY) game.skipReady();
        }
        Pilot pilot = new Pilot(1);
        Game played = new Game(3);
        List<Event> seen = new ArrayList<>();
        for (int i = 0; i < 120 * 120 && seen.stream().noneMatch(Event.FruitShown.class::isInstance); i++) {
            played.update(STEP, pilot.steer(played, STEP));
            seen.addAll(played.events());
        }
        assertInstanceOf(Event.FruitShown.class, seen.getLast());
        assertEquals(Fruit.CHERRY, played.fruit());
        assertEquals(240 - 70, played.dotsLeft());
    }

    @Test
    void theGameEndsWithTheLastLife() {
        for (int life = 0; life < Field.START_LIVES; life++) {
            game.skipReady();
            game.putMuncher(4, 28, Direction.RIGHT);
            game.putGhost(Kind.RED, 7, 28, Direction.LEFT, Ghost.Mode.CHASE);
            seconds(Field.DYING_SECONDS + 0.5, null);
        }
        assertEquals(Game.Phase.OVER, game.phase());
        assertEquals(List.of(new Event.GameOver(new GameResult(game.score(), 1))), events(Event.GameOver.class));
    }

    // --8<-- [start:test-determinism]
    @Test
    void theSameSeedPlaysTheSameGame() {
        Game a = new Game(7);
        Game b = new Game(7);
        Pilot pa = new Pilot(7);
        Pilot pb = new Pilot(7);
        for (int i = 0; i < 90 * 120; i++) {
            a.update(STEP, pa.steer(a, STEP));
            b.update(STEP, pb.steer(b, STEP));
        }
        assertEquals(a.score(), b.score());
        assertEquals(a.muncher().walker().x(), b.muncher().walker().x());
        assertEquals(a.ghost(Kind.CYAN).walker().y(), b.ghost(Kind.CYAN).walker().y());
    }
    // --8<-- [end:test-determinism]
}
