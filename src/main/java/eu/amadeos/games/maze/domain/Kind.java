package eu.amadeos.games.maze.domain;

import eu.amadeos.gameengine.tile.Cell;
import eu.amadeos.gameengine.tile.Direction;

/**
 * The four ghosts. They move by the same rule, and differ only in the tile they aim at while they hunt:
 * that one difference gives each a character of its own.
 */
// --8<-- [start:kind]
public enum Kind {
    /** Goes straight for the muncher. */
    RED,
    /** Aims four tiles ahead of the muncher, to cut it off. */
    PINK,
    /** Aims past the muncher, as far again as the red ghost is from a point two tiles ahead of it. */
    CYAN,
    /** Hunts like red while far away, but runs to its corner when it comes within eight tiles. */
    ORANGE;

    /** The tile this ghost aims at while it hunts. */
    Cell target(Game game, Ghost self) {
        Muncher muncher = game.muncher();
        Cell at = muncher.walker().cell();
        Direction facing = muncher.facing();
        return switch (this) {
            case RED -> at;
            case PINK -> ahead(at, facing, 4);
            case CYAN -> {
                Cell pivot = ahead(at, facing, 2);
                Cell red = game.ghost(RED).walker().cell();
                yield new Cell(2 * pivot.col() - red.col(), 2 * pivot.row() - red.row());
            }
            case ORANGE -> Ghost.distanceSquared(self.walker().cell(), at) > 8 * 8 ? at : self.corner();
        };
    }

    private static Cell ahead(Cell at, Direction facing, int tiles) {
        return new Cell(at.col() + facing.dx * tiles, at.row() + facing.dy * tiles);
    }
}
// --8<-- [end:kind]
