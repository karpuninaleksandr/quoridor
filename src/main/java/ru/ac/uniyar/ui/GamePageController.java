package ru.ac.uniyar.ui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import ru.ac.uniyar.model.Board;
import ru.ac.uniyar.model.Game;
import ru.ac.uniyar.model.Move;
import ru.ac.uniyar.model.players.HumanPlayer;
import ru.ac.uniyar.service.GameProcessor;

@Route("/game")
public class GamePageController extends VerticalLayout {
    private UI ui;
    private final GameProcessor gameProcessor;
    private final Div boardGrid = new Div();
    private final H1 turnLabel = new H1();

    public GamePageController(GameProcessor gameProcessor) {
        this.gameProcessor = gameProcessor;

        add(new H1("Игра"));
        add(turnLabel);
        add(boardGrid);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        this.ui = attachEvent.getUI();

        renderBoard();
        processTurn();
    }

    private void renderBoard() {
        boardGrid.removeAll();

        Game game = gameProcessor.getGame();
        if (game == null) {
            add(new H1("Ошибка: игра не создана"));
            return;
        }

        turnLabel.setText("Ход игрока: P" + game.getCurrentPlayer());

        Board board = game.getBoard();
        int size = game.getGameSize().getAmountOfTilesPerSide();

        boardGrid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(" + size + ", 50px)");

        for (int i = size - 1; i >= 0; --i) {
            for (int j = size - 1; j >= 0; --j) {
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
                    if (!(gameProcessor.getCurrentPlayer() instanceof HumanPlayer)) {
                        return;
                    }

                    Move move = Move.movePlayer(game.getCurrentPlayer(), pos);

                    gameProcessor.makeMove(move);

                    renderBoard();
                    processTurn();
                });

                boardGrid.add(cell);
            }
        }
    }

    private void processTurn() {
        Game game = gameProcessor.getGame();

        if (game.isFinished()) {
            turnLabel.setText("Игра окончена");
            return;
        }

        if (!(gameProcessor.getCurrentPlayer() instanceof HumanPlayer)) {
                Game currentGame = gameProcessor.getGame();

                Move move = gameProcessor.getCurrentPlayer().getMove(currentGame.getBoard());

                if (move != null) {
                    gameProcessor.makeMove(move);
                }

                if (ui != null && ui.isAttached()) {
                    ui.access(() -> {
                        renderBoard();
                        processTurn();
                    });
                }
        }
    }
}
