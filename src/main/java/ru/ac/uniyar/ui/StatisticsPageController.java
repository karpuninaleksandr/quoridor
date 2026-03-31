package ru.ac.uniyar.ui;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import ru.ac.uniyar.model.Game;

import java.time.Duration;
import java.time.Instant;

@Route("/statistics")
public class StatisticsPageController extends VerticalLayout {
    public StatisticsPageController() {
        Game game = (Game) VaadinSession.getCurrent().getAttribute("game");

        add(new H1("Статистика игры"));

        add(new Div(new Text("Количество ходов: " + game.getAmountOfMoves())));
        add(new Div(new Text("Время игры (c): " + Duration.between(game.getGameTimeStart(), Instant.now()).toSeconds())));

        Button restartButton = new Button("Начать сначала");
        restartButton.addClickListener(e -> UI.getCurrent().navigate("start"));
        add(restartButton);
    }
}
