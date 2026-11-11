package morris.view;

import morris.controller.GameController;
import morris.model.GamePhase;
import morris.model.GameState;
import morris.model.Player;

import javax.swing.*;
import java.awt.*;

/** Status line, pieces-in-hand counters and the Undo / Save / Load buttons. */
public class ControlPanel extends JPanel {
    private final GameController controller;
    private final JLabel status = new JLabel(" ");
    private final JLabel hands = new JLabel(" ");
    private final JButton undo = new JButton("Undo");

    public ControlPanel(GameController controller, Runnable onSave, Runnable onLoad) {
        this.controller = controller;
        setLayout(new BorderLayout(8, 4));
        setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        JPanel labels = new JPanel(new GridLayout(2, 1));
        status.setFont(status.getFont().deriveFont(Font.BOLD, 14f));
        labels.add(status);
        labels.add(hands);
        add(labels, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        undo.addActionListener(e -> controller.undo());
        JButton save = new JButton("Save…");
        save.addActionListener(e -> onSave.run());
        JButton load = new JButton("Load…");
        load.addActionListener(e -> onLoad.run());
        buttons.add(undo);
        buttons.add(save);
        buttons.add(load);
        add(buttons, BorderLayout.EAST);
        refresh();
    }

    public void refresh() {
        GameState s = controller.state();
        status.setText(statusText(s));
        hands.setText("In hand: White " + s.handSize(Player.WHITE) + "   Black " + s.handSize(Player.BLACK)
                + "   |   On board: White " + s.board().count(Player.WHITE) + "   Black " + s.board().count(Player.BLACK));
        undo.setEnabled(controller.canUndo());
        undo.setVisible(controller.config().has(morris.model.HouseRule.UNDO_ENABLED));
    }

    private String statusText(GameState s) {
        if (s.phase() == GamePhase.FINISHED) {
            return name(s.winner()) + " wins!";
        }
        if (controller.isAiThinking()) {
            return "Computer is thinking…";
        }
        String who = name(s.currentPlayer());
        return switch (s.phase()) {
            case PLACING -> who + ": place a piece";
            case MOVING -> who + ": select a piece, then an adjacent empty node";
            case FLYING -> who + ": select a piece, then jump to any empty node";
            case REMOVING -> who + ": mill! remove an opponent piece (marked red)";
            default -> "";
        };
    }

    private String name(Player p) {
        String n = p == Player.WHITE ? "White" : "Black";
        return controller.config().vsComputer() && p == controller.config().aiPlayer() ? n + " (computer)" : n;
    }
}
