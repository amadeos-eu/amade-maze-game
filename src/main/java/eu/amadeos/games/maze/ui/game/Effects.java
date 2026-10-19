package eu.amadeos.games.maze.ui.game;

import eu.amadeos.gameengine.audio.Mixer;
import eu.amadeos.games.maze.domain.Event;

import java.util.Arrays;
import java.util.List;

/**
 * What the events of a step look and sound like: the points of an eaten ghost or fruit where it was, the
 * sounds and the music. The rules know nothing of this.
 */
final class Effects {
    /** Most point labels at once; a new one replaces the oldest. */
    static final int LABELS = 8;
    /** How long a label stays. */
    static final double LABEL_SECONDS = 1.2;

    private final SoundBank sounds = new SoundBank();
    private final Music music = new Music();
    private final Mixer mixer;
    /** Seconds since the effects started: the clock of blinking, waving skirts and labels. */
    private double time;
    private boolean munchUp;
    // Point labels, in a ring: where in tiles, since when, how many points.
    final double[] labelX = new double[LABELS];
    final double[] labelY = new double[LABELS];
    final double[] labelTime = new double[LABELS];
    final int[] labelPoints = new int[LABELS];
    private int nextLabel;

    Effects(Mixer mixer) {
        this.mixer = mixer;
        Arrays.fill(labelTime, Double.NEGATIVE_INFINITY);
    }

    double time() {
        return time;
    }

    /** A new game or the title: no labels and no music from before. */
    void clear() {
        Arrays.fill(labelTime, Double.NEGATIVE_INFINITY);
        mixer.stopMusic();
    }

    void update(double seconds) {
        time += seconds;
    }

    // --8<-- [start:react]
    /** Shows and, if {@code audible}, plays what happened in the last step; the music too. */
    void react(List<Event> events, boolean audible) {
        for (Event event : events) {
            switch (event) {
                case Event.Ready(int level, boolean newGame) -> {
                    if (audible && newGame) mixer.playMusic(music.start, false);
                }
                case Event.DotEaten dot -> {
                    munchUp = !munchUp;
                    if (audible) mixer.play(munchUp ? sounds.munchUp : sounds.munchDown);
                }
                case Event.EnergizerEaten energizer -> {
                    if (audible) {
                        mixer.play(sounds.energizer);
                        mixer.playMusic(music.frightened, true);
                    }
                }
                case Event.FrightOver over -> mixer.stopMusic();
                case Event.GhostEaten(var kind, double x, double y, int points) -> {
                    label(x, y, points);
                    if (audible) mixer.play(sounds.ghostEaten);
                }
                case Event.FruitShown shown -> {
                }
                case Event.FruitEaten(var fruit, var at, int points) -> {
                    label(at.col(), at.row(), points);
                    if (audible) mixer.play(sounds.fruitEaten);
                }
                case Event.Caught caught -> {
                    mixer.stopMusic();
                    if (audible) mixer.play(sounds.caught);
                }
                case Event.LevelCleared cleared -> {
                    mixer.stopMusic();
                    if (audible) mixer.playMusic(music.cleared, false);
                }
                case Event.ExtraLife life -> {
                    if (audible) mixer.play(sounds.extraLife);
                }
                case Event.GameOver over -> {
                    if (audible) mixer.playMusic(music.over, false);
                }
            }
        }
    }
    // --8<-- [end:react]

    private void label(double x, double y, int points) {
        labelX[nextLabel] = x;
        labelY[nextLabel] = y;
        labelTime[nextLabel] = time;
        labelPoints[nextLabel] = points;
        nextLabel = (nextLabel + 1) % LABELS;
    }
}
