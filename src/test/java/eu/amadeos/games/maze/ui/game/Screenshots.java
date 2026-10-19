package eu.amadeos.games.maze.ui.game;

import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.audio.Mixer;
import eu.amadeos.gameengine.input.Keyboard;
import eu.amadeos.gameengine.path.AStar;
import eu.amadeos.gameengine.path.DistanceMap;
import eu.amadeos.gameengine.tile.Cell;
import eu.amadeos.games.maze.bootstrap.MazeModule;
import eu.amadeos.games.maze.domain.Event;
import eu.amadeos.games.maze.domain.Field;
import eu.amadeos.games.maze.domain.Game;
import eu.amadeos.games.maze.domain.Ghost;
import eu.amadeos.games.maze.domain.Maze;
import eu.amadeos.games.maze.domain.Pilot;
import eu.amadeos.games.maze.domain.Tile;
import eu.amadeos.games.maze.repository.MazeSettings;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.Locale;

/**
 * The screenshots of chapter 25, drawn without a screen: the title, a game with blue ghosts, the
 * pilot's search, the eyes' way home, the wall pieces. {@code ./gradlew screenshots -Pshots=<folder>}.
 */
public final class Screenshots {
    private final long[] now = {0};
    private final Keyboard keyboard = new Keyboard(() -> now[0]);
    private final MazeGame game;

    private Screenshots() throws Exception {
        Messages msg = Messages.load(MazeModule.class, MazeModule.MESSAGES, Locale.ENGLISH);
        long[] seeds = {3};
        game = new MazeGame(keyboard, msg,
                new MazeSettings(Files.createTempDirectory("maze").resolve("maze.properties")), () -> seeds[0]++,
                new Mixer(), Runnable::run);
    }

    public static void main(String[] args) throws Exception {
        File dir = new File(args.length > 0 ? args[0] : "screenshots");
        dir.mkdirs();
        Screenshots shots = new Screenshots();
        shots.steps(14 * 120);
        shots.save(new File(dir, "title.png"), true, null);
        // The demo: blue ghosts, one eaten, its eyes on their way home.
        MazeGame.TitleScene title = (MazeGame.TitleScene) shots.game.top();
        for (int i = 0; i < 600 * 120; i++) {
            shots.steps(1);
            Game demo = title.demo();
            if (demo.phase() == Game.Phase.PLAYING && demo.frightLeft() > 2.5 && eyes(demo) >= 1 && blue(demo) >= 2) break;
        }
        shots.steps(40);
        shots.save(new File(dir, "blue.png"), false, null);
        // A moment of the hunt, with the pilot's plan drawn over it.
        for (int i = 0; i < 600 * 120; i++) {
            shots.steps(1);
            Game demo = title.demo();
            if (demo.phase() == Game.Phase.PLAYING && !demo.frightened() && hunting(demo) == 4
                    && demo.dotsLeft() < 170 && title.pilot().path() != null && title.pilot().path().cells().size() > 8) {
                break;
            }
        }
        shots.save(new File(dir, "plan.png"), false, title);
        distances(new File(dir, "distances.png"), title.pilot());
        pieces(new File(dir, "walls.png"));
        System.exit(0);
    }

    private static int eyes(Game game) {
        int n = 0;
        for (Ghost ghost : game.ghosts()) if (ghost.eyes()) n++;
        return n;
    }

    private static int blue(Game game) {
        int n = 0;
        for (Ghost ghost : game.ghosts()) if (ghost.mode() == Ghost.Mode.FRIGHTENED) n++;
        return n;
    }

    private static int hunting(Game game) {
        int n = 0;
        for (Ghost ghost : game.ghosts()) if (ghost.dangerous()) n++;
        return n;
    }

    private void steps(int n) {
        for (int i = 0; i < n; i++) {
            now[0] += (long) (MazeGame.STEP * 1e9);
            game.update(MazeGame.STEP);
        }
    }

