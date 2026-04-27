package ru.ac.uniyar.ui;

import ru.ac.uniyar.model.enums.ComputerAlgorithmType;
import ru.ac.uniyar.model.enums.ComputerPlayerHardnessLevel;
import ru.ac.uniyar.model.enums.GameSize;
import ru.ac.uniyar.service.GameProcessor;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
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
        getStyle()
                .set("background", "#f6f7f9")
                .set("padding", "24px")
                .set("box-sizing", "border-box");

        H1 title = new H1("Игра «Коридор»");
        title.getStyle().set("margin", "0 0 18px");

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

        ComboBox<String> hardness1 = new ComboBox<>("Сложность P1");
        hardness1.setItems(Arrays.stream(ComputerPlayerHardnessLevel.values()).map(ComputerPlayerHardnessLevel::getDescription).toList());
        hardness1.setHelperText("Используется, если P1 играет ИИ");

        ComboBox<String> hardness2 = new ComboBox<>("Сложность P2");
        hardness2.setItems(Arrays.stream(ComputerPlayerHardnessLevel.values()).map(ComputerPlayerHardnessLevel::getDescription).toList());

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

        Div panel = new Div(sizeSelector, player1, player2, hardness1, hardness2);
        panel.getStyle()
                .set("display", "grid")
                .set("gap", "12px")
                .set("width", "min(420px, 100%)")
                .set("background", "white")
                .set("padding", "22px")
                .set("border-radius", "8px")
                .set("box-shadow", "0 12px 28px rgba(15, 23, 42, 0.12)");

        HorizontalLayout buttons = new HorizontalLayout(startButton, tournamentButton);
        buttons.setJustifyContentMode(JustifyContentMode.CENTER);
        add(title, panel, buttons);
    }
}
