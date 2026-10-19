package eu.amadeos.games.maze.ui.game;

import com.google.inject.Inject;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.commons.swing.EdtExecutor;
import eu.amadeos.gameengine.audio.Mixer;
import eu.amadeos.gameengine.input.Controls;
import eu.amadeos.gameengine.input.Keyboard;
import eu.amadeos.gameengine.loop.GameLoop;
import eu.amadeos.gameengine.random.SeededRandom;
import eu.amadeos.gameengine.scene.Scene;
import eu.amadeos.gameengine.scene.SceneStack;
import eu.amadeos.gameengine.swing.GameCanvas;
import eu.amadeos.gameengine.tile.Direction;
import eu.amadeos.games.maze.domain.Event;
import eu.amadeos.games.maze.domain.Game;
import eu.amadeos.games.maze.domain.GameResult;
import eu.amadeos.games.maze.domain.Maze;
import eu.amadeos.games.maze.domain.Pilot;
import eu.amadeos.games.maze.repository.MazeSettings;

import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/**
 * The running game: the loop, the controls, the effects and the scenes (title with the computer playing, a
 * game, the pause screen, the end of a game). Everything here runs on the game loop thread, except the
 * {@link Arena} methods, which only post commands to it.
 */
public final class MazeGame implements Arena {
    /** The actions of the game; which keys trigger them comes from the settings. */
    public enum Action { UP, LEFT, DOWN, RIGHT, PAUSE, START }

    static final int UPDATES_PER_SECOND = 120;
    static final int FRAMES_PER_SECOND = 60;
    static final double STEP = 1.0 / UPDATES_PER_SECOND;

    private final Keyboard keyboard;
    private final Controls<Action> controls;
    private final Messages msg;
    private final LongSupplier seeds;
    private final Executor ui;
    private final Mixer mixer;
    private final MazePainter painter = new MazePainter(Maze.STANDARD);
    private final Hud hud;
    private final Effects effects;
    private final SceneStack scenes = new SceneStack();
    private volatile Consumer<GameResult> onGameOver = result -> { };
    private GameLoop loop;

    @Inject
    public MazeGame(Keyboard keyboard, Messages msg, MazeSettings settings, Mixer mixer, EdtExecutor ui) {
        this(keyboard, msg, settings, SeededRandom::freshSeed, mixer, (Executor) ui);
    }

    /** @param seeds the seed of each new game */
    MazeGame(Keyboard keyboard, Messages msg, MazeSettings settings, LongSupplier seeds, Mixer mixer, Executor ui) {
        this.keyboard = keyboard;
        this.msg = msg;
        this.seeds = seeds;
        this.mixer = mixer;
        this.ui = ui;
        this.hud = new Hud(msg, painter);
        this.effects = new Effects(mixer);
        controls = new Controls<>(Action.class, keyboard)
                .bind(Action.UP, settings.keys("up", KeyEvent.VK_UP, KeyEvent.VK_W))
                .bind(Action.LEFT, settings.keys("left", KeyEvent.VK_LEFT, KeyEvent.VK_A))
                .bind(Action.DOWN, settings.keys("down", KeyEvent.VK_DOWN, KeyEvent.VK_S))
                .bind(Action.RIGHT, settings.keys("right", KeyEvent.VK_RIGHT, KeyEvent.VK_D))
                .bind(Action.PAUSE, settings.keys("pause", KeyEvent.VK_P, KeyEvent.VK_ESCAPE))
                .bind(Action.START, KeyEvent.VK_ENTER, KeyEvent.VK_SPACE);
        scenes.push(new TitleScene());
    }

    /** Starts the loop; the canvas draws the scenes' worlds and then their overlays. */
    public void start(GameCanvas canvas) {
        loop = new GameLoop(UPDATES_PER_SECOND, FRAMES_PER_SECOND, this::update, canvas::render);
        loop.start();
    }

    public void stop() {
        if (loop != null) loop.stop();
    }

    void update(double seconds) {
        keyboard.poll();
        scenes.update(seconds);
    }

    /** The world of all scenes, in logical coordinates. */
    public void render(Graphics2D g, double alpha) {
        scenes.render(g, alpha);
    }

    /** Score and texts of all scenes, drawn after the world at the screen's full size. */
    public void renderOverlay(Graphics2D g, double alpha) {
        scenes.renderOverlay(g, alpha);
    }

    // ---- Arena: called on the Swing thread, carried out on the loop thread -----------------------

    @Override
    public void showTitle() {
        run(() -> scenes.replace(new TitleScene()));
    }

    @Override
    public void newGame() {
        run(() -> scenes.replace(new PlayScene()));
    }

    @Override
    public void pause() {
        run(() -> {
            if (scenes.top() instanceof PlayScene) scenes.push(new PauseScene());
        });
    }

    @Override
    public void setSound(boolean on) {
        mixer.setMuted(!on);
    }

    @Override
    public void onGameOver(Consumer<GameResult> listener) {
        onGameOver = listener;
    }

    /** On the loop thread when it runs; directly before the loop starts and in tests. */
    private void run(Runnable command) {
        if (loop != null && loop.running()) loop.post(command);
        else command.run();
    }

    // ---- for tests -------------------------------------------------------------------------------

    Scene top() {
        return scenes.top();
    }

    int sceneCount() {
        return scenes.size();
    }

