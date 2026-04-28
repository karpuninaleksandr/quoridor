package ru.ac.uniyar.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
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
public class AlgorithmComparisonPageController extends VerticalLayout {
    public AlgorithmComparisonPageController(TournamentService tournamentService) {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        getStyle()
                .set("background", "#f6f7f9")
                .set("padding", "28px")
                .set("box-sizing", "border-box");

        H1 title = new H1("Сравнение алгоритмов");
        title.getStyle().set("margin", "0 0 8px");

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
        result.setWidth("min(1100px, 100%)");
        result.getStyle()
                .set("background", "#111827")
                .set("color", "#e5e7eb")
                .set("padding", "18px")
                .set("border-radius", "8px")
                .set("min-height", "360px")
                .set("overflow", "auto")
                .set("box-shadow", "0 12px 28px rgba(15, 23, 42, 0.16)");

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

        Button back = new Button("Назад", e -> getUI().ifPresent(ui -> ui.navigate("start")));
        addTooltip(run, "Запустить матчи всех разных пар алгоритмов без зеркальных повторов.");
        addTooltip(back, "Вернуться к стартовому меню.");

        HorizontalLayout controls = new HorizontalLayout(
                randomHardness,
                minimaxHardness,
                monteCarloHardness,
                alphaBetaHardness,
                size,
                games
        );
        controls.setWidth("min(1100px, 100%)");
        controls.getStyle()
                .set("background", "white")
                .set("padding", "16px")
                .set("border-radius", "8px")
                .set("box-shadow", "0 8px 20px rgba(15, 23, 42, 0.08)")
                .set("flex-wrap", "wrap");

        add(title, controls, new HorizontalLayout(run, back), result);
    }

    private String buildTable(List<AlgorithmComparisonResult> rows) {
        StringBuilder table = new StringBuilder();
        table.append("Итог по алгоритмам\n");
        table.append("Алгоритм           | Партий | Победы | Поражения | Ничьи | Winrate | Очки/партия | Ср. ходов\n");
        table.append("-------------------|--------|--------|-----------|-------|---------|-------------|----------\n");
        Map<String, AlgorithmSummary> summaries = buildSummaries(rows);
        for (Map.Entry<String, AlgorithmSummary> entry : summaries.entrySet()) {
            AlgorithmSummary summary = entry.getValue();
            table.append(String.format("%-18s | %6d | %6d | %9d | %5d | %6.2f%% | %11.2f | %.2f%n",
                    entry.getKey(),
                    summary.games,
                    summary.wins,
                    summary.losses,
                    summary.draws,
                    summary.winrate() * 100,
                    summary.pointsPerGame(),
                    summary.averageMoves()));
        }

        table.append("\nПопарные результаты\n");
        table.append("Алгоритм P1        | Алгоритм P2        | Игры | Победы P1 | Победы P2 | Ничьи | Ср. ходов\n");
        table.append("-------------------|--------------------|------|-----------|-----------|-------|----------\n");
        for (AlgorithmComparisonResult row : rows) {
            table.append(String.format("%-18s | %-18s | %4d | %9d | %9d | %5d | %.2f%n",
                    row.algorithm1(),
                    row.algorithm2(),
                    row.games(),
                    row.wins1(),
                    row.wins2(),
                    row.draws(),
                    row.averageMoves()));
        }
        return table.toString();
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

            first.add(row.games(), row.wins1(), row.wins2(), row.draws(), row.averageMoves());
            second.add(row.games(), row.wins2(), row.wins1(), row.draws(), row.averageMoves());
        }
        return summaries;
    }

    private static class AlgorithmSummary {
        int games;
        int wins;
        int losses;
        int draws;
        double totalMoves;

        void add(int games, int wins, int losses, int draws, double averageMoves) {
            this.games += games;
            this.wins += wins;
            this.losses += losses;
            this.draws += draws;
            this.totalMoves += averageMoves * games;
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
    }
}
