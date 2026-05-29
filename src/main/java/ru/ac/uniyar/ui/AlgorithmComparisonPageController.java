package ru.ac.uniyar.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.router.Route;
import ru.ac.uniyar.model.enums.ComputerAlgorithmType;
import ru.ac.uniyar.model.enums.ComputerPlayerHardnessLevel;
import ru.ac.uniyar.model.enums.GameSize;
import ru.ac.uniyar.service.AlgorithmComparisonResult;
import ru.ac.uniyar.service.TournamentService;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Route("/comparison")
@CssImport("./styles/comparison-page.css")
public class AlgorithmComparisonPageController extends VerticalLayout {
    public AlgorithmComparisonPageController(TournamentService tournamentService) {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        addClassName("comparison-page");

        H1 title = new H1("Сравнение алгоритмов");
        title.addClassName("comparison-page__title");

        ComboBox<String> randomHardness = createHardnessSelector("Сложность Random");
        ComboBox<String> minimaxHardness = createHardnessSelector("Сложность MiniMax");
        ComboBox<String> monteCarloHardness = createHardnessSelector("Сложность MonteCarlo");
        ComboBox<String> alphaBetaHardness = createHardnessSelector("Сложность AlphaBeta");
        addTooltip(randomHardness, "Случайный алгоритм: выбирает допустимый ход случайно, нужен как базовая линия.");
        addTooltip(minimaxHardness, "MiniMax: просматривает дерево ходов и выбирает позицию с лучшей оценкой.");
        addTooltip(monteCarloHardness, "MonteCarlo: оценивает ходы через случайные доигрывания.");
        addTooltip(alphaBetaHardness, "AlphaBeta: MiniMax с отсечениями бесперспективных веток.");


        ComboBox<String> size = new ComboBox<>("Размер поля");
        size.setItems(Arrays.stream(GameSize.values()).map(GameSize::getDescription).toList());
        addTooltip(size, "Размер доски для всех матчей сравнения.");

        IntegerField games = new IntegerField("Партий на пару");
        games.setValue(2);
        games.setMin(1);
        games.setMax(50);
        addTooltip(games, "Сколько партий сыграет каждая пара разных алгоритмов.");

        Pre result = new Pre();
        result.addClassName("comparison-page__logs");

        Button run = new Button("Сравнить");
        run.addClickListener(event -> {
            if (randomHardness.getValue() == null || minimaxHardness.getValue() == null
                    || monteCarloHardness.getValue() == null || alphaBetaHardness.getValue() == null
                    || size.getValue() == null || games.getValue() == null) {
                result.setText("Заполни параметры сравнения");
                return;
            }

            UI ui = UI.getCurrent();
            Map<String, String> hardnessByAlgorithm = new LinkedHashMap<>();
            hardnessByAlgorithm.put(ComputerAlgorithmType.RANDOM.getDescription(), randomHardness.getValue());
            hardnessByAlgorithm.put(ComputerAlgorithmType.MINIMAX.getDescription(), minimaxHardness.getValue());
            hardnessByAlgorithm.put(ComputerAlgorithmType.MONTECARLO.getDescription(), monteCarloHardness.getValue());
            hardnessByAlgorithm.put(ComputerAlgorithmType.ALPHABETA.getDescription(), alphaBetaHardness.getValue());
            GameSize selectedSize = GameSize.findByDescription(size.getValue());
            int selectedGames = games.getValue();
            ui.setPollInterval(500);
            run.setEnabled(false);
            result.setText("");

            new Thread(() -> {
                List<AlgorithmComparisonResult> rows = tournamentService.compareAlgorithms(
                        hardnessByAlgorithm,
                        selectedSize,
                        selectedGames,
                        line -> ui.access(() -> result.setText(result.getText() + line + "\n"))
                );
                String table = buildTable(rows);
                ui.access(() -> {
                    result.setText(result.getText() + "\n" + table);
                    run.setEnabled(true);
                    ui.setPollInterval(-1);
                });
            }, "quoridor-comparison").start();
        });

        Button compareWeights = new Button("Сравнить веса AlphaBeta");
        compareWeights.addClickListener(event -> {
            if (games.getValue() == null) {
                result.setText("Укажи количество партий");
                return;
            }

            UI ui = UI.getCurrent();
            int selectedGames = games.getValue();
            ui.setPollInterval(500);
            run.setEnabled(false);
            compareWeights.setEnabled(false);
            result.setText("");

            new Thread(() -> {
                tournamentService.compareAlphaBetaWeights(
                        selectedGames,
                        line -> ui.access(() -> result.setText(result.getText() + line + "\n"))
                );
                ui.access(() -> {
                    run.setEnabled(true);
                    compareWeights.setEnabled(true);
                    ui.setPollInterval(-1);
                });
            }, "quoridor-weights-comparison").start();
        });

        Button back = new Button("Назад", e -> getUI().ifPresent(ui -> ui.navigate("start")));
        addTooltip(run, "Запустить матчи всех разных пар алгоритмов без зеркальных повторов.");
        addTooltip(compareWeights, "Запустить AlphaBeta HARD на поле 11 на 11: начальные веса против обновленных.");
        addTooltip(back, "Вернуться к стартовому меню.");

        HorizontalLayout controls = new HorizontalLayout(
                randomHardness,
                minimaxHardness,
                monteCarloHardness,
                alphaBetaHardness,
                size,
                games
        );
        controls.addClassName("comparison-page__controls");

        add(title, controls, new HorizontalLayout(run, compareWeights, back), result);
    }

