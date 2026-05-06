package ru.ac.uniyar.ui;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import ru.ac.uniyar.model.Game;

import java.time.Duration;
import java.time.Instant;

@Route("/statistics")
@CssImport("./styles/statistics-page.css")
public class StatisticsPageController extends VerticalLayout {
    public StatisticsPageController() {
        setAlignItems(Alignment.CENTER);
        addClassName("statistics-page");
        Game game = (Game) VaadinSession.getCurrent().getAttribute("game");

        H1 title = new H1("Статистика игры");
        title.addClassName("statistics-page__title");

        Div card = new Div(
                new Div(new Text("Количество ходов: " + game.getAmountOfMoves())),
                new Div(new Text("Время игры (c): " + Duration.between(game.getGameTimeStart(), Instant.now()).toSeconds()))
        );
        card.addClassName("statistics-page__card");

        Button restartButton = new Button("Начать сначала");
        restartButton.addClickListener(e -> UI.getCurrent().navigate("start"));
        add(title, card, restartButton);
    }
}