    Effects effects() {
        return effects;
    }

    MazePainter painter() {
        return painter;
    }

    /** Score, level and lives of any game, for the screenshots. */
    void status(Graphics2D g, Game game) {
        hud.status(g, game, effects);
    }

    // ---- the scenes ------------------------------------------------------------------------------

    /** The computer plays, silently, game after game; Enter or Space starts a game. */
    final class TitleScene implements Scene {
        private Game demo;
        private Pilot pilot;
        private double over;

        TitleScene() {
            effects.clear();
            startDemo();
        }

        private void startDemo() {
            demo = new Game(seeds.getAsLong());
            pilot = new Pilot(demo.seed());
            over = 0;
        }

        @Override
        public void update(double seconds) {
            if (controls.wasPressed(Action.START)) {
                scenes.replace(new PlayScene());
                return;
            }
            demo.update(seconds, pilot.steer(demo, seconds));
            effects.react(demo.events(), false);
            effects.update(seconds);
            if (demo.phase() == Game.Phase.OVER) {
                over += seconds;
                if (over > 3) startDemo();
            }
        }

        @Override
        public void render(Graphics2D g, double alpha) {
            painter.paint(g, demo, effects, alpha);
        }

        @Override
        public void renderOverlay(Graphics2D g, double alpha) {
            hud.veil(g);
            hud.text(g, msg.get("window.title"), 150, 72, true, Hud.GOLD);
            hud.text(g, msg.get("title.start"), 210, 18, false, Hud.CHALK);
            hud.text(g, msg.get("title.keys"), 268, 13, false, Hud.CHALK);
            hud.text(g, msg.get("title.ghosts"), 290, 13, false, Hud.CHALK);
            hud.text(g, msg.get("title.screen"), 312, 13, false, Hud.CHALK);
        }

        Game demo() {
            return demo;
        }

        Pilot pilot() {
            return pilot;
        }
    }

    // --8<-- [start:play]
    /**
     * A game. The last arrow key pressed is the way the player wants to go; the muncher takes it at the
     * next crossing where it is open, so a key may come a little early.
     */
    final class PlayScene implements Scene {
        private final Game game = new Game(seeds.getAsLong());
        private Direction wanted;

        PlayScene() {
            effects.clear();
            effects.react(game.events(), true);    // "Ready!" and the tune
        }

        @Override
        public void update(double seconds) {
            if (controls.wasPressed(Action.PAUSE)) {
                scenes.push(new PauseScene());
                return;
            }
            // The first four actions are the directions, in the same order.
            for (Direction d : Direction.values()) {
                if (controls.wasPressed(Action.values()[d.ordinal()])) wanted = d;
            }
            game.update(seconds, wanted);
            effects.react(game.events(), true);
            effects.update(seconds);
            for (Event event : game.events()) {
                if (event instanceof Event.Ready) wanted = null;      // a new round: forget the old key
                if (event instanceof Event.GameOver(GameResult result)) {
                    scenes.replace(new OverScene(game, result));
                    // The presenter lives on the Swing thread: hand the result over there.
                    Consumer<GameResult> listener = onGameOver;
                    ui.execute(() -> listener.accept(result));
                }
            }
        }
        // --8<-- [end:play]

        @Override
        public void render(Graphics2D g, double alpha) {
            painter.paint(g, game, effects, alpha);
        }

        @Override
        public void renderOverlay(Graphics2D g, double alpha) {
            hud.status(g, game, effects);
        }

        Game game() {
            return game;
        }

        Direction wanted() {
            return wanted;
        }
    }

    /** Drawn over the frozen game until P, Esc, Enter or Space. */
    final class PauseScene implements Scene {
        @Override
        public void update(double seconds) {
            if (controls.wasPressed(Action.PAUSE) || controls.wasPressed(Action.START)) scenes.pop();
        }

        @Override
        public void render(Graphics2D g, double alpha) {
        }

        @Override
        public void renderOverlay(Graphics2D g, double alpha) {
            hud.veil(g);
            hud.text(g, msg.get("pause.title"), 200, 40, true, Hud.CHALK);
            hud.text(g, msg.get("pause.continue"), 245, 18, false, Hud.CHALK);
        }
    }

    /** The end: the maze stays, the score too, Enter or Space leads back to the title. */
    final class OverScene implements Scene {
        private final Game game;
        private final GameResult result;
        private double shown;

        OverScene(Game game, GameResult result) {
            this.game = game;
            this.result = result;
        }

        @Override
        public void update(double seconds) {
            effects.update(seconds);
            shown += seconds;
            // A short wait, so a key meant for the muncher does not skip the end.
            if (shown > 1.5 && controls.wasPressed(Action.START)) scenes.replace(new TitleScene());
        }

        @Override
        public void render(Graphics2D g, double alpha) {
            painter.paint(g, game, effects, alpha);
        }

        @Override
        public void renderOverlay(Graphics2D g, double alpha) {
            hud.status(g, game, effects);
            hud.veil(g);
            hud.text(g, msg.get("over.lost"), 190, 40, true, Hud.RED);
            hud.text(g, msg.format("over.score", result.score(), result.level()), 232, 20, false, Hud.CHALK);
            hud.text(g, msg.get("over.continue"), 270, 16, false, Hud.CHALK);
        }
    }
}
