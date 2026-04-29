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
    private final Div actionsPanel = new Div();

    public GameInfoPanel() {
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

        styleInfoPanel(hintPanel);
        hintPanel.getStyle()
                .set("height", "164px")
                .set("min-height", "164px")
                .set("overflow", "auto");
        styleInfoPanel(reportPanel);
        reportPanel.getStyle()
                .set("height", "250px")
                .set("min-height", "250px")
                .set("overflow", "auto");
        styleInfoPanel(statisticsPanel);
        statisticsPanel.getStyle()
                .set("flex", "1")
                .set("min-height", "120px")
                .set("overflow", "auto");
        styleInfoPanel(actionsPanel);
        actionsPanel.getStyle().set("display", "none");

        styleInfoPanel(turnPanel);
        styleInfoPanel(wallsPanel);

        add(turnPanel, wallsPanel, hintPanel, reportPanel, statisticsPanel, actionsPanel);
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

    public void setWallsCounts(int walls1, int walls2) {
        wallsPanel.removeAll();
        wallsPanel.add(createWallCounter("P1", walls1, "#2563eb"), createWallCounter("P2", walls2, "#dc2626"));
        wallsPanel.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "1fr 1fr")
                .set("gap", "10px");
    }

    public void setHintLegend(List<AlgorithmReport> reports, Function<String, String> colorProvider) {
        hintPanel.removeAll();
        hintPanel.getStyle().set("display", "grid").set("gap", "8px");
        addAlgorithmColorRow("Случайный", colorProvider.apply("Случайный"));
        addAlgorithmColorRow("MiniMax", colorProvider.apply("MiniMax"));
        addAlgorithmColorRow("AlphaBeta", colorProvider.apply("AlphaBeta"));
        addAlgorithmColorRow("MonteCarlo", colorProvider.apply("MonteCarlo"));
        if (reports.isEmpty()) {
            return;
        }

        Div separator = new Div();
        separator.getStyle()
                .set("height", "1px")
                .set("background", "#e5e7eb")
                .set("margin", "2px 0");
        hintPanel.add(separator);

        for (AlgorithmReport report : reports) {
            addAlgorithmColorRow(report.algorithm(), colorProvider.apply(report.algorithm()));
        }
    }

    public void setReport(AlgorithmReport report, String moveDescription) {
        reportPanel.removeAll();
        reportPanel.getStyle()
                .set("display", "grid")
                .set("gap", "10px")
                .set("height", "250px")
                .set("min-height", "250px");

        if (report == null) {
            return;
        }

        Div heading = new Div();
        heading.setText(report.algorithm() + ": " + moveDescription);
        heading.getStyle()
                .set("font-weight", "700")
                .set("color", "#111827");

        Div metrics = new Div();
        metrics.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "1fr 1fr")
                .set("gap", "6px");
        metrics.add(
                createMetric("Оценка", String.valueOf(report.score())),
                createMetric("Время", report.timeMs() + " мс"),
                createMetric("Глубина", String.valueOf(report.reachedDepth())),
                createMetric("Узлы", String.valueOf(report.nodesVisited())),
                createMetric("Кандидаты", String.valueOf(report.consideredMoves())),
                createMetric("Отсечения", String.valueOf(report.cutoffs())),
                createMetric("TT hits", String.valueOf(report.tableHits()))
        );

        Div explanation = new Div();
        explanation.setText(report.explanation());
        explanation.getStyle()
                .set("color", "#475569")
                .set("line-height", "1.35")
                .set("font-size", "13px");

        reportPanel.add(heading, metrics, explanation);
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

    private void addAlgorithmColorRow(String algorithm, String color) {
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
                .set("background", color)
                .set("box-shadow", "inset 0 0 0 1px rgba(15, 23, 42, 0.25)");

        Div name = new Div();
        name.setText(algorithm);
        name.getStyle().set("font-weight", "600");
        row.add(swatch, name);
        hintPanel.add(row);
    }

    private Div createMetric(String label, String value) {
        Div metric = new Div();
        metric.getStyle()
                .set("background", "#f8fafc")
                .set("border", "1px solid #e5e7eb")
                .set("border-radius", "8px")
                .set("padding", "7px 8px");

        Div name = new Div();
        name.setText(label);
        name.getStyle()
                .set("font-size", "11px")
                .set("color", "#64748b");

        Div number = new Div();
        number.setText(value);
        number.getStyle()
                .set("font-weight", "700")
                .set("font-size", "13px")
                .set("color", "#111827");

        metric.add(name, number);
        return metric;
    }
}
