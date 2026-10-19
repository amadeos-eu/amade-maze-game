package eu.amadeos.games.maze.ui.game;

import eu.amadeos.gameengine.audio.Sound;
import eu.amadeos.gameengine.audio.Sound.Wave;

/** The sound effects of Maze, made once at start from tones: no recordings. */
final class SoundBank {
    /** Two short sweeps, up and down, by turns with every dot: the munching. */
    final Sound munchUp = Sound.tone(Wave.TRIANGLE, 260, 520, 0.07, 0.22);
    final Sound munchDown = Sound.tone(Wave.TRIANGLE, 520, 260, 0.07, 0.22);
    final Sound energizer = Sound.tone(Wave.SQUARE, 220, 110, 0.25, 0.12);
    /** A ghost is eaten: a fast sweep up. */
    final Sound ghostEaten = Sound.tone(Wave.SQUARE, 200, 1600, 0.3, 0.1);
    final Sound fruitEaten = Sound.tone(Wave.SQUARE, 880, 1320, 0.08, 0.1)
            .then(Sound.tone(Wave.SQUARE, 1320, 1760, 0.12, 0.1));
    /** Caught: falling wails, each lower than the last. */
    final Sound caught = wail(900).then(wail(760)).then(wail(640)).then(wail(520)).then(wail(420))
            .then(Sound.tone(Wave.TRIANGLE, 300, 60, 0.3, 0.25));
    final Sound extraLife = ping().then(silence(0.05)).then(ping()).then(silence(0.05)).then(ping());

    private static Sound wail(double hz) {
        return Sound.tone(Wave.SQUARE, hz, hz * 0.6, 0.16, 0.08);
    }

    private static Sound ping() {
        return Sound.tone(Wave.SQUARE, 1760, 1760, 0.06, 0.12);
    }

    private static Sound silence(double seconds) {
        return Sound.of(new short[(int) Math.round(seconds * Sound.RATE)]);
    }
}
