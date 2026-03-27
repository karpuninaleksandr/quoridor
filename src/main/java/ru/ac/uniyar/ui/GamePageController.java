package ru.ac.uniyar.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import ru.ac.uniyar.model.Board;
import ru.ac.uniyar.model.Game;
import ru.ac.uniyar.model.Move;
import ru.ac.uniyar.service.GameProcessor;

@Route("/game")
public class GamePageController extends VerticalLayout {

    private final GameProcessor gameProcessor;
    private final Div boardGrid = new Div();

    public GamePageController(GameProcessor gameProcessor) {
        this.gameProcessor = gameProcessor;

        add(new H1("Игра"));

        add(boardGrid);

        Button nextMove = new Button("Следующий ход");
        nextMove.addClickListener(e -> {
//            gameProcessor.makeHumanMove(m);
            renderBoard();
        });

        add(nextMove);

        renderBoard();
    }

    private void renderBoard() {
        boardGrid.removeAll();

        Game game = gameProcessor.getGame();
        if (game == null) {
            add(new H1("Ошибка: игра не создана"));
            return;
        }
        Board board = game.getBoard();
        int size = game.getGameSize().getAmountOfTilesPerSide();

        boardGrid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(" + size + ", 50px)");

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                String pos = i + "" + j;

                Div cell = new Div();
                cell.setWidth("50px");
                cell.setHeight("50px");

                cell.getStyle()
                        .set("border", "1px solid black")
                        .set("display", "flex")
                        .set("align-items", "center")
                        .set("justify-content", "center");

                if (pos.equals(board.getPositionOfPlayer1())) {
                    cell.setText("P1");
                    cell.getStyle().set("background", "lightblue");
                }

                if (pos.equals(board.getPositionOfPlayer2())) {
                    cell.setText("P2");
                    cell.getStyle().set("background", "lightcoral");
                }

                cell.addClickListener(e -> {
                    Move move = Move.movePlayer(
                            game.getCurrentPlayer(),
                            pos
                    );

                    gameProcessor.makeHumanMove(move);
                    renderBoard();
                });

                boardGrid.add(cell);
            }
        }
    }
}
