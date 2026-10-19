package eu.amadeos.games.maze.bootstrap;

import eu.amadeos.commons.app.AmadeApplication;
import eu.amadeos.commons.app.AppInfo;
import eu.amadeos.gameengine.audio.Mixer;
import eu.amadeos.games.maze.ui.game.MazeGame;
import eu.amadeos.games.maze.ui.main.MazePresenter;
import eu.amadeos.games.maze.ui.main.MazeWindow;

public final class AmadeMazeApplication {
    public static final AppInfo APP = new AppInfo("amade-maze-game", "Maze", "maze");

    private AmadeMazeApplication() {
    }

    /** The version from the jar's manifest; "dev" when run from the IDE. */
    public static String version() {
        String version = AmadeMazeApplication.class.getPackage().getImplementationVersion();
        return version == null ? "dev" : version;
    }

    // --8<-- [start:main]
    public static void main(String[] args) {
        AmadeApplication.launch(APP, injector -> {
            Mixer mixer = injector.getInstance(Mixer.class);
            mixer.start();
            MazeWindow window = injector.getInstance(MazeWindow.class);
            MazePresenter presenter = injector.getInstance(MazePresenter.class);
            MazeGame game = injector.getInstance(MazeGame.class);
            window.bind(presenter);
            presenter.start();
            window.whenClosed(() -> {
                game.stop();
                mixer.close();
                System.exit(0);
            });
            window.setLocationByPlatform(true);
            window.setVisible(true);
            game.start(window.canvas());
        }, new MazeModule());
    }
    // --8<-- [end:main]
}