    private String buildTable(List<AlgorithmComparisonResult> rows) {
        StringBuilder table = new StringBuilder();
        table.append("Итог по алгоритмам\n");
        table.append("Алгоритм           | Партий | Победы | Поражения | Ничьи | Winrate | Очки/партия | Ср. ходов | Ходов ИИ | Ср. мс | Ср. глуб. | Ср. узлы | Ср. отсеч. | Ср. TT\n");
        table.append("-------------------|--------|--------|-----------|-------|---------|-------------|----------|----------|--------|-----------|----------|------------|-------\n");
        Map<String, AlgorithmSummary> summaries = buildSummaries(rows);
        for (Map.Entry<String, AlgorithmSummary> entry : summaries.entrySet()) {
            AlgorithmSummary summary = entry.getValue();
            table.append(String.format("%-18s | %6d | %6d | %9d | %5d | %6.2f%% | %11.2f | %8.2f | %8d | %6.1f | %9.2f | %8.1f | %10.1f | %.1f%n",
                    entry.getKey(),
                    summary.games,
                    summary.wins,
                    summary.losses,
                    summary.draws,
                    summary.winrate() * 100,
                    summary.pointsPerGame(),
                    summary.averageMoves(),
                    summary.metricReports,
                    summary.averageTimeMs(),
                    summary.averageDepth(),
                    summary.averageNodes(),
                    summary.averageCutoffs(),
                    summary.averageTableHits()));
        }

        table.append("\nПопарные результаты\n");
        table.append("Алгоритм P1        | Алгоритм P2        | Игры | Победы P1 | Победы P2 | Ничьи | Ср. ходов\n");
        table.append("-------------------|--------------------|------|-----------|-----------|-------|----------\n");
        for (AlgorithmComparisonResult row : rows) {
            table.append(String.format("%-18s | %-18s | %4d | %9d | %9d | %5d | %8.2f%n",
                    row.algorithm1(),
                    row.algorithm2(),
                    row.games(),
                    row.wins1(),
                    row.wins2(),
                    row.draws(),
                    row.averageMoves()));
        }

        table.append("\nВычислительные метрики по ходам алгоритмов\n");
        table.append("Пара               | Алгоритм           | Ходов ИИ | Ср. мс | Ср. глуб. | Ср. узлы | Ср. отсеч. | Ср. TT\n");
        table.append("-------------------|--------------------|----------|--------|-----------|----------|------------|-------\n");
        for (AlgorithmComparisonResult row : rows) {
            appendPairMetrics(table, row.algorithm1() + " vs " + row.algorithm2(), row.algorithm1(),
                    row.reports1(), row.averageTimeMs1(), row.averageDepth1(), row.averageNodes1(),
                    row.averageCutoffs1(), row.averageTableHits1());
            appendPairMetrics(table, row.algorithm1() + " vs " + row.algorithm2(), row.algorithm2(),
                    row.reports2(), row.averageTimeMs2(), row.averageDepth2(), row.averageNodes2(),
                    row.averageCutoffs2(), row.averageTableHits2());
        }
        return table.toString();
    }

