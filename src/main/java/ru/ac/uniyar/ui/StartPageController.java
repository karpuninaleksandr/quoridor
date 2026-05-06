package ru.ac.uniyar.ui;

import ru.ac.uniyar.model.enums.ComputerAlgorithmType;
import ru.ac.uniyar.model.enums.ComputerPlayerHardnessLevel;
import ru.ac.uniyar.model.enums.GameSize;
import ru.ac.uniyar.model.algorithms.AlgorithmProfile;
import ru.ac.uniyar.service.GameProcessor;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.dependency.CssImport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Route("/start")
@CssImport("./styles/start-page.css")
public class StartPageController extends VerticalLayout {
    public StartPageController(GameProcessor gameProcessor) {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        addClassName("start-page");

        H1 title = new H1("Игра «Коридор»");
        title.addClassName("start-page__title");

        ComboBox<String> sizeSelector = new ComboBox<>("Размер поля");
        sizeSelector.setItems(Arrays.stream(GameSize.values()).map(GameSize::getDescription).toList());
        sizeSelector.setPlaceholder("Выберите размер");
        sizeSelector.setValue(GameSize.NORMAL.getDescription());
        addTooltip(sizeSelector, "Размер доски влияет на длину партии, число стен и сложность поиска.");

        ComboBox<String> player1 = new ComboBox<>("Выберите первого игрока");
        List<String> player1s = new ArrayList<>();
        player1s.add("Игрок");
        player1s.addAll(Arrays.stream(ComputerAlgorithmType.values()).map(ComputerAlgorithmType::getDescription).toList());
        player1.setItems(player1s);
        player1.setValue("Игрок");
        addTooltip(player1, "P1 может быть человеком или ИИ. Если выбран ИИ, ниже появится отдельная сложность.");

        ComboBox<String> player2 = new ComboBox<>("Выберите второго игрока");
        player2.setItems(Arrays.stream(ComputerAlgorithmType.values()).map(ComputerAlgorithmType::getDescription).toList());
        player2.setValue(ComputerAlgorithmType.ALPHABETA.getDescription());
        addTooltip(player2, "P2 всегда управляется ИИ: выбери алгоритм, против которого будет играть человек или другой ИИ.");

        ComboBox<String> hardness1 = new ComboBox<>("Сложность P1");
        hardness1.setItems(Arrays.stream(ComputerPlayerHardnessLevel.values()).map(ComputerPlayerHardnessLevel::getDescription).toList());
        addTooltip(hardness1, "Сложность P1 используется только когда первый игрок управляется алгоритмом.");
        hardness1.setVisible(false);

        ComboBox<String> hardness2 = new ComboBox<>("Сложность P2");
        hardness2.setItems(Arrays.stream(ComputerPlayerHardnessLevel.values()).map(ComputerPlayerHardnessLevel::getDescription).toList());
        hardness2.setValue(ComputerPlayerHardnessLevel.MEDIUM.getDescription());
        addTooltip(hardness2, "Сложность второго ИИ. Эта же сложность используется для подсказок в игре против ИИ.");

        Div player1Profile = createProfileBlock("Профиль P1", AlgorithmProfile.describe(null));
        Div player2Profile = createProfileBlock("Профиль P2", AlgorithmProfile.describe(null));

        player1.addValueChangeListener(event -> {
            boolean firstPlayerIsAi = event.getValue() != null && !"Игрок".equals(event.getValue());
            hardness1.setVisible(firstPlayerIsAi);
            player1Profile.setText(firstPlayerIsAi
                    ? "Профиль P1: " + AlgorithmProfile.describe(event.getValue())
                    : "Профиль P1: человек управляет ходами вручную.");
            if (!firstPlayerIsAi) {
                hardness1.clear();
            }
        });
        player2.addValueChangeListener(event ->
                player2Profile.setText("Профиль P2: " + AlgorithmProfile.describe(event.getValue())));
        player1Profile.setText("Профиль P1: человек управляет ходами вручную.");
        player2Profile.setText("Профиль P2: " + AlgorithmProfile.describe(player2.getValue()));

        Dialog rulesDialog = createRulesDialog();

        Button startButton = new Button("Начать игру", event -> {
            String size = sizeSelector.getValue();
            if (size == null) {
                Notification.show("Выберите размер поля");
                return;
            }
            if (player1.getValue() == null || player2.getValue() == null) {
                Notification.show("Выберите обоих игроков");
                return;
            }
            if (!"Игрок".equals(player1.getValue()) && hardness1.getValue() == null) {
                Notification.show("Выберите сложность P1");
                return;
            }
            if (hardness2.getValue() == null) {
                Notification.show("Выберите сложность P2");
                return;
            }

            gameProcessor.startNewGame(size, player1.getValue(), player2.getValue(), hardness1.getValue(), hardness2.getValue());
            getUI().ifPresent(ui -> ui.navigate("/game"));
        });
        Button rulesButton = new Button("Правила игры", event -> rulesDialog.open());
        Button tournamentButton = new Button("Турнир ИИ", event -> getUI().ifPresent(ui -> ui.navigate("/tournament")));
        Button comparisonButton = new Button("Сравнение алгоритмов", event -> getUI().ifPresent(ui -> ui.navigate("/comparison")));
        Button trainingButton = new Button("Обучение весов", event -> getUI().ifPresent(ui -> ui.navigate("/training")));
        addTooltip(startButton, "Обычная партия: человек против ИИ или пошаговый режим ИИ против ИИ.");
        addTooltip(rulesButton, "Открыть краткое описание правил игры.");
        addTooltip(tournamentButton, "Турнир: серия партий между двумя выбранными алгоритмами с логами и итоговой статистикой.");
        addTooltip(comparisonButton, "Сравнение: матчи всех разных пар алгоритмов без зеркальных повторов.");
        addTooltip(trainingButton, "Self-play: ИИ играет партии сам с собой и обновляет веса функции оценки по победителю.");

        Div panel = new Div(sizeSelector, player1, player1Profile, player2, player2Profile, hardness1, hardness2);
        panel.addClassName("start-page__panel");

        HorizontalLayout buttons = new HorizontalLayout(startButton, rulesButton, tournamentButton, comparisonButton, trainingButton);
        buttons.setJustifyContentMode(JustifyContentMode.CENTER);
        buttons.addClassName("start-page__buttons");

        Div menuColumn = new Div(panel, buttons);
        menuColumn.addClassName("start-page__menu");

        HorizontalLayout content = new HorizontalLayout(menuColumn);
        content.setAlignItems(Alignment.STRETCH);
        content.setJustifyContentMode(JustifyContentMode.CENTER);
        content.addClassName("start-page__content");

        add(title, content);
    }

