package morris.view;

import morris.controller.GameController;
import morris.model.GameConfig;
import morris.model.Player;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.io.IOException;
import java.nio.file.Path;

/** The main window: menu bar, board and control panel. */
public class GameFrame extends JFrame implements GameController.Listener {
    private final GameController controller;
    private final BoardPanel boardPanel;
    private final ControlPanel controlPanel;
    private final JFileChooser chooser = new JFileChooser();

    public GameFrame() {
        super("Nine Men's Morris");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        controller = new GameController(GameConfig.defaults());
        controller.addListener(this);
        boardPanel = new BoardPanel(controller);
        controlPanel = new ControlPanel(controller, this::saveGame, this::loadGame);
        chooser.setFileFilter(new FileNameExtensionFilter("Morris save files (*.json)", "json"));

        setJMenuBar(buildMenu());
        setLayout(new BorderLayout());
        add(boardPanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(null);
    }

    private JMenuBar buildMenu() {
        JMenuBar bar = new JMenuBar();
        JMenu game = new JMenu("Game");
        JMenu newGame = new JMenu("New Game");
        JMenuItem quick = new JMenuItem("Same settings");
        quick.addActionListener(e -> controller.newGame(controller.config()));
        JMenuItem advanced = new JMenuItem("Advanced Setup…");
        advanced.addActionListener(e -> advancedSetup());
        newGame.add(quick);
        newGame.add(advanced);
        JMenuItem save = new JMenuItem("Save…");
        save.addActionListener(e -> saveGame());
        JMenuItem load = new JMenuItem("Load…");
        load.addActionListener(e -> loadGame());
        JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener(e -> dispose());
        game.add(newGame);
        game.addSeparator();
        game.add(save);
        game.add(load);
        game.addSeparator();
        game.add(exit);
        bar.add(game);
        return bar;
    }

    private void advancedSetup() {
        GameConfig config = AdvancedSetupDialog.show(this, controller.config());
        if (config != null) {
            controller.newGame(config);
        }
    }

    private void saveGame() {
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path path = chooser.getSelectedFile().toPath();
        if (!path.getFileName().toString().toLowerCase().endsWith(".json")) {
            path = path.resolveSibling(path.getFileName() + ".json");
        }
        try {
            controller.save(path);
        } catch (IOException ex) {
            error("Could not save the game:\n" + ex.getMessage());
        }
    }

    private void loadGame() {
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            controller.load(chooser.getSelectedFile().toPath());
        } catch (IOException ex) {
            error("Could not load the game:\n" + ex.getMessage());
        }
    }

    private void error(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    // GameController.Listener ----------------------------------------------------

    @Override
    public void stateChanged() {
        boardPanel.revalidate();
        boardPanel.repaint();
        controlPanel.refresh();
    }

    @Override
    public void gameOver(Player winner) {
        boardPanel.repaint();
        Object[] options = {"New Game", "Close"};
        int choice = JOptionPane.showOptionDialog(this,
                (winner == Player.WHITE ? "White" : "Black") + " wins!", "Game over",
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
        if (choice == 0) {
            advancedSetup();
        }
    }
}
