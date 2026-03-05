package ru.ac.uniyar.ui;

import ru.ac.uniyar.service.GameProcessor;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import java.util.ArrayList;
import java.util.List;

@Route("/start")
public class StartPageController extends VerticalLayout {

    public StartPageController(GameProcessor gameProcessor) {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        H1 title = new H1("Игра «Коридор»");

        ComboBox<String> sizeSelector = new ComboBox<>("Размер поля");
        sizeSelector.setItems(gameProcessor.getAvailableBoardSizes());
        sizeSelector.setPlaceholder("Выберите размер");

        ComboBox<String> player1 = new ComboBox<>("Выберите первого игрока");
        List<String> player1s = new ArrayList<>();
        player1s.add("Игрок");
        player1s.addAll(gameProcessor.getAvailablePlayerTypes());
        player1.setItems(player1s);

        ComboBox<String> player2 = new ComboBox<>("Выберите второго игрока");
        player2.setItems(gameProcessor.getAvailablePlayerTypes());

        ComboBox<String> hardness = new ComboBox<>("Выберите уровень сложности игры");
        hardness.setItems(gameProcessor.getAvailableHardnessLevels());

        Button startButton = new Button("Начать игру", event -> {
            String size = sizeSelector.getValue();
            if (size == null) {
                Notification.show("Выберите размер поля");
                return;
            }

            gameProcessor.startNewGame(size, player1.getValue(), player2.getValue(), hardness.getValue());
        });

        add(title, sizeSelector, startButton);
    }
}