    /** Twice the logical size, as on a 1280 × 800 screen; with the pilot's plan, if a title is given. */
    private void save(File file, boolean overlay, MazeGame.TitleScene plan) throws Exception {
        BufferedImage image = new BufferedImage(2 * (int) Field.WIDTH, 2 * (int) Field.HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.scale(2, 2);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        game.render(g, 0);
        if (plan != null) {
            MazePainter painter = game.painter();
            Game demo = plan.demo();
            // The danger the pilot sees, as red squares, and its way, as a line.
            for (int row = 0; row < demo.tiles().height(); row++) {
                for (int col = 0; col < demo.tiles().width(); col++) {
                    if (!demo.tiles().get(col, row).corridor()) continue;
                    double danger = Pilot.danger(demo, col, row);
                    if (danger <= 0) continue;
                    g.setColor(new Color(255, 40, 40, (int) Math.min(150, 40 + danger / 4)));
                    g.fill(new Rectangle2D.Double(painter.x(col) - 6, painter.y(row) - 6, 12, 12));
                }
            }
            AStar.Path path = plan.pilot().path();
            Path2D.Double line = new Path2D.Double();
            Cell first = path.cells().getFirst();
            line.moveTo(painter.x(first.col()), painter.y(first.row()));
            for (Cell cell : path.cells()) line.lineTo(painter.x(cell.col()), painter.y(cell.row()));
            g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(0x40FF80));
            g.draw(line);
        }
        if (overlay || game.top() instanceof MazeGame.PlayScene) game.renderOverlay(g, 0);
        else if (plan == null && game.top() instanceof MazeGame.TitleScene title) game.status(g, title.demo());
        g.dispose();
        ImageIO.write(image, "png", file);
    }

    /** The maze with the number of steps to the doorstep in every corridor, as the eyes see it. */
    private static void distances(File file, Pilot pilot) throws Exception {
        Maze maze = Maze.STANDARD;
        DistanceMap home = maze.toDoorstep();
        int tile = 24;
        BufferedImage image = new BufferedImage(maze.width() * tile, maze.height() * tile, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        int far = 0;
        for (int row = 0; row < maze.height(); row++) {
            for (int col = 0; col < maze.width(); col++) {
                int d = home.distance(col, row);
                if (d != DistanceMap.UNREACHABLE && maze.tile(col, row) != Tile.WALL) far = Math.max(far, d);
            }
        }
        for (int row = 0; row < maze.height(); row++) {
            for (int col = 0; col < maze.width(); col++) {
                Tile t = maze.tile(col, row);
                int d = home.distance(col, row);
                if (t == Tile.WALL) {
                    g.setColor(new Color(0x0A1440));
                } else {
                    float warm = 1 - (float) d / far;
                    g.setColor(Color.getHSBColor(0.62f - 0.62f * warm, 0.55f, 0.35f + 0.4f * warm));
                }
                g.fillRect(col * tile, row * tile, tile, tile);
                if (t != Tile.WALL && d != DistanceMap.UNREACHABLE) {
                    g.setColor(Color.WHITE);
                    String text = Integer.toString(d);
                    g.drawString(text, col * tile + (tile - g.getFontMetrics().stringWidth(text)) / 2, row * tile + 16);
                }
            }
        }
        g.dispose();
        ImageIO.write(image, "png", file);
    }

    /** The sixteen wall pieces of the mask, each with its number, drawn as the painter draws them. */
    private static void pieces(File file) throws Exception {
        int cell = 64;
        BufferedImage image = new BufferedImage(16 * cell, cell + 18, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(0x101010));
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        double a = MazePainter.INSET;
        double b = 1 - a;
        for (int mask = 0; mask < 16; mask++) {
            Graphics2D t = (Graphics2D) g.create();
            t.translate(mask * cell + 8, 4);
            t.scale(cell - 16, cell - 16);
            t.setColor(new Color(0x202838));
            t.fill(new Rectangle2D.Double(0, 0, 1, 1));
            Path2D.Double piece = new Path2D.Double();
            piece.append(new Rectangle2D.Double(a, a, b - a, b - a), false);
            if ((mask & 1) != 0) piece.append(new Rectangle2D.Double(a, 0, b - a, a), false);
            if ((mask & 2) != 0) piece.append(new Rectangle2D.Double(0, a, a, b - a), false);
            if ((mask & 4) != 0) piece.append(new Rectangle2D.Double(a, b, b - a, a), false);
            if ((mask & 8) != 0) piece.append(new Rectangle2D.Double(b, a, a, b - a), false);
            java.awt.geom.Area area = new java.awt.geom.Area(piece);
            t.setColor(MazePainter.WALL_FILL);
            t.fill(area);
            t.setColor(MazePainter.WALL_EDGE);
            t.setStroke(new BasicStroke(0.04f));
            t.draw(area);
            t.dispose();
            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            String text = Integer.toString(mask);
            g.drawString(text, mask * cell + (cell - g.getFontMetrics().stringWidth(text)) / 2, cell + 12);
        }
        g.dispose();
        ImageIO.write(image, "png", file);
    }
}
