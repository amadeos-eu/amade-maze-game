import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Draws the program icon of Maze: the muncher, mouth open, a row of dots and a ghost behind them, in a
 * dark square with a blue wall round it. Drawn in code, as the game draws its figures, as a PNG of the
 * given size.
 *
 * <pre>
 * java tools/MakeIcon.java 48 src/main/resources/icons/amade-maze-game.png
 * java tools/MakeIcon.java 256 build/icon-256.png
 * python3 -c "from PIL import Image; Image.open('build/icon-256.png').save('packaging/windows/amade-maze-game.ico',
 *     sizes=[(16,16),(24,24),(32,32),(48,48),(64,64),(128,128),(256,256)])"
 * </pre>
 *
 * The files are part of the repository; run this again only to change the icon.
 */
public class MakeIcon {
    public static void main(String[] args) throws IOException {
        int size = Integer.parseInt(args[0]);
        File file = new File(args[1]);
        if (file.getParentFile() != null) file.getParentFile().mkdirs();
        ImageIO.write(icon(size), "png", file);
    }

    static BufferedImage icon(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.scale(size / 48.0, size / 48.0);
        RoundRectangle2D.Double back = new RoundRectangle2D.Double(2, 2, 44, 44, 10, 10);
        g.setPaint(new GradientPaint(0, 2, new Color(0x141C48), 0, 46, new Color(0x04060E)));
        g.fill(back);
        g.setColor(new Color(0x3C64FF));
        g.setStroke(new BasicStroke(2f));
        g.draw(back);
        // The dots between them.
        g.setColor(new Color(0xFFD8B8));
        for (double x : new double[]{25, 30.5}) g.fill(new Ellipse2D.Double(x - 1.4, 30.6, 2.8, 2.8));
        // The ghost, top right, looking at the muncher.
        Path2D.Double ghost = new Path2D.Double();
        ghost.moveTo(26, 22);
        ghost.curveTo(26, 7, 43, 7, 43, 22);
        ghost.lineTo(43, 26);
        ghost.lineTo(40.2, 23.5);
        ghost.lineTo(37.4, 26);
        ghost.lineTo(34.5, 23.5);
        ghost.lineTo(31.6, 26);
        ghost.lineTo(28.8, 23.5);
        ghost.lineTo(26, 26);
        ghost.closePath();
        g.setColor(new Color(0xFF2828));
        g.fill(ghost);
        for (double ex : new double[]{30.5, 37}) {
            g.setColor(Color.WHITE);
            g.fill(new Ellipse2D.Double(ex - 2.4, 12.5, 4.8, 6));
            g.setColor(new Color(0x2030C0));
            g.fill(new Ellipse2D.Double(ex - 2.6, 15, 2.8, 2.8));
        }
        // The muncher, bottom left, its mouth open to the right.
        g.setColor(new Color(0xFFE030));
        g.fill(new Arc2D.Double(4, 20, 20, 20, 38, 284, Arc2D.PIE));
        g.dispose();
        return image;
    }
}
