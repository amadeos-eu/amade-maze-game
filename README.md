# amade-maze-game

**amade-maze-game** is the classic chase through a maze on the [amadeOS](https://github.com/datazuul/amadeos) desktop: eat every dot while four ghosts hunt you, each in its own way. Built on [amade-game-engine](https://github.com/datazuul/amade-game-engine); runs on other operating systems as well.

![License](https://img.shields.io/badge/license-EUPL--1.2-blue)
![Java](https://img.shields.io/badge/Java-25-orange)

## Playing

- **Enter** or **Space** (or F2) starts a game.
- The **arrow keys** (or W, A, S, D) steer the muncher. A key pressed before a crossing is kept until the way is open there, so you can press it a little early; back the way you came works at once.
- A **dot** is worth 10 points, an **energizer** 50. After an energizer the ghosts turn **blue** for a few seconds: they flee, and eating them brings 200, 400, 800 and 1,600 points. Their eyes fly home and they come back.
- The four ghosts hunt in different ways: **red** goes straight for you, **pink** aims four tiles ahead of you, **cyan** works together with red, and **orange** gives up when it comes close. Now and then they all scatter to their corners.
- **Fruit** appears twice a level below the ghosts' house: a cherry for 100 points in the first level, more in later ones.
- Three lives, an **extra life** at 10,000 points. Each level is a little faster, and the ghosts stay blue for less time.
- **P** or **Esc** pauses, and so does leaving the window.
- **Best scores**, ten of them. On the title screen the computer plays.
- **F11** switches to full screen and back. *Game → Graphics* chooses how the field reaches the screen: **Direct** (at the screen's full size) or **Pixels** (the field at 640 × 400, enlarged by whole factors). **F12** shows how long each frame takes.
- Sound effects and music can be switched off under *Game → Sound*.

The keys can be changed in `~/.config/amadeos/maze/maze.properties`, with the names Java gives them, several separated by spaces:

```properties
keys.up=UP W
keys.left=LEFT A
keys.down=DOWN S
keys.right=RIGHT D
keys.pause=P ESCAPE
```

Texts in English, Deutsch, Français, Español and Italiano.

## Files

| What | Where |
|---|---|
| Settings (graphics, sound, keys) | `~/.config/amadeos/maze/maze.properties` |
| Name for high scores | `~/.config/amadeos/maze/player.properties` |
| Best scores | `~/.local/share/amadeos/games/maze/classic.scores` |

## Building

```bash
./gradlew test          # the window test needs a display (xvfb-run on servers)
./gradlew run
./gradlew deb           # build/distributions/amade-maze-game_<version>_all.deb
./gradlew installToAmadeOs   # copies the package to ../amadeos/config/packages.chroot/
```

The maze is the text file `src/main/resources/eu/amadeos/games/maze/maze.txt`: a letter per tile, plus where everybody starts, the ghosts' corners and when they leave the house. Change it without compiling; a mistake is reported with the file and the line. The muncher, the ghosts and the fruit are drawn in code, the walls are worked out from the tiles. The icon is drawn by `tools/MakeIcon.java`. `./gradlew screenshots -Pshots=<folder>` draws the handbook's screenshots.

With `../amade-game-engine` (and `../amade-commons`) checked out next to this repository, Gradle builds those checkouts instead of the JitPack versions.

## Structure

- `domain`: the rules without user interface or time of their own. `Game` (the level's tiles, the muncher and the ghosts, scatter and chase by turns, the energizer's spell, points, lives, levels; chance from a seeded random generator; it reports `Event`s), `Maze` (reads `maze.txt`; the ways home of the ghosts as distance maps), `Tile`, `Muncher` (walks the corridors, keeps the wanted turn), `Ghost` (its modes as a state machine, and how it chooses its way at a crossing), `Kind` (the four ghosts and the tiles they aim at), `Fruit`, `Field` (speeds, times, points), `Pilot` (plays the title screen's demo with breadth-first search and A*), `GameResult`.
- `repository`: `MazeSettings` (graphics, sound, keys).
- `ui.game`: the running game on the loop thread. `MazeGame` (loop, controls, scenes: title, game, pause, end), `MazePainter` (the walls as one shape from the tiles, dots, fruit, ghosts and muncher, drawn between update steps), `FruitPictures`, `Hud` (score, level, lives, points, and the texts), `Effects` (events become point labels, sounds and music), `SoundBank`, `Music` (the tunes, in MML), `Arena` (what the presenter may ask of the game).
- `ui.main`: Model-View-Presenter for everything around the field: `MazePresenter`, `MazeView`, `MazeWindow`.

From the engine: `TileMap`, `Walker` and `Direction`, `StateMachine`, `DistanceMap` and `AStar`, `TextData`, `GameLoop`, `GameCanvas`, `Sprite`, `SeededRandom`, `Sound`, `Tune` and `Mixer`, `Keyboard` and `Controls`, `SceneStack`, `FullScreen`, high scores.

Comments of the form `// --8<-- [start:name]` mark the excerpts the [Game Developer's Handbook](https://datazuul.github.io/amade-game-engine/) quotes; chapter 25 is about this game.

## Licence

[EUPL-1.2](LICENSE)
