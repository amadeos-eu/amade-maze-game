package eu.amadeos.games.maze.domain;

import eu.amadeos.gameengine.data.TextData;
import eu.amadeos.gameengine.tile.Cell;
import eu.amadeos.gameengine.tile.Direction;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MazeTest {
    /** A small maze: a ring of corridors round the house. */
    static final String SMALL = """
            row XXXXXXXXX
            row Xo.....oX
            row X.XX-XX.X
            row X.XHHHX.X
            row X.XXXXX.X
            row X...._..X
            row XXXXXXXXX
            muncher 5 5 left
            ghost red 4 1 left
            ghost pink 4 3 up
            ghost cyan 3 3 up
            ghost orange 5 3 up
            fruit 4 5
            """;

    static Maze read(String text) {
        return Maze.read(TextData.parse("maze.txt", new StringReader(text)));
    }

    @Test
    void theStandardMazeIsReadWithEverythingInPlace() {
        Maze maze = Maze.STANDARD;
        assertEquals(27, maze.width());
        assertEquals(30, maze.height());
        assertEquals(240, maze.tiles().count(Tile::food), "236 dots and 4 energizers");
        assertEquals(new Cell(13, 12), maze.door());
        assertEquals(new Cell(13, 11), maze.doorstep());
        assertEquals(new Cell(13, 14), maze.house());
        assertEquals(new Maze.Start(new Cell(13, 22), Direction.LEFT), maze.muncher());
        assertEquals(new Cell(24, -3), maze.corner(Kind.RED));
        assertEquals(10, maze.release(Kind.ORANGE));
        assertEquals(3, maze.toHouse().distance(maze.doorstep()));
    }

    @Test
    void aSmallMazeNeedsNoCornersAndNoReleaseTimes() {
        Maze maze = read(SMALL);
        assertEquals(new Cell(4, 3), maze.house());
        assertEquals(new Cell(3, 3), maze.corner(Kind.CYAN), "its start");
        assertEquals(0, maze.release(Kind.PINK));
    }

    // --8<-- [start:test-errors]
    @Test
    void mistakesInTheFileNameTheLine() {
        assertEquals("maze.txt:10: \"blue\" is not a ghost (red, pink, cyan, orange)",
                assertThrows(TextData.DataException.class, () -> read(SMALL.replace("ghost pink", "ghost blue")))
                        .getMessage());
        assertEquals("maze.txt:9: (0, 0) is not inside the maze",
                assertThrows(TextData.DataException.class, () -> read(SMALL.replace("red 4 1", "red 0 0")))
                        .getMessage());
        assertEquals("maze.txt:2: column 4: the muncher cannot reach this dot",
                assertThrows(TextData.DataException.class, () -> read(SMALL.replace("Xo.....oX", "Xo.X.X.oX")))
                        .getMessage(), "walled off: this level could never be cleared");
    }
    // --8<-- [end:test-errors]

    @Test
    void eachLevelGetsAFreshCopy() {
        var level = Maze.STANDARD.tiles();
        level.set(1, 1, Tile.FLOOR);
        assertEquals(Tile.DOT, Maze.STANDARD.tiles().get(1, 1));
        assertEquals(List.of(), Maze.STANDARD.tiles().cells(t -> t == null));
    }
}
