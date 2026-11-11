package morris.view;

import morris.controller.GameController;
import morris.logic.BoardTopology;
import morris.model.Piece;
import morris.model.Player;
import morris.model.Position;

import javax.swing.JPanel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.util.List;
import java.util.Set;

/** Draws the board and pieces with Graphics2D and turns mouse clicks into board positions. */
public class BoardPanel extends JPanel {
    private static final Color BOARD = new Color(222, 184, 135);
    private static final Color LINE = new Color(80, 52, 28);

    private final GameController controller;

    public BoardPanel(GameController controller) {
        this.controller = controller;
        setPreferredSize(new Dimension(560, 560));
        setMinimumSize(new Dimension(320, 320));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Position p = positionAt(e.getX(), e.getY());
                if (p != null) {
                    controller.onPositionClicked(p);
                }
            }
        });
    }

    // geometry -----------------------------------------------------------------

    private double margin() {
        return Math.min(getWidth(), getHeight()) * 0.08;
    }

    private double step() {
        BoardTopology t = topology();
        return (Math.min(getWidth(), getHeight()) - 2 * margin()) / (t.gridSize() - 1);
    }

    private double originX() {
        BoardTopology t = topology();
        return (getWidth() - step() * (t.gridSize() - 1)) / 2.0;
    }

    private double originY() {
        BoardTopology t = topology();
        return (getHeight() - step() * (t.gridSize() - 1)) / 2.0;
    }

    private BoardTopology topology() {
        return controller.logic().topology();
    }

    private double x(Position p) {
        return originX() + topology().col(p) * step();
    }

    private double y(Position p) {
        return originY() + topology().row(p) * step();
    }

    private double pieceRadius() {
        double wanted = step() * (topology().gridSize() == 7 ? 0.30 : 0.16);
        return Math.max(8, Math.min(wanted, margin() * 0.85)); // keep edge pieces inside the panel
    }

    /** The node under the given pixel, or null. */
    Position positionAt(int px, int py) {
        double limit = pieceRadius() * 1.2;
        Position best = null;
        double bestDist = Double.MAX_VALUE;
        for (Position p : topology().positions()) {
            double d = Math.hypot(px - x(p), py - y(p));
            if (d <= limit && d < bestDist) {
                best = p;
                bestDist = d;
            }
        }
        return best;
    }

    // painting -----------------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(BOARD);
        g.fillRect(0, 0, getWidth(), getHeight());

        BoardTopology topo = topology();
        double r = pieceRadius();

        g.setColor(LINE);
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (List<Position> line : topo.mills()) {
            Position a = line.get(0), b = line.get(2);
            g.drawLine((int) x(a), (int) y(a), (int) x(b), (int) y(b));
        }
        for (Position p : topo.positions()) {
            double dot = r * 0.22;
            g.fill(new Ellipse2D.Double(x(p) - dot, y(p) - dot, 2 * dot, 2 * dot));
        }

        Set<Position> targets = controller.targets();
        List<Position> removable = controller.removable();
        for (Position p : topo.positions()) {
            Piece piece = controller.state().board().getPiece(p);
            if (piece != null) {
                drawPiece(g, x(p), y(p), r, piece.owner());
            }
            if (targets.contains(p)) {
                g.setColor(new Color(40, 160, 60, 200));
                g.fill(new Ellipse2D.Double(x(p) - r * 0.45, y(p) - r * 0.45, r * 0.9, r * 0.9));
            }
            if (removable.contains(p)) {
                g.setColor(new Color(210, 30, 30));
                g.setStroke(new BasicStroke(4f));
                g.draw(new Ellipse2D.Double(x(p) - r - 3, y(p) - r - 3, 2 * r + 6, 2 * r + 6));
            }
        }
        Position sel = controller.selected();
        if (sel != null) {
            g.setColor(new Color(240, 190, 0));
            g.setStroke(new BasicStroke(4f));
            g.draw(new Ellipse2D.Double(x(sel) - r - 3, y(sel) - r - 3, 2 * r + 6, 2 * r + 6));
        }
        g.dispose();
    }

    private void drawPiece(Graphics2D g, double cx, double cy, double r, Player owner) {
        Ellipse2D disc = new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r);
        boolean white = owner == Player.WHITE;
        g.setPaint(new GradientPaint((float) (cx - r), (float) (cy - r),
                white ? new Color(255, 255, 255) : new Color(90, 90, 90),
                (float) (cx + r), (float) (cy + r),
                white ? new Color(205, 205, 205) : new Color(10, 10, 10)));
        g.fill(disc);
        g.setColor(white ? new Color(90, 90, 90) : Color.BLACK);
        g.setStroke(new BasicStroke(2f));
        g.draw(disc);
    }
}
