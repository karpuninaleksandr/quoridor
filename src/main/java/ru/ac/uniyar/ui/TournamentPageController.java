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
import ru.ac.uniyar.service.TournamentService;

import java.util.Arrays;

@Route("/tournament")
public class TournamentPageController extends VerticalLayout {
    public TournamentPageController(TournamentService tournamentService) {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        getStyle()
                .set("background", "#f6f7f9")
                .set("padding", "28px")
                .set("box-sizing", "border-box");

        H1 title = new H1("Турнир ИИ");
        title.getStyle().set("margin", "0 0 8px");
        ComboBox<String> algorithm1 = new ComboBox<>("ИИ P1");
        algorithm1.setItems(Arrays.stream(ComputerAlgorithmType.values()).map(ComputerAlgorithmType::getDescription).toList());
        addTooltip(algorithm1, "Алгоритм первого участника турнира.");

        ComboBox<String> algorithm2 = new ComboBox<>("ИИ P2");
        algorithm2.setItems(Arrays.stream(ComputerAlgorithmType.values()).map(ComputerAlgorithmType::getDescription).toList());
        addTooltip(algorithm2, "Алгоритм второго участника турнира.");

        ComboBox<String> hardness1 = new ComboBox<>("Сложность P1");
        hardness1.setItems(Arrays.stream(ComputerPlayerHardnessLevel.values()).map(ComputerPlayerHardnessLevel::getDescription).toList());
        addTooltip(hardness1, "Глубина/интенсивность вычислений первого ИИ.");

        ComboBox<String> hardness2 = new ComboBox<>("Сложность P2");
        hardness2.setItems(Arrays.stream(ComputerPlayerHardnessLevel.values()).map(ComputerPlayerHardnessLevel::getDescription).toList());
        addTooltip(hardness2, "Глубина/интенсивность вычислений второго ИИ.");

        ComboBox<String> size = new ComboBox<>("Размер поля");
        size.setItems(Arrays.stream(GameSize.values()).map(GameSize::getDescription).toList());
        addTooltip(size, "Размер доски для всех партий турнира.");

        IntegerField games = new IntegerField("Количество партий");
        games.setValue(10);
        games.setMin(1);
        games.setMax(200);
        addTooltip(games, "Сколько партий подряд сыграют выбранные ИИ.");

        Pre result = new Pre();
        result.setWidth("min(900px, 100%)");
        result.getStyle()
                .set("background", "#111827")
                .set("color", "#e5e7eb")
                .set("padding", "18px")
                .set("border-radius", "8px")
                .set("min-height", "280px")
                .set("overflow", "auto")
                .set("box-shadow", "0 12px 28px rgba(15, 23, 42, 0.16)");
        Button run = new Button("Запустить турнир");
        run.addClickListener(e -> {
            if (algorithm1.getValue() == null || algorithm2.getValue() == null
                    || hardness1.getValue() == null || hardness2.getValue() == null
                    || size.getValue() == null || games.getValue() == null) {
                result.setText("Заполни параметры турнира");
                return;
            }

            UI ui = UI.getCurrent();
            String selectedAlgorithm1 = algorithm1.getValue();
            String selectedAlgorithm2 = algorithm2.getValue();
            String selectedHardness1 = hardness1.getValue();
            String selectedHardness2 = hardness2.getValue();
            GameSize selectedSize = GameSize.findByDescription(size.getValue());
            Integer selectedGames = games.getValue();
            ui.setPollInterval(500);
            run.setEnabled(false);
            result.setText("");

            new Thread(() -> {
                String summary = tournamentService.runTournament(
                        selectedAlgorithm1,
                        selectedAlgorithm2,
                        selectedHardness1,
                        selectedHardness2,
                        selectedSize,
                        selectedGames,
                        line -> ui.access(() -> result.setText(result.getText() + line + "\n"))
                );
                ui.access(() -> {
                    result.setText(result.getText() + "\n" + summary);
                    run.setEnabled(true);
                    ui.setPollInterval(-1);
                });
            }, "quoridor-tournament").start();
        });
        Button back = new Button("Назад", e -> getUI().ifPresent(ui -> ui.navigate("start")));
        addTooltip(run, "Запустить серию партий между двумя выбранными алгоритмами.");
        addTooltip(back, "Вернуться к стартовому меню.");

        HorizontalLayout controls = new HorizontalLayout(algorithm1, algorithm2, hardness1, hardness2, size, games);
        controls.setWidth("min(900px, 100%)");
        controls.getStyle()
                .set("background", "white")
                .set("padding", "16px")
                .set("border-radius", "8px")
                .set("box-shadow", "0 8px 20px rgba(15, 23, 42, 0.08)")
                .set("flex-wrap", "wrap");

        HorizontalLayout buttons = new HorizontalLayout(run, back);
        add(title, controls, buttons, result);
    }

    private void addTooltip(Component component, String text) {
        Tooltip.forComponent(component)
                .withText(text)
                .withHoverDelay(250)
                .withFocusDelay(250);
    }
}
