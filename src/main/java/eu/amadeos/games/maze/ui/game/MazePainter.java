package eu.amadeos.games.maze.ui.game;

import eu.amadeos.gameengine.sprite.Sprite;
import eu.amadeos.gameengine.tile.Direction;
import eu.amadeos.gameengine.tile.TileMap;
import eu.amadeos.gameengine.tile.Walker;
import eu.amadeos.games.maze.domain.Field;
import eu.amadeos.games.maze.domain.Fruit;
import eu.amadeos.games.maze.domain.Game;
import eu.amadeos.games.maze.domain.Ghost;
import eu.amadeos.games.maze.domain.Kind;
import eu.amadeos.games.maze.domain.Maze;
import eu.amadeos.games.maze.domain.Tile;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

/**
 * Draws the world in logical coordinates (640 × 400): the maze in the middle, 12 units per tile, its dots,
 * the fruit, the ghosts and the muncher. The walls are one picture, worked out once from the tiles and
 * cached at the screen's size; the muncher and the ghosts are pictures drawn in code, a few per direction
 * and movement.
 */
final class MazePainter {
    /** The size of a tile in logical units. */
    static final double TILE = 12;
    /** Muncher and ghosts are larger than a tile, as in the arcade: they fill the corridor. */
    static final double ACTOR = 18;
    /** How far the walls stay from the edges of their tiles, in tiles: the corridors look wider. */
    static final double INSET = 0.28;
    static final Color WALL_FILL = new Color(0x0A1440);
    static final Color WALL_EDGE = new Color(0x3C64FF);
    static final Color FLASH_EDGE = new Color(0xF0F4FF);
    static final Color DOOR = new Color(0xFFB0D8);
    static final Color DOT = new Color(0xFFD8B8);
    static final Color YELLOW = new Color(0xFFE030);
    static final Color[] GHOSTS = {new Color(0xFF2828), new Color(0xFFA8DC), new Color(0x30E8F0), new Color(0xFFB040)};
    static final Color BLUE = new Color(0x2840FF);
    static final Color PUPIL = new Color(0x2030C0);
    /** The mouth's opening, as a part of the full circle, while the muncher walks: closed, half, open, half. */
    static final double[] MOUTH = {0.02, 0.12, 0.22, 0.12};

    private final Maze maze;
    /** The top left corner of the maze. */
    final double left;
    final double top;
    private final Sprite walls;
    private final Sprite flash;
    private final Sprite[][] muncher = new Sprite[4][MOUTH.length];
    private final Sprite[][] bodies = new Sprite[GHOSTS.length][2];
    private final Sprite[] blue = new Sprite[2];
    private final Sprite[] white = new Sprite[2];
    private final Sprite[] eyes = new Sprite[4];
    private final Sprite[] fruits = new Sprite[Fruit.values().length];
    private final Rectangle2D.Double rect = new Rectangle2D.Double();
    private final Ellipse2D.Double circle = new Ellipse2D.Double();
    private final Arc2D.Double arc = new Arc2D.Double();
    private final Rectangle2D.Double field;
    private final Rectangle2D.Double screen = new Rectangle2D.Double(0, 0, Field.WIDTH, Field.HEIGHT);

