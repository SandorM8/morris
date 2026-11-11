package morris.view;

import morris.model.*;

import javax.swing.*;
import java.awt.*;
import java.util.EnumSet;
import java.util.Set;

/** Modal dialog where the game mode, opponent, AI level and house rules are chosen (and then locked). */
public class AdvancedSetupDialog extends JDialog {
    private final JComboBox<GameMode> mode = new JComboBox<>(GameMode.values());
    private final JComboBox<OpponentType> opponent = new JComboBox<>(OpponentType.values());
    private final JComboBox<AIDifficulty> difficulty = new JComboBox<>(AIDifficulty.values());
    private final JCheckBox undo = new JCheckBox(HouseRule.UNDO_ENABLED.toString());
    private final JCheckBox flying = new JCheckBox(HouseRule.FLYING_ALLOWED.toString());
    private GameConfig result;

    private AdvancedSetupDialog(Frame owner, GameConfig initial) {
        super(owner, "Advanced Setup", true);
        mode.setSelectedItem(initial.mode());
        opponent.setSelectedItem(initial.opponentType());
        difficulty.setSelectedItem(initial.aiDifficulty());
        undo.setSelected(initial.has(HouseRule.UNDO_ENABLED));
        flying.setSelected(initial.has(HouseRule.FLYING_ALLOWED));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 4, 12));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
        row(form, c, 0, "Game mode:", mode);
        row(form, c, 1, "Opponent:", opponent);
        row(form, c, 2, "AI difficulty:", difficulty);
        row(form, c, 3, "House rules:", undo);
        row(form, c, 4, "", flying);
        JLabel note = new JLabel("These settings cannot be changed during the game.");
        note.setFont(note.getFont().deriveFont(Font.ITALIC));
        c.gridx = 0; c.gridy = 5; c.gridwidth = 2;
        form.add(note, c);

        mode.addActionListener(e -> syncEnabled());
        opponent.addActionListener(e -> syncEnabled());
        syncEnabled();

        JButton ok = new JButton("Start");
        ok.addActionListener(e -> {
            Set<HouseRule> rules = EnumSet.noneOf(HouseRule.class);
            if (undo.isSelected()) rules.add(HouseRule.UNDO_ENABLED);
            if (flying.isSelected() && flying.isEnabled()) rules.add(HouseRule.FLYING_ALLOWED);
            result = new GameConfig((GameMode) mode.getSelectedItem(), (OpponentType) opponent.getSelectedItem(),
                    (AIDifficulty) difficulty.getSelectedItem(), rules);
            dispose();
        });
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancel);
        buttons.add(ok);

        setLayout(new BorderLayout());
        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(ok);
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private void syncEnabled() {
        difficulty.setEnabled(opponent.getSelectedItem() == OpponentType.AI);
        boolean nine = mode.getSelectedItem() == GameMode.NINE_MENS_MORRIS;
        flying.setEnabled(nine);
        if (!nine) flying.setSelected(false);
    }

    private static void row(JPanel p, GridBagConstraints c, int y, String label, JComponent comp) {
        c.gridwidth = 1;
        c.gridx = 0; c.gridy = y;
        p.add(new JLabel(label), c);
        c.gridx = 1;
        p.add(comp, c);
    }

    /** Shows the dialog; returns the chosen config, or null if cancelled. */
    public static GameConfig show(Frame owner, GameConfig initial) {
        AdvancedSetupDialog d = new AdvancedSetupDialog(owner, initial);
        d.setVisible(true);
        return d.result;
    }
}
