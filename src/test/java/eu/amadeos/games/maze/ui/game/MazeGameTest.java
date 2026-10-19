package eu.amadeos.games.maze.ui.game;

import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.audio.Mixer;
import eu.amadeos.gameengine.input.Keyboard;
import eu.amadeos.gameengine.tile.Direction;
import eu.amadeos.games.maze.bootstrap.MazeModule;
import eu.amadeos.games.maze.domain.Field;
import eu.amadeos.games.maze.domain.Game;
import eu.amadeos.games.maze.domain.GameResult;
import eu.amadeos.games.maze.domain.Maze;
import eu.amadeos.games.maze.repository.MazeSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The game without a window and without the loop thread: the test presses keys and calls the steps
 * itself, so everything happens in order on the test thread.
 */
class MazeGameTest {
    private static final double STEP = MazeGame.STEP;

    @TempDir
    Path dir;

    private final long[] now = {0};
    private final Keyboard keyboard = new Keyboard(() -> now[0]);
    private final List<GameResult> results = new ArrayList<>();
    private MazeGame game;

    @BeforeEach
    void create() {
        Messages msg = Messages.load(MazeModule.class, MazeModule.MESSAGES, Locale.ENGLISH);
        game = new MazeGame(keyboard, msg, new MazeSettings(dir.resolve("maze.properties")), () -> 77L, new Mixer(),
                Runnable::run);
        game.onGameOver(results::add);
    }

    private void tap(int key) {
        keyboard.pressed(key, now[0]);
        keyboard.released(key, now[0] + 1_000_000);
        steps(1);
    }

    private void steps(int n) {
        for (int i = 0; i < n; i++) {
            now[0] += (long) (STEP * 1e9);
            game.update(STEP);
        }
    }

    private void seconds(double seconds) {
        steps((int) Math.round(seconds * MazeGame.UPDATES_PER_SECOND));
    }

    private MazeGame.PlayScene play() {
        tap(KeyEvent.VK_ENTER);
        return assertInstanceOf(MazeGame.PlayScene.class, game.top());
    }

    // --8<-- [start:test-keys]
    @Test
    void aKeyIsKeptUntilTheMuncherCanTurn() {
        MazeGame.PlayScene play = play();
        seconds(Field.FIRST_READY_SECONDS + 0.05);
        assertEquals(Game.Phase.PLAYING, play.game().phase());
        tap(KeyEvent.VK_UP);          // up is a wall at the start: the key waits
        assertEquals(Direction.UP, play.wanted());
        assertEquals(Direction.LEFT, play.game().muncher().walker().heading());
        seconds(0.3);
        assertEquals(Direction.UP, play.game().muncher().walker().heading(), "up at the next crossing");
        assertEquals(12, play.game().muncher().walker().col());
    }
    // --8<-- [end:test-keys]

    @Test
    void pFreezesTheGameAndEnterContinuesIt() {
        MazeGame.PlayScene play = play();
        seconds(Field.FIRST_READY_SECONDS + 0.5);
        tap(KeyEvent.VK_P);
        assertEquals(2, game.sceneCount());
        double x = play.game().muncher().walker().x();
        seconds(1);
        assertEquals(x, play.game().muncher().walker().x(), "frozen");
        tap(KeyEvent.VK_ENTER);
        seconds(0.2);
        assertTrue(play.game().muncher().walker().x() < x, "goes on");
    }

    @Test
    void aMuncherThatDoesNothingIsCaughtAndTheResultReachesThePresenter() {
        MazeGame.PlayScene play = play();
        for (int i = 0; i < 10 * 60 * 120 && results.isEmpty(); i++) steps(1);
        assertEquals(1, results.size());
        assertEquals(Game.Phase.OVER, play.game().phase());
        assertInstanceOf(MazeGame.OverScene.class, game.top());
        seconds(2);
        tap(KeyEvent.VK_ENTER);
        assertInstanceOf(MazeGame.TitleScene.class, game.top());
    }

    @Test
    void theDemoEatsDots() {
        seconds(10);
        MazeGame.TitleScene title = assertInstanceOf(MazeGame.TitleScene.class, game.top());
        assertTrue(title.demo().score() > 300, "score " + title.demo().score());
    }

    @Test
    void theWallsCoverTheWallTilesAndLeaveTheCorridorsFree() {
        Shape walls = MazePainter.wallShape(Maze.STANDARD);
        assertTrue(walls.contains(0.5, 0.5), "the corner of the outer wall");
        assertTrue(walls.contains(3.5, 3.5), "inside a block");
        assertFalse(walls.contains(1.5, 1.5), "a corridor");
        assertFalse(walls.contains(0.1, 1.5), "the outer wall keeps away from the edge");
    }

    @Test
    void everySceneDrawsWorldAndOverlay() {
        BufferedImage image = new BufferedImage((int) Field.WIDTH, (int) Field.HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        seconds(2);
        game.render(g, 0.5);
        game.renderOverlay(g, 0.5);
        MazeGame.PlayScene play = play();
        game.render(g, 0.5);
        MazePainter painter = game.painter();
        // The muncher at its start, mouth to the left: yellow right of its centre.
        int x = (int) Math.round(painter.x(13)) + 4;
        int y = (int) Math.round(painter.y(22));
        assertEquals(MazePainter.YELLOW.getRGB() & 0xFFFFFF, image.getRGB(x, y) & 0xFFFFFF, "the muncher");
        game.renderOverlay(g, 0.5);
        seconds(Field.FIRST_READY_SECONDS + 3);
        game.render(g, 0.5);
        tap(KeyEvent.VK_P);
        game.render(g, 0.5);
        game.renderOverlay(g, 0.5);
        g.dispose();
        assertTrue(play.game().score() > 0);
    }
}
