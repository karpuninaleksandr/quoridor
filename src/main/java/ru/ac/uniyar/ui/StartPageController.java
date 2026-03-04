package ru.ac.uniyar.ui;

import ru.ac.uniyar.model.enums.GameSize;
import ru.ac.uniyar.service.GameProcessor;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

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

        Button startButton = new Button("Начать игру", event -> {
            String size = sizeSelector.getValue();
            if (size == null) {
                Notification.show("Выберите размер поля");
                return;
            }

            gameProcessor.startNewGame(size);
        });

        add(title, sizeSelector, startButton);
    }
}