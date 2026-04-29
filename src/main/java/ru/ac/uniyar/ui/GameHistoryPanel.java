package ru.ac.uniyar.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import ru.ac.uniyar.model.Move;

import java.util.List;

public class GameHistoryPanel extends Div {
    private final Div historyPanel = new Div();

    public GameHistoryPanel(Component... controls) {
        getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "12px")
                .set("width", "360px")
                .set("min-width", "360px")
                .set("height", "calc(100vh - 88px)")
                .set("max-height", "calc(100vh - 88px)")
                .set("box-sizing", "border-box")
                .set("overflow", "hidden");

        historyPanel.setWidthFull();
        historyPanel.getStyle()
                .set("background", "white")
                .set("padding", "12px 14px")
                .set("border-radius", "8px")
                .set("box-shadow", "0 8px 20px rgba(15, 23, 42, 0.08)")
                .set("box-sizing", "border-box")
                .set("white-space", "pre-line")
                .set("flex", "1")
                .set("min-height", "0")
                .set("overflow", "auto");

        HorizontalLayout replayControls = new HorizontalLayout(controls);
        replayControls.setWidthFull();
        replayControls.getStyle()
                .set("display", "flex")
                .set("flex-wrap", "nowrap")
                .set("gap", "6px")
                .set("justify-content", "center")
                .set("background", "white")
                .set("padding", "8px")
                .set("border-radius", "8px")
                .set("box-shadow", "0 8px 20px rgba(15, 23, 42, 0.08)")
                .set("box-sizing", "border-box")
                .set("flex-shrink", "0");

        for (Component control : controls) {
            control.getElement().getStyle()
                    .set("min-width", "42px")
                    .set("height", "34px");
        }

        add(historyPanel, replayControls);
    }

    public void render(List<Move> moves) {
        StringBuilder history = new StringBuilder("История ходов:\n");
        for (int i = 0; i < moves.size(); ++i) {
            history.append(i + 1).append(". ").append(describeMove(moves.get(i))).append("\n");
        }
        historyPanel.setText(history.toString());
    }

    private String describeMove(Move move) {
        if (move == null) {
            return "-";
        }
        if (move.getMoveType() == ru.ac.uniyar.model.enums.MoveType.MOVE_PLAYER) {
            return "Игрок P" + move.getPlayerId()
                    + " перешел на клетку "
                    + formatPosition(move.getEndPosition());
        }
        return "Игрок P" + move.getPlayerId()
                + " поставил стену "
                + formatPosition(move.getStartPosition())
                + " - "
                + formatPosition(move.getEndPosition());
    }

    private String formatPosition(ru.ac.uniyar.model.Position position) {
        return "(" + (position.row() + 1) + ", " + (position.col() + 1) + ")";
    }
}
