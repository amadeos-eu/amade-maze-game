package eu.amadeos.games.maze.domain;

/** How a game ended: the points, and the level the last life was lost in. */
public record GameResult(int score, int level) {
}