    private void appendPairMetrics(StringBuilder table, String pair, String algorithm, long reports,
                                   double timeMs, double depth, double nodes, double cutoffs, double tableHits) {
        table.append(String.format("%-18s | %-18s | %8d | %6.1f | %9.2f | %8.1f | %10.1f | %.1f%n",
                pair,
                algorithm,
                reports,
                timeMs,
                depth,
                nodes,
                cutoffs,
                tableHits));
    }

    private ComboBox<String> createHardnessSelector(String label) {
        ComboBox<String> selector = new ComboBox<>(label);
        selector.setItems(Arrays.stream(ComputerPlayerHardnessLevel.values()).map(ComputerPlayerHardnessLevel::getDescription).toList());
        return selector;
    }

    private void addTooltip(Component component, String text) {
        Tooltip.forComponent(component)
                .withText(text)
                .withHoverDelay(250)
                .withFocusDelay(250);
    }

    private Map<String, AlgorithmSummary> buildSummaries(List<AlgorithmComparisonResult> rows) {
        Map<String, AlgorithmSummary> summaries = new LinkedHashMap<>();
        for (ComputerAlgorithmType type : ComputerAlgorithmType.values()) {
            summaries.put(type.getDescription(), new AlgorithmSummary());
        }

        for (AlgorithmComparisonResult row : rows) {
            AlgorithmSummary first = summaries.get(row.algorithm1());
            AlgorithmSummary second = summaries.get(row.algorithm2());

            first.add(row);
            second.add(row.reversed());
        }
        return summaries;
    }

    private static class AlgorithmSummary {
        int games;
        int wins;
        int losses;
        int draws;
        double totalMoves;
        double totalTimeMs;
        double totalDepth;
        double totalNodes;
        double totalCutoffs;
        double totalTableHits;
        long metricReports;

        void add(AlgorithmComparisonResult row) {
            this.games += row.games();
            this.wins += row.wins1();
            this.losses += row.wins2();
            this.draws += row.draws();
            this.totalMoves += row.averageMoves() * row.games();
            this.metricReports += row.reports1();
            this.totalTimeMs += row.averageTimeMs1() * row.reports1();
            this.totalDepth += row.averageDepth1() * row.reports1();
            this.totalNodes += row.averageNodes1() * row.reports1();
            this.totalCutoffs += row.averageCutoffs1() * row.reports1();
            this.totalTableHits += row.averageTableHits1() * row.reports1();
        }

        double winrate() {
            return games == 0 ? 0 : wins * 1.0 / games;
        }

        double pointsPerGame() {
            return games == 0 ? 0 : (wins + draws * 0.5) / games;
        }

        double averageMoves() {
            return games == 0 ? 0 : totalMoves / games;
        }

        double averageTimeMs() {
            return metricReports == 0 ? 0 : totalTimeMs / metricReports;
        }

        double averageDepth() {
            return metricReports == 0 ? 0 : totalDepth / metricReports;
        }

        double averageNodes() {
            return metricReports == 0 ? 0 : totalNodes / metricReports;
        }

        double averageCutoffs() {
            return metricReports == 0 ? 0 : totalCutoffs / metricReports;
        }

        double averageTableHits() {
            return metricReports == 0 ? 0 : totalTableHits / metricReports;
        }
    }
}
