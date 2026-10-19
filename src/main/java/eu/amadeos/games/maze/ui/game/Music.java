package eu.amadeos.games.maze.ui.game;

import eu.amadeos.gameengine.audio.Sound;
import eu.amadeos.gameengine.audio.Sound.Wave;
import eu.amadeos.gameengine.audio.Tune;

/**
 * The music of Maze, written in MML and made into sounds once at start: a little march before the first
 * round, the wail of blue ghosts that loops while they are blue, a tune for a cleared maze and one for
 * the end. The mixer plays them on its music voice.
 */
final class Music {
    /** A game begins: four bars over a hopping bass, about four seconds. */
    final Sound start = Sound.mix(
            Tune.voice(Wave.SQUARE, 0.1, "t150 o5 l16 c8 g8 e8 g8 c8 g e g4 | d8 a8 f8 a8 d8 a f a4"
                    + "| e8 >c8< g8 >c8< e8 >c< g >c4< | d e f g a b >c d c4 r4"),
            Tune.voice(Wave.TRIANGLE, 0.24, "t150 o3 l8 c g c g c g c g | d a d a d a d a"
                    + "| e >c< e >c< e >c< e >c< | g4 g4 c4 r4"));

    /** Blue ghosts: a quick wail, again and again. */
    final Sound frightened = Sound.tone(Wave.TRIANGLE, 700, 350, 0.14, 0.09);

    /** The maze is cleared. */
    final Sound cleared = Tune.voice(Wave.SQUARE, 0.11, "t180 o5 l16 c e g >c< g >c e8");

    /** The end: down, slowly. */
    final Sound over = Sound.mix(
            Tune.voice(Wave.SQUARE, 0.1, "t90 o5 l8 e d c < b a4 r8"),
            Tune.voice(Wave.TRIANGLE, 0.22, "t90 o3 l4 a e a2"));
}
