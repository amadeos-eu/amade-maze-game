package eu.amadeos.games.maze.domain;

import eu.amadeos.gameengine.data.TextData;
import eu.amadeos.gameengine.path.DistanceMap;
import eu.amadeos.gameengine.tile.Cell;
import eu.amadeos.gameengine.tile.Direction;
import eu.amadeos.gameengine.tile.TileMap;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The maze as read from {@code maze.txt}: the tiles, where the muncher and the ghosts start, the ghosts'
 * corners and the seconds they wait in the house, the place of the fruit. Everything here is fixed; a level
 * plays on a {@link #tiles() copy} of the tiles, from which it eats the dots.
 *
 * <p>Also fixed, and so computed once here: the two ways every ghost needs to find from anywhere, to the
 * tile in front of the house's door and into the house, as {@link DistanceMap}s.
 */
public final class Maze {
    /** The maze of the game, from {@code maze.txt} next to this class. */
    public static final Maze STANDARD = read(TextData.load(Maze.class, "/eu/amadeos/games/maze/maze.txt"));

    /** Where the muncher or a ghost starts, and the way it looks. */
    public record Start(Cell cell, Direction heading) {
    }

    private final TileMap<Tile> tiles;
    private final Start muncher;
    private final Map<Kind, Start> ghosts;
    private final Map<Kind, Cell> corners;
    private final Map<Kind, Double> release;
    private final Cell fruit;
    private final Cell door;
    private final Cell doorstep;
    private final Cell house;
    private final DistanceMap toDoorstep;
    private final DistanceMap toHouse;

    private Maze(TileMap<Tile> tiles, Start muncher, Map<Kind, Start> ghosts, Map<Kind, Cell> corners,
                 Map<Kind, Double> release, Cell fruit, Cell door) {
        this.tiles = tiles;
        this.muncher = muncher;
        this.ghosts = ghosts;
        this.corners = corners;
        this.release = release;
        this.fruit = fruit;
        this.door = door;
        this.doorstep = door.next(Direction.UP);
        Cell inside = door.next(Direction.DOWN);
        while (tiles.get(inside.next(Direction.DOWN)) == Tile.HOUSE) inside = inside.next(Direction.DOWN);
        this.house = inside;
        // --8<-- [start:ways-home]
        // A ghost flying home may pass the door, and nothing stops it but walls.
        this.toDoorstep = DistanceMap.search(tiles, (col, row) -> tiles.get(col, row) != Tile.WALL, doorstep);
        this.toHouse = DistanceMap.search(tiles, (col, row) -> tiles.get(col, row) != Tile.WALL, house);
        // --8<-- [end:ways-home]
    }

    // --8<-- [start:read]
    /** Reads a maze; every mistake is an error that names the file and the line. */
    public static Maze read(List<TextData.Line> lines) {
        List<TextData.Line> rows = new ArrayList<>();
        Start muncher = null;
        Map<Kind, Start> ghosts = new EnumMap<>(Kind.class);
        Map<Kind, Cell> corners = new EnumMap<>(Kind.class);
        Map<Kind, Double> release = new EnumMap<>(Kind.class);
        Cell fruit = null;
        List<TextData.Line> places = new ArrayList<>();
        for (TextData.Line line : lines) {
            switch (line.keyword()) {
                case "row" -> rows.add(line);
                case "muncher" -> {
                    muncher = new Start(cell(line, 1), direction(line, 3));
                    places.add(line);
                }
                case "ghost" -> {
                    ghosts.put(kind(line), new Start(cell(line, 2), direction(line, 4)));
                    places.add(line);
                }
                case "corner" -> corners.put(kind(line), cell(line, 2));
                case "release" -> release.put(kind(line), line.number(2));
                case "fruit" -> {
                    fruit = cell(line, 1);
                    places.add(line);
                }
                default -> throw line.error("\"" + line.keyword() + "\" is not a line of a maze");
            }
        }
        TileMap<Tile> tiles = TileMap.read(rows, Tile.LEGEND);
        TextData.Line last = lines.getLast();
        if (muncher == null) throw last.error("the muncher has no start");
        if (fruit == null) throw last.error("the fruit has no place");
        for (Kind kind : Kind.values()) {
            if (!ghosts.containsKey(kind)) throw last.error("the " + name(kind) + " ghost has no start");
            if (!corners.containsKey(kind)) corners.put(kind, ghosts.get(kind).cell());
            release.putIfAbsent(kind, 0.0);
        }
        for (TextData.Line line : places) {
            Cell at = line.keyword().equals("ghost") ? cell(line, 2) : cell(line, 1);
            if (!tiles.contains(at.col(), at.row()) || tiles.get(at) == Tile.WALL) {
                throw line.error("(" + at.col() + ", " + at.row() + ") is not inside the maze");
            }
        }
        List<Cell> doors = tiles.cells(tile -> tile == Tile.DOOR);
        if (doors.size() != 1) throw rows.getFirst().error("a maze needs one door, not " + doors.size());
        Cell door = doors.getFirst();
        if (!tiles.get(door.next(Direction.UP)).corridor() || tiles.get(door.next(Direction.DOWN)) != Tile.HOUSE) {
            throw rows.get(door.row()).error("the door needs a corridor above it and the house below");
        }
        // Every dot must be reachable, or a level can never be cleared.
        DistanceMap reach = DistanceMap.search(tiles, (col, row) -> tiles.get(col, row).corridor(), muncher.cell());
        for (Cell dot : tiles.cells(Tile::food)) {
            if (reach.distance(dot) == DistanceMap.UNREACHABLE) {
                throw rows.get(dot.row()).error("column " + dot.col() + ": the muncher cannot reach this dot");
            }
        }
        return new Maze(tiles, muncher, ghosts, corners, release, fruit, door);
    }
    // --8<-- [end:read]

    private static Cell cell(TextData.Line line, int word) {
        return new Cell(line.integer(word), line.integer(word + 1));
    }

    private static Direction direction(TextData.Line line, int word) {
        String name = line.word(word);
        for (Direction d : Direction.values()) {
            if (d.name().equalsIgnoreCase(name)) return d;
        }
        throw line.error("\"" + name + "\" is not a direction (up, left, down, right)");
    }

    private static Kind kind(TextData.Line line) {
        String name = line.word(1);
        for (Kind kind : Kind.values()) {
            if (name(kind).equals(name)) return kind;
        }
        throw line.error("\"" + name + "\" is not a ghost (red, pink, cyan, orange)");
    }

    private static String name(Kind kind) {
        return kind.name().toLowerCase(Locale.ROOT);
    }

    /** A fresh copy of the tiles, all dots in place: the field of a new level. */
    public TileMap<Tile> tiles() {
        return tiles.copy();
    }

    public int width() {
        return tiles.width();
    }

    public int height() {
        return tiles.height();
    }

    /** The fixed tiles; read only. */
    public Tile tile(int col, int row) {
        return tiles.get(col, row);
    }

    public Start muncher() {
        return muncher;
    }

    public Start ghost(Kind kind) {
        return ghosts.get(kind);
    }

    public Cell corner(Kind kind) {
        return corners.get(kind);
    }

    /** Seconds after the start of a round the ghost leaves the house, in the first level. */
    public double release(Kind kind) {
        return release.get(kind);
    }

    public Cell fruit() {
        return fruit;
    }

    public Cell door() {
        return door;
    }

    /** The tile in front of the door, where ghosts leave the house and where eaten ghosts fly to. */
    public Cell doorstep() {
        return doorstep;
    }

    /** The tile inside the house where eaten ghosts come back to life. */
    public Cell house() {
        return house;
    }

    public DistanceMap toDoorstep() {
        return toDoorstep;
    }

    public DistanceMap toHouse() {
        return toHouse;
    }
}
