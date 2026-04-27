package ru.ac.uniyar.ui;

import ru.ac.uniyar.model.enums.ComputerAlgorithmType;
import ru.ac.uniyar.model.enums.ComputerPlayerHardnessLevel;
import ru.ac.uniyar.model.enums.GameSize;
import ru.ac.uniyar.service.GameProcessor;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
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

        H1 title = new H1("Игра «Коридор»");

        ComboBox<String> sizeSelector = new ComboBox<>("Размер поля");
        sizeSelector.setItems(Arrays.stream(GameSize.values()).map(GameSize::getDescription).toList());
        sizeSelector.setPlaceholder("Выберите размер");

        ComboBox<String> player1 = new ComboBox<>("Выберите первого игрока");
        List<String> player1s = new ArrayList<>();
        player1s.add("Игрок");
        player1s.addAll(Arrays.stream(ComputerAlgorithmType.values()).map(ComputerAlgorithmType::getDescription).toList());
        player1.setItems(player1s);

        ComboBox<String> player2 = new ComboBox<>("Выберите второго игрока");
        player2.setItems(Arrays.stream(ComputerAlgorithmType.values()).map(ComputerAlgorithmType::getDescription).toList());

        ComboBox<String> hardness = new ComboBox<>("Выберите уровень сложности игры");
        hardness.setItems(Arrays.stream(ComputerPlayerHardnessLevel.values()).map(ComputerPlayerHardnessLevel::getDescription).toList());

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
            if (hardness.getValue() == null) {
                Notification.show("Выберите сложность ИИ");
                return;
            }

            gameProcessor.startNewGame(size, player1.getValue(), player2.getValue(), hardness.getValue());
            getUI().ifPresent(ui -> ui.navigate("/game"));
        });

        add(title, sizeSelector, player1, player2, hardness, startButton);
    }
}
