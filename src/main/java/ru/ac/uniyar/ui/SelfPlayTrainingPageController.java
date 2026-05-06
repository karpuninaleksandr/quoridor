package ru.ac.uniyar.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.router.Route;
import ru.ac.uniyar.model.enums.ComputerAlgorithmType;
import ru.ac.uniyar.model.enums.ComputerPlayerHardnessLevel;
import ru.ac.uniyar.model.enums.GameSize;
import ru.ac.uniyar.service.SelfPlayTrainingResult;
import ru.ac.uniyar.service.SelfPlayTrainingService;

import java.util.Arrays;

@Route("/training")
@CssImport("./styles/training-page.css")
public class SelfPlayTrainingPageController extends VerticalLayout {
    public SelfPlayTrainingPageController(SelfPlayTrainingService trainingService) {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        addClassName("training-page");

        H1 title = new H1("Обучение весов функции оценки");
        title.addClassName("training-page__title");

        ComboBox<String> algorithm1 = algorithmSelector("Алгоритм P1", ComputerAlgorithmType.ALPHABETA);
        ComboBox<String> algorithm2 = algorithmSelector("Алгоритм P2", ComputerAlgorithmType.MONTECARLO);
        ComboBox<String> hardness1 = hardnessSelector("Сложность P1");
        ComboBox<String> hardness2 = hardnessSelector("Сложность P2");

        ComboBox<String> size = new ComboBox<>("Размер поля");
        size.setItems(Arrays.stream(GameSize.values()).map(GameSize::getDescription).toList());
        size.setValue(GameSize.NORMAL.getDescription());
        addTooltip(size, "Размер доски для всех self-play партий.");

        IntegerField games = new IntegerField("Количество партий");
        games.setMin(1);
        games.setMax(200);
        games.setValue(20);
        games.setStepButtonsVisible(true);
        addTooltip(games, "Сколько партий подряд сыграют выбранные ИИ для обновления весов.");

        Pre logs = new Pre();
        logs.addClassName("training-page__logs");

        Button run = new Button("Запустить self-play");
        addTooltip(run, "Запустить серию партий ИИ против ИИ с обучением весов функции оценки.");
        run.addClickListener(event -> {
            if (algorithm1.getValue() == null || algorithm2.getValue() == null
                    || hardness1.getValue() == null || hardness2.getValue() == null
                    || size.getValue() == null || games.getValue() == null) {
                Notification.show("Заполните параметры обучения");
                return;
            }

            UI ui = UI.getCurrent();
            String selectedAlgorithm1 = algorithm1.getValue();
            String selectedAlgorithm2 = algorithm2.getValue();
            String selectedHardness1 = hardness1.getValue();
            String selectedHardness2 = hardness2.getValue();
            GameSize selectedSize = GameSize.findByDescription(size.getValue());
            Integer selectedGames = games.getValue();

            logs.setText("");
            ui.setPollInterval(500);
            run.setEnabled(false);

            new Thread(() -> {
                SelfPlayTrainingResult result = trainingService.train(
                        selectedAlgorithm1,
                        selectedAlgorithm2,
                        selectedHardness1,
                        selectedHardness2,
                        selectedSize,
                        selectedGames,
                        line -> ui.access(() -> logs.setText(logs.getText() + line + "\n"))
                );
                ui.access(() -> {
                    Notification.show("Обучение завершено: обновленных партий " + result.updatedGames());
                    run.setEnabled(true);
                    ui.setPollInterval(-1);
                });
            }, "quoridor-self-play-training").start();
        });

        Button back = new Button("Назад", event -> getUI().ifPresent(ui -> ui.navigate("/start")));
        addTooltip(back, "Вернуться к стартовому меню.");

        HorizontalLayout controls = new HorizontalLayout(algorithm1, algorithm2, hardness1, hardness2, size, games);
        controls.addClassName("training-page__controls");

        HorizontalLayout buttons = new HorizontalLayout(run, back);
        add(title, controls, buttons, logs);
    }

    private ComboBox<String> algorithmSelector(String label, ComputerAlgorithmType defaultValue) {
        ComboBox<String> selector = new ComboBox<>(label);
        selector.setItems(Arrays.stream(ComputerAlgorithmType.values()).map(ComputerAlgorithmType::getDescription).toList());
        selector.setValue(defaultValue.getDescription());
        addTooltip(selector, "Алгоритм участника self-play обучения.");
        return selector;
    }

    private ComboBox<String> hardnessSelector(String label) {
        ComboBox<String> selector = new ComboBox<>(label);
        selector.setItems(Arrays.stream(ComputerPlayerHardnessLevel.values()).map(ComputerPlayerHardnessLevel::getDescription).toList());
        selector.setValue(ComputerPlayerHardnessLevel.MEDIUM.getDescription());
        addTooltip(selector, "Глубина/интенсивность вычислений выбранного ИИ.");
        return selector;
    }

    private void addTooltip(Component component, String text) {
        Tooltip.forComponent(component)
                .withText(text)
                .withHoverDelay(250)
                .withFocusDelay(250);
    }
}
