package ru.ac.uniyar.ui;

import com.vaadin.flow.component.html.Div;
import ru.ac.uniyar.model.algorithms.AlgorithmReport;

import java.util.List;
import java.util.function.Function;

public class GameInfoPanel extends Div {
    private final Div turnPanel = new Div();
    private final Div wallsPanel = new Div();
    private final Div hintPanel = new Div();
    private final Div reportPanel = new Div();
    private final Div statisticsPanel = new Div();

    public GameInfoPanel() {
        getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "12px")
                .set("width", "300px")
                .set("min-width", "260px");

        styleInfoPanel(hintPanel);
        styleInfoPanel(reportPanel);
        styleInfoPanel(statisticsPanel);

        styleInfoPanel(turnPanel);
        styleInfoPanel(wallsPanel);

        add(turnPanel, wallsPanel, hintPanel, reportPanel, statisticsPanel);
    }

    public void setTurn(String text) {
        setTurnStatus(text, 0);
    }

    public void setTurnStatus(String text, int playerId) {
        turnPanel.removeAll();
        Div label = new Div();
        label.setText(text);
        label.getStyle()
                .set("font-size", "18px")
                .set("font-weight", "700");
        if (playerId == 1 || playerId == 2) {
            Div badge = new Div();
            badge.setText("P" + playerId);
            badge.getStyle()
                    .set("width", "34px")
                    .set("height", "34px")
                    .set("border-radius", "50%")
                    .set("background", playerId == 1 ? "#2563eb" : "#dc2626")
                    .set("color", "white")
                    .set("display", "flex")
                    .set("align-items", "center")
                    .set("justify-content", "center")
                    .set("font-weight", "700");
            turnPanel.add(badge, label);
        } else {
            turnPanel.add(label);
        }
        turnPanel.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("gap", "10px");
    }

    public void setWalls(String text) {
        wallsPanel.setText(text);
    }

    public void setWallsCounts(int walls1, int walls2) {
        wallsPanel.removeAll();
        wallsPanel.add(createWallCounter("P1", walls1, "#2563eb"), createWallCounter("P2", walls2, "#dc2626"));
        wallsPanel.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "1fr 1fr")
                .set("gap", "10px");
    }

    public void setHint(String text) {
        hintPanel.getStyle().set("white-space", "pre-line");
        hintPanel.setText(text);
    }

    public void setHintLegend(List<AlgorithmReport> reports, Function<String, String> colorProvider) {
        hintPanel.removeAll();
        hintPanel.getStyle()
                .set("display", reports.isEmpty() ? "none" : "grid")
                .set("gap", "8px");
        for (AlgorithmReport report : reports) {
            Div row = new Div();
            row.getStyle()
                    .set("display", "flex")
                    .set("align-items", "center")
                    .set("gap", "8px");

            Div swatch = new Div();
            swatch.getStyle()
                    .set("width", "18px")
                    .set("height", "18px")
                    .set("border-radius", "4px")
                    .set("background", colorProvider.apply(report.algorithm()))
                    .set("box-shadow", "inset 0 0 0 1px rgba(15, 23, 42, 0.25)");

            Div name = new Div();
            name.setText(report.algorithm());
            name.getStyle().set("font-weight", "600");
            row.add(swatch, name);
            hintPanel.add(row);
        }
    }

    public void setReport(String text) {
        reportPanel.setText(text);
    }

    public void setStatistics(String text) {
        statisticsPanel.getStyle().set("white-space", "pre-line");
        statisticsPanel.setText(text);
    }

    private void styleInfoPanel(Div panel) {
        panel.setWidthFull();
        panel.getStyle()
                .set("background", "white")
                .set("padding", "12px 14px")
                .set("border-radius", "8px")
                .set("box-shadow", "0 8px 20px rgba(15, 23, 42, 0.08)")
                .set("box-sizing", "border-box");
    }

    private Div createWallCounter(String player, int walls, String color) {
        Div counter = new Div();
        counter.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("gap", "8px")
                .set("padding", "8px")
                .set("border-radius", "8px")
                .set("background", "#f8fafc");

        Div marker = new Div();
        marker.getStyle()
                .set("width", "10px")
                .set("height", "30px")
                .set("border-radius", "3px")
                .set("background", color);

        Div text = new Div();
        text.setText(player + ": " + walls);
        text.getStyle()
                .set("font-size", "16px")
                .set("font-weight", "700");

        counter.add(marker, text);
        return counter;
    }
}