    MazePainter(Maze maze) {
        this.maze = maze;
        double width = maze.width() * TILE;
        double height = maze.height() * TILE;
        left = Math.round((Field.WIDTH - width) / 2);
        top = Math.round((Field.HEIGHT - height) / 2);
        field = new Rectangle2D.Double(left, top, width, height);
        Shape shape = wallShape(maze);
        walls = Sprite.drawn(width, height, (g, w, h) -> drawWalls(g, shape, WALL_EDGE));
        flash = Sprite.drawn(width, height, (g, w, h) -> drawWalls(g, shape, FLASH_EDGE));
        for (Direction d : Direction.values()) {
            for (int i = 0; i < MOUTH.length; i++) {
                double opening = MOUTH[i];
                muncher[d.ordinal()][i] = Sprite.drawn(ACTOR, ACTOR, (g, w, h) -> drawMuncher(g, w, d.angle(), opening));
            }
            eyes[d.ordinal()] = Sprite.drawn(ACTOR, ACTOR, (g, w, h) -> drawEyes(g, w, d));
        }
        for (int skirt = 0; skirt < 2; skirt++) {
            int s = skirt;
            for (Kind kind : Kind.values()) {
                Color colour = GHOSTS[kind.ordinal()];
                bodies[kind.ordinal()][s] = Sprite.drawn(ACTOR, ACTOR, (g, w, h) -> drawGhost(g, w, colour, s));
            }
            blue[s] = Sprite.drawn(ACTOR, ACTOR, (g, w, h) -> drawScared(g, w, BLUE, DOT, s));
            white[s] = Sprite.drawn(ACTOR, ACTOR, (g, w, h) -> drawScared(g, w, Color.WHITE, Color.RED, s));
        }
        for (Fruit fruit : Fruit.values()) {
            fruits[fruit.ordinal()] = Sprite.drawn(ACTOR, ACTOR, (g, w, h) -> FruitPictures.draw(g, w, fruit));
        }
    }

    // --8<-- [start:walls]
    /**
     * The walls as one shape, in tiles. Each wall tile is a square smaller than the tile, with an arm
     * towards each neighbour that is a wall too, as {@link TileMap#mask} tells; where four walls meet in a
     * block, the gap between their arms is filled. All pieces together, made into one {@link Area}, give
     * the outline of every wall, with the corridors wider than the walls.
     */
    static Shape wallShape(Maze maze) {
        TileMap<Tile> tiles = maze.tiles();
        Path2D.Double pieces = new Path2D.Double();
        double a = INSET;
        double b = 1 - INSET;
        double e = 0.01;             // pieces overlap a little: shapes that only touch may keep a seam
        for (int row = 0; row < tiles.height(); row++) {
            for (int col = 0; col < tiles.width(); col++) {
                if (tiles.get(col, row) != Tile.WALL) continue;
                int mask = tiles.mask(col, row, tile -> tile == Tile.WALL, false);
                pieces.append(new Rectangle2D.Double(col + a, row + a, b - a, b - a), false);
                if ((mask & 1) != 0) pieces.append(new Rectangle2D.Double(col + a, row - e, b - a, a + 2 * e), false);
                if ((mask & 2) != 0) pieces.append(new Rectangle2D.Double(col - e, row + a, a + 2 * e, b - a), false);
                if ((mask & 4) != 0) pieces.append(new Rectangle2D.Double(col + a, row + b - e, b - a, a + 2 * e), false);
                if ((mask & 8) != 0) pieces.append(new Rectangle2D.Double(col + b - e, row + a, a + 2 * e, b - a), false);
                // Right, down and the tile diagonally between them are walls: a block, no gap at the corner.
                boolean block = (mask & 8) != 0 && (mask & 4) != 0 && tiles.get(col + 1, row + 1) == Tile.WALL;
                if (block) pieces.append(new Rectangle2D.Double(col + b - e, row + b - e, 2 * a + 2 * e, 2 * a + 2 * e), false);
            }
        }
        return new Area(pieces);
    }
    // --8<-- [end:walls]

