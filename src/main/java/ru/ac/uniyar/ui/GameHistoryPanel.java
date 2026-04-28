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
                .set("width", "300px")
                .set("min-width", "260px");

        historyPanel.setWidthFull();
        historyPanel.getStyle()
                .set("background", "white")
                .set("padding", "12px 14px")
                .set("border-radius", "8px")
                .set("box-shadow", "0 8px 20px rgba(15, 23, 42, 0.08)")
                .set("box-sizing", "border-box")
                .set("white-space", "pre-line")
                .set("height", "560px")
                .set("max-height", "560px")
                .set("overflow", "auto");

        HorizontalLayout replayControls = new HorizontalLayout(controls);
        replayControls.getStyle().set("flex-wrap", "wrap");

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
            return "P" + move.getPlayerId() + " -> " + move.getEndPosition();
        }
        return "P" + move.getPlayerId() + " стена " + move.getStartPosition() + "-" + move.getEndPosition();
    }
}
