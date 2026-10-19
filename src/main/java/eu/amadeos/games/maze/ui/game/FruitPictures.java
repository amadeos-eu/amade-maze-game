package eu.amadeos.games.maze.ui.game;

import eu.amadeos.games.maze.domain.Fruit;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;

/** The fruits, drawn in code from circles and curves, in a square of {@code size}. */
final class FruitPictures {
    private FruitPictures() {
    }

    static void draw(Graphics2D g, double size, Fruit fruit) {
        double s = size;
        switch (fruit) {
            case CHERRY -> {
                stem(g, s * 0.3, s * 0.62, s * 0.62, s * 0.18, new Color(0x9A5A20));
                stem(g, s * 0.68, s * 0.7, s * 0.62, s * 0.18, new Color(0x9A5A20));
                ball(g, s * 0.3, s * 0.66, s * 0.22, new Color(0xE82020));
                ball(g, s * 0.66, s * 0.74, s * 0.22, new Color(0xE82020));
            }
            case STRAWBERRY -> {
                Path2D.Double berry = new Path2D.Double();
                berry.moveTo(s * 0.5, s * 0.92);
                berry.curveTo(s * 0.1, s * 0.6, s * 0.12, s * 0.28, s * 0.5, s * 0.3);
                berry.curveTo(s * 0.88, s * 0.28, s * 0.9, s * 0.6, s * 0.5, s * 0.92);
                g.setColor(new Color(0xE82040));
                g.fill(berry);
                g.setColor(Color.WHITE);
                for (double[] p : new double[][]{{0.38, 0.45}, {0.6, 0.48}, {0.48, 0.62}, {0.4, 0.74}, {0.6, 0.7}}) {
                    g.fill(new Ellipse2D.Double(s * p[0] - 0.6, s * p[1] - 0.6, 1.2, 1.2));
                }
                leaves(g, s);
            }
            case ORANGE -> {
                ball(g, s * 0.5, s * 0.58, s * 0.36, new Color(0xFF9A20));
                leaves(g, s);
            }
            case APPLE -> {
                ball(g, s * 0.5, s * 0.58, s * 0.36, new Color(0xE02828));
                g.setColor(new Color(0xFFB0B0));
                g.fill(new Ellipse2D.Double(s * 0.3, s * 0.42, s * 0.12, s * 0.16));
                stem(g, s * 0.5, s * 0.3, s * 0.56, s * 0.12, new Color(0x9A5A20));
            }
            case GRAPES -> {
                Color green = new Color(0x70D040);
                double[][] grapes = {{0.36, 0.38}, {0.64, 0.38}, {0.5, 0.52}, {0.3, 0.56}, {0.7, 0.56}, {0.42, 0.7},
                        {0.58, 0.7}, {0.5, 0.84}};
                for (double[] p : grapes) ball(g, s * p[0], s * p[1], s * 0.13, green);
                stem(g, s * 0.5, s * 0.3, s * 0.5, s * 0.1, new Color(0x9A5A20));
            }
        }
    }

    private static void ball(Graphics2D g, double x, double y, double r, Color colour) {
        g.setColor(colour);
        g.fill(new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r));
    }

    private static void stem(Graphics2D g, double x0, double y0, double x1, double y1, Color colour) {
        g.setColor(colour);
        g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D.Double stem = new Path2D.Double();
        stem.moveTo(x0, y0);
        stem.quadTo((x0 + x1) / 2 - 2, (y0 + y1) / 2, x1, y1);
        g.draw(stem);
    }

    private static void leaves(Graphics2D g, double s) {
        g.setColor(new Color(0x30B040));
        g.fill(new Ellipse2D.Double(s * 0.32, s * 0.16, s * 0.2, s * 0.12));
        g.fill(new Ellipse2D.Double(s * 0.48, s * 0.16, s * 0.2, s * 0.12));
    }
}