    private void drawWalls(Graphics2D g, Shape shape, Color edge) {
        g.scale(TILE, TILE);
        g.setColor(WALL_FILL);
        g.fill(shape);
        g.setColor(edge);
        g.setStroke(new BasicStroke((float) (1.2 / TILE), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(shape);
        g.setColor(DOOR);
        g.fill(new Rectangle2D.Double(maze.door().col() - 0.1, maze.door().row() + 0.38, 1.2, 0.24));
    }

    // --8<-- [start:paint]
    /** @param alpha how far the time is between the last two update steps, 0 to 1 */
    void paint(Graphics2D g, Game game, Effects effects, double alpha) {
        Object smoothing = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setColor(Color.BLACK);
        g.fill(screen);
        boolean flashing = game.phase() == Game.Phase.CLEARED && (int) (game.phaseTime() * 4) % 2 == 1;
        (flashing ? flash : walls).draw(g, left, top);
        dots(g, game, effects);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, smoothing);
        Shape clip = g.getClip();
        g.clip(field);               // the tunnel's ends: whoever goes in disappears at the edge
        if (game.fruit() != null) {
            fruits[game.fruit().ordinal()].draw(g, x(game.maze().fruit().col()) - ACTOR / 2,
                    y(game.maze().fruit().row()) - ACTOR / 2);
        }
        boolean ghostsShown = switch (game.phase()) {
            case READY, PLAYING -> true;
            case DYING -> game.phaseTime() < 0.5;
            case CLEARED, OVER -> false;
        };
        if (ghostsShown) {
            for (Ghost ghost : game.ghosts()) drawGhost(g, game, ghost, effects, alpha);
        }
        drawMuncher(g, game, alpha);
        g.setClip(clip);
    }
    // --8<-- [end:paint]

    /** Dots are small squares without antialiasing; energizers blink while the game runs. */
    private void dots(Graphics2D g, Game game, Effects effects) {
        TileMap<Tile> tiles = game.tiles();
        boolean blinkOff = game.phase() == Game.Phase.PLAYING && (int) (effects.time() * 5) % 2 == 1;
        g.setColor(DOT);
        for (int row = 0; row < tiles.height(); row++) {
            for (int col = 0; col < tiles.width(); col++) {
                Tile tile = tiles.get(col, row);
                if (tile == Tile.DOT) {
                    rect.setRect(x(col) - 1, y(row) - 1, 2, 2);
                    g.fill(rect);
                } else if (tile == Tile.ENERGIZER && !blinkOff) {
                    circle.setFrame(x(col) - 4.5, y(row) - 4.5, 9, 9);
                    g.fill(circle);
                }
            }
        }
    }

    // --8<-- [start:ghost]
    /** A ghost's body by its mode, its skirt waving, and its eyes looking where it goes. */
    private void drawGhost(Graphics2D g, Game game, Ghost ghost, Effects effects, double alpha) {
        Walker walker = ghost.walker();
        double x = x(between(walker.previousX(), walker.x(), alpha)) - ACTOR / 2;
        double y = y(between(walker.previousY(), walker.y(), alpha)) - ACTOR / 2;
        int skirt = (int) (effects.time() * 8) % 2;
        if (ghost.mode() == Ghost.Mode.FRIGHTENED) {
            boolean flashing = game.frightLeft() < Field.FLASH_SECONDS && (int) (effects.time() * 6) % 2 == 0;
            (flashing ? white : blue)[skirt].draw(g, x, y);
            return;
        }
        if (!ghost.eyes()) bodies[ghost.kind().ordinal()][skirt].draw(g, x, y);
        Direction looking = walker.heading() != null ? walker.heading() : Direction.UP;
        eyes[looking.ordinal()].draw(g, x, y);
    }
    // --8<-- [end:ghost]

    private void drawMuncher(Graphics2D g, Game game, double alpha) {
        Walker walker = game.muncher().walker();
        double cx = x(between(walker.previousX(), walker.x(), alpha));
        double cy = y(between(walker.previousY(), walker.y(), alpha));
        switch (game.phase()) {
            case DYING -> {
                // It turns to face up, and its mouth opens wider and wider until nothing is left.
                double t = Math.max(0, (game.phaseTime() - 0.5) / (Field.DYING_SECONDS - 0.7));
                if (t >= 1) return;
                double opening = 0.1 + 0.9 * t;
                g.setColor(YELLOW);
                arc.setArcByCenter(cx, cy, ACTOR / 2 - 1, 90 + 180 * opening, 360 * (1 - opening), Arc2D.PIE);
                g.fill(arc);
            }
            case CLEARED, OVER -> muncher[game.muncher().facing().ordinal()][0].draw(g, cx - ACTOR / 2, cy - ACTOR / 2);
            default -> {
                int frame = (int) (game.muncher().walked() * 4) % MOUTH.length;
                muncher[game.muncher().facing().ordinal()][frame].draw(g, cx - ACTOR / 2, cy - ACTOR / 2);
            }
        }
    }

    /** Between the last two positions; through the tunnel, the walker moved its previous position along. */
    private static double between(double previous, double now, double alpha) {
        return previous + (now - previous) * alpha;
    }

    /** The centre of a column, in logical units. */
    double x(double col) {
        return left + (col + 0.5) * TILE;
    }

    double y(double row) {
        return top + (row + 0.5) * TILE;
    }

    /** The muncher's picture, open, looking left: for the lives in the overlay. */
    Sprite life() {
        return muncher[Direction.LEFT.ordinal()][2];
    }

    Sprite fruit(Fruit fruit) {
        return fruits[fruit.ordinal()];
    }

    // ---- the pictures --------------------------------------------------------------------------

    /** A yellow circle with a wedge cut out, the mouth, turned to {@code angle}. */
    static void drawMuncher(Graphics2D g, double size, double angle, double opening) {
        g.setColor(YELLOW);
        double start = -Math.toDegrees(angle) + 180 * opening;
        g.fill(new Arc2D.Double(1, 1, size - 2, size - 2, start, 360 * (1 - opening), Arc2D.PIE));
    }

    /** A dome on top, straight sides, and a wavy skirt with three or four points. */
    static Path2D.Double body(double size, int skirt) {
        double s = size;
        Path2D.Double body = new Path2D.Double();
        body.moveTo(1.5, s * 0.5);
        body.curveTo(1.5, s * 0.1, s - 1.5, s * 0.1, s - 1.5, s * 0.5);
        body.lineTo(s - 1.5, s - 1.5);
        int points = skirt == 0 ? 4 : 3;
        double step = (s - 3) / points;
        for (int i = points; i > 0; i--) {
            double right = 1.5 + i * step;
            body.lineTo(right - step / 2, s - 4.5);
            body.lineTo(right - step, s - 1.5);
        }
        body.closePath();
        return body;
    }

    static void drawGhost(Graphics2D g, double size, Color colour, int skirt) {
        g.setColor(colour);
        g.fill(body(size, skirt));
    }

    /** White eyes with blue pupils, both looking the same way. */
    static void drawEyes(Graphics2D g, double size, Direction looking) {
        double dx = looking.dx * 1.2;
        double dy = looking.dy * 1.4;
        for (double ex : new double[]{size * 0.33, size * 0.67}) {
            g.setColor(Color.WHITE);
            g.fill(new Ellipse2D.Double(ex - 2.6 + dx * 0.6, size * 0.42 - 3.2 + dy * 0.6, 5.2, 6.4));
            g.setColor(PUPIL);
            g.fill(new Ellipse2D.Double(ex - 1.5 + dx * 1.4, size * 0.42 - 1.5 + dy * 1.4, 3, 3));
        }
    }

    /** A scared ghost: two small eyes and a wavy mouth. */
    static void drawScared(Graphics2D g, double size, Color body, Color face, int skirt) {
        g.setColor(body);
        g.fill(body(size, skirt));
        g.setColor(face);
        g.fill(new Rectangle2D.Double(size * 0.33 - 1.2, size * 0.38, 2.4, 2.4));
        g.fill(new Rectangle2D.Double(size * 0.67 - 1.2, size * 0.38, 2.4, 2.4));
        Path2D.Double mouth = new Path2D.Double();
        mouth.moveTo(size * 0.2, size * 0.68);
        for (int i = 1; i <= 6; i++) mouth.lineTo(size * (0.2 + i * 0.1), size * (i % 2 == 1 ? 0.62 : 0.68));
        g.setStroke(new BasicStroke(1.1f));
        g.draw(mouth);
    }
}
