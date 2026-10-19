package eu.amadeos.games.maze.ui.game;

import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.games.maze.domain.Field;
import eu.amadeos.games.maze.domain.Fruit;
import eu.amadeos.games.maze.domain.Game;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;

/**
 * The overlay: score and level beside the maze, the lives and the fruits below them, the points of eaten
 * ghosts and fruits, and the texts of the title, pause and end screens. The canvas draws it after the
 * world, at the full size of the screen, so the letters are sharp in every render mode.
 */
final class Hud {
    static final Color CHALK = Color.WHITE;
    static final Color GOLD = new Color(0xFFE030);
    static final Color CYAN = new Color(0x30E8F0);
    static final Color RED = new Color(0xFF4040);
    /** A veil over the field behind texts. */
    static final Color VEIL = new Color(0, 0, 0, 160);

    private final Messages msg;
    private final MazePainter painter;
    private final Font label = new Font(Font.SANS_SERIF, Font.BOLD, 1).deriveFont(12f);
    private final Font score = new Font(Font.SANS_SERIF, Font.BOLD, 1).deriveFont(20f);
    private final Font points = new Font(Font.SANS_SERIF, Font.BOLD, 1).deriveFont(9f);
    private final Map<Integer, Font> fonts = new HashMap<>();
    private final Rectangle2D.Double veil = new Rectangle2D.Double(0, 0, Field.WIDTH, Field.HEIGHT);

    Hud(Messages msg, MazePainter painter) {
        this.msg = msg;
        this.painter = painter;
    }

    // --8<-- [start:hud]
    /** Score and level left and right of the maze, the lives and the level's fruit below them, the points. */
    void status(Graphics2D g, Game game, Effects effects) {
        double margin = painter.left;
        g.setFont(label);
        g.setColor(GOLD);
        centred(g, msg.get("hud.score"), margin / 2, 40);
        centred(g, msg.get("hud.level"), Field.WIDTH - margin / 2, 40);
        g.setFont(score);
        g.setColor(CHALK);
        centred(g, msg.format("hud.points", game.score()), margin / 2, 64);
        centred(g, msg.format("hud.points", game.level()), Field.WIDTH - margin / 2, 64);
        int waiting = game.lives() - (game.phase() == Game.Phase.OVER ? 0 : 1);
        for (int i = 0; i < waiting; i++) painter.life().draw(g, margin / 2 - 33 + i * 22, Field.HEIGHT - 40);
        for (int level = Math.max(1, game.level() - 3); level <= game.level(); level++) {
            painter.fruit(Fruit.of(level)).draw(g, Field.WIDTH - margin / 2 + 24 - (game.level() - level) * 22,
                    Field.HEIGHT - 40);
        }
        g.setFont(points);
        g.setColor(CYAN);
        for (int i = 0; i < Effects.LABELS; i++) {
            double age = effects.time() - effects.labelTime[i];
            if (age < 0 || age > Effects.LABEL_SECONDS) continue;
            centred(g, Integer.toString(effects.labelPoints[i]), painter.x(effects.labelX[i]),
                    painter.y(effects.labelY[i]) + 4);
        }
        if (game.phase() == Game.Phase.READY) {
            g.setFont(label);
            g.setColor(GOLD);
            centred(g, msg.get("play.ready"), Field.WIDTH / 2, painter.y(game.maze().fruit().row()) + 5);
        }
    }
    // --8<-- [end:hud]

    private static void centred(Graphics2D g, String text, double x, double y) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, (float) (x - fm.stringWidth(text) / 2.0), (float) y);
    }

    /** Darkens the field so text over it stays readable. */
    void veil(Graphics2D g) {
        Object smoothing = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setColor(VEIL);
        g.fill(veil);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, smoothing);
    }

    /** A line of text centred horizontally, its baseline at {@code y}. */
    void text(Graphics2D g, String line, double y, int size, boolean bold, Color colour) {
        g.setFont(fonts.computeIfAbsent(2 * size + (bold ? 1 : 0),
                key -> new Font(Font.SANS_SERIF, bold ? Font.BOLD : Font.PLAIN, size)));
        g.setColor(colour);
        g.drawString(line, (float) (Field.WIDTH - g.getFontMetrics().stringWidth(line)) / 2, (float) y);
    }
}
