package ru.ac.uniyar.ui;

import ru.ac.uniyar.model.enums.ComputerAlgorithmType;
import ru.ac.uniyar.model.enums.ComputerPlayerHardnessLevel;
import ru.ac.uniyar.model.enums.GameSize;
import ru.ac.uniyar.service.GameProcessor;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.router.Route;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Route("/start")
public class StartPageController extends VerticalLayout {
    public StartPageController(GameProcessor gameProcessor) {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        getStyle()
                .set("background", "#f6f7f9")
                .set("padding", "24px")
                .set("box-sizing", "border-box");

        H1 title = new H1("Игра «Коридор»");
        title.getStyle().set("margin", "0 0 18px");

        ComboBox<String> sizeSelector = new ComboBox<>("Размер поля");
        sizeSelector.setItems(Arrays.stream(GameSize.values()).map(GameSize::getDescription).toList());
        sizeSelector.setPlaceholder("Выберите размер");
        addTooltip(sizeSelector, "Размер доски влияет на длину партии, число стен и сложность поиска.");

        ComboBox<String> player1 = new ComboBox<>("Выберите первого игрока");
        List<String> player1s = new ArrayList<>();
        player1s.add("Игрок");
        player1s.addAll(Arrays.stream(ComputerAlgorithmType.values()).map(ComputerAlgorithmType::getDescription).toList());
        player1.setItems(player1s);
        addTooltip(player1, "P1 может быть человеком или ИИ. Если выбран ИИ, ниже появится отдельная сложность.");

        ComboBox<String> player2 = new ComboBox<>("Выберите второго игрока");
        player2.setItems(Arrays.stream(ComputerAlgorithmType.values()).map(ComputerAlgorithmType::getDescription).toList());
        addTooltip(player2, "P2 всегда управляется ИИ: выбери алгоритм, против которого будет играть человек или другой ИИ.");

        ComboBox<String> hardness1 = new ComboBox<>("Сложность P1");
        hardness1.setItems(Arrays.stream(ComputerPlayerHardnessLevel.values()).map(ComputerPlayerHardnessLevel::getDescription).toList());
        addTooltip(hardness1, "Сложность P1 используется только когда первый игрок управляется алгоритмом.");
        hardness1.setVisible(false);

        ComboBox<String> hardness2 = new ComboBox<>("Сложность P2");
        hardness2.setItems(Arrays.stream(ComputerPlayerHardnessLevel.values()).map(ComputerPlayerHardnessLevel::getDescription).toList());
        addTooltip(hardness2, "Сложность второго ИИ. Эта же сложность используется для подсказок в игре против ИИ.");

        player1.addValueChangeListener(event -> {
            boolean firstPlayerIsAi = event.getValue() != null && !"Игрок".equals(event.getValue());
            hardness1.setVisible(firstPlayerIsAi);
            if (!firstPlayerIsAi) {
                hardness1.clear();
            }
        });

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
        Button tournamentButton = new Button("Турнир ИИ", event -> getUI().ifPresent(ui -> ui.navigate("/tournament")));
        Button comparisonButton = new Button("Сравнение алгоритмов", event -> getUI().ifPresent(ui -> ui.navigate("/comparison")));
        addTooltip(startButton, "Обычная партия: человек против ИИ или пошаговый режим ИИ против ИИ.");
        addTooltip(tournamentButton, "Турнир: серия партий между двумя выбранными алгоритмами с логами и итоговой статистикой.");
        addTooltip(comparisonButton, "Сравнение: матчи всех разных пар алгоритмов без зеркальных повторов.");

        Div panel = new Div(sizeSelector, player1, player2, hardness1, hardness2);
        panel.getStyle()
                .set("display", "grid")
                .set("gap", "12px")
                .set("width", "100%")
                .set("background", "white")
                .set("padding", "22px")
                .set("border-radius", "8px")
                .set("box-shadow", "0 12px 28px rgba(15, 23, 42, 0.12)");

        HorizontalLayout buttons = new HorizontalLayout(startButton, tournamentButton, comparisonButton);
        buttons.setJustifyContentMode(JustifyContentMode.CENTER);
        buttons.getStyle().set("flex-wrap", "wrap");

        Div menuColumn = new Div(panel, buttons);
        menuColumn.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "14px")
                .set("width", "min(420px, 100%)");

        add(title, menuColumn);
    }

    private void addTooltip(Component component, String text) {
        Tooltip.forComponent(component)
                .withText(text)
                .withHoverDelay(250)
                .withFocusDelay(250);
    }
}