    private void addTooltip(Component component, String text) {
        Tooltip.forComponent(component)
                .withText(text)
                .withHoverDelay(250)
                .withFocusDelay(250);
    }

    private Div createProfileBlock(String title, String text) {
        Div profile = new Div();
        profile.setText(title + ": " + text);
        profile.addClassName("start-page__profile");
        return profile;
    }

    private Dialog createRulesDialog() {
        Dialog dialog = new Dialog();
        dialog.addClassName("start-page__rules-dialog");

        Div card = new Div();
        card.addClassName("start-page__rules");

        Div title = new Div();
        title.setText("Правила игры");
        title.addClassName("start-page__rules-title");

        Button close = new Button("Закрыть", event -> dialog.close());
        close.addClassName("start-page__rules-close");

        card.add(
                title,
                createRuleItem("Цель", "первым дойти своей фишкой до противоположной стороны поля."),
                createRuleItem("Ход", "можно либо передвинуть фишку, либо поставить одну стену."),
                createRuleItem("Фишка", "ходит на соседнюю свободную клетку по вертикали или горизонтали."),
                createRuleItem("Прыжок", "если рядом стоит соперник, можно перепрыгнуть через него; если прямо нельзя, разрешен диагональный обход."),
                createRuleItem("Стена", "занимает два промежутка между клетками и блокирует проход в этом месте."),
                createRuleItem("Ограничение", "стену нельзя поставить так, чтобы у любого игрока пропал путь к финишу."),
                createRuleItem("Победа", "партия заканчивается сразу, когда фишка достигает целевой строки."),
                close
        );

        dialog.add(card);
        return dialog;
    }

    private Div createRuleItem(String label, String text) {
        Div item = new Div();
        item.setText(label + ": " + text);
        item.addClassName("start-page__rule-item");
        return item;
    }
}
