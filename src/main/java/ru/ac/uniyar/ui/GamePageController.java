package ru.ac.uniyar.ui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import ru.ac.uniyar.model.*;
import ru.ac.uniyar.model.players.HumanPlayer;
import ru.ac.uniyar.model.players.Player;
import ru.ac.uniyar.service.GameProcessor;

import java.util.List;

@Route("/game")
public class GamePageController extends VerticalLayout {

    private UI ui;
    private final GameProcessor gameProcessor;

    private final Div boardGrid = new Div();
    private final H1 turnLabel = new H1();
    private final H1 wallsLabel = new H1();

    public GamePageController(GameProcessor gameProcessor) {
        this.gameProcessor = gameProcessor;

        Button restartButton = new Button("Начать сначала");
        restartButton.addClickListener(e -> UI.getCurrent().navigate("start"));

        add(new H1("Игра"));
        add(turnLabel);
        add(wallsLabel);
        add(boardGrid);
        add(restartButton);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        this.ui = attachEvent.getUI();
        renderBoard();
        processTurn();
    }

    private void renderBoard() {
        boardGrid.removeAll();

        Game game = gameProcessor.getGame();
        if (game == null) return;

        turnLabel.setText("Ход игрока: P" + gameProcessor.getCurrentPlayer().getPlayerId());

        Player p1 = game.getPlayer1();
        Player p2 = game.getPlayer2();

        wallsLabel.setText("Стены: P1=" + p1.getAmountOfWallsLeft() + " | P2=" + p2.getAmountOfWallsLeft());

        Board board = game.getBoard();
        int size = game.getGameSize().getAmountOfTilesPerSide();
        int renderSize = size * 2 - 1;

        boardGrid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", buildTemplate(renderSize));

        String currentPos = gameProcessor.getCurrentPlayer().getPlayerId() == 1
                ? board.getPositionOfPlayer1()
                : board.getPositionOfPlayer2();

        List<String> allowedMoves = board.getAvailableMoves(currentPos);

        for (int i = 0; i < renderSize; i++) {
            for (int j = 0; j < renderSize; j++) {

                Div cell = new Div();
                boolean isTile = (i % 2 == 0 && j % 2 == 0);

                if (isTile) {
                    int ci = i / 2;
                    int cj = j / 2;
                    String pos = ci + "" + cj;

                    cell.setWidth("50px");
                    cell.setHeight("50px");

                    cell.getStyle()
                            .set("border", "1px solid black")
                            .set("display", "flex")
                            .set("align-items", "center")
                            .set("justify-content", "center")
                            .set("background", "white");

                    if (pos.equals(board.getPositionOfPlayer1())) cell.setText("P1");
                    if (pos.equals(board.getPositionOfPlayer2())) cell.setText("P2");

                    if (allowedMoves.contains(pos)) {
                        cell.getElement().addEventListener("mouseover", e ->
                                cell.getStyle().set("background", "#a5d6a7"));
                        cell.getElement().addEventListener("mouseout", e ->
                                cell.getStyle().set("background", "white"));
                    }

                    cell.addClickListener(e -> {
                        if (!(gameProcessor.getCurrentPlayer() instanceof HumanPlayer)) return;

                        if (!allowedMoves.contains(pos)) {
                            Notification.show("Нельзя ходить сюда", 1000, Notification.Position.MIDDLE);
                            return;
                        }

                        gameProcessor.makeMove(
                                Move.movePlayer(gameProcessor.getCurrentPlayer().getPlayerId(), pos)
                        );

                        renderBoard();
                        processTurn();
                    });

                } else {
                    boolean horizontalGap = (i % 2 == 1 && j % 2 == 0);
                    boolean verticalGap = (i % 2 == 0 && j % 2 == 1);

                    if (horizontalGap) {
                        cell.setWidth("50px");
                        cell.setHeight("8px");
                    } else if (verticalGap) {
                        cell.setWidth("8px");
                        cell.setHeight("50px");
                    } else {
                        cell.setWidth("8px");
                        cell.setHeight("8px");
                    }

                    boolean isWall = false;

                    if (horizontalGap) {
                        int ci = i / 2;
                        int cj = j / 2;

                        BoardTile left = board.getTiles().get(ci + "" + (cj + 1));
                        BoardTile middle = board.getTiles().get(ci + "" + cj);
                        BoardTile right = board.getTiles().get(ci + "" + (cj - 1));

                        isWall = ((left != null && !left.isBackwardsMovementAvailable()) ||
                                (right != null && !right.isBackwardsMovementAvailable()))
                                && (middle != null && !middle.isBackwardsMovementAvailable());
                    }

                    if (verticalGap) {
                        int ci = i / 2;
                        int cj = j / 2;

                        BoardTile top = board.getTiles().get((ci + 1) + "" + cj);
                        BoardTile middle = board.getTiles().get(ci + "" + cj);
                        BoardTile bottom = board.getTiles().get((ci - 1) + "" + cj);

                        isWall = ((top != null && !top.isRightMovementAvailable()) ||
                                (bottom != null && !bottom.isRightMovementAvailable()))
                                && (middle != null && !middle.isRightMovementAvailable());
                    }

                    cell.getStyle().set("background", isWall ? "brown" : "#eaeaea");

                    final int fi = i;
                    final int fj = j;

                    cell.getElement().addEventListener("mouseover", e -> highlightIfValid(fi, fj));
                    cell.getElement().addEventListener("mouseout", e -> renderBoard());
                    cell.addClickListener(e -> tryPlaceWall(fi, fj));
                }

                boardGrid.add(cell);
            }
        }
    }

    private void highlightIfValid(int i, int j) {
        int[] w = extractWall(i, j);
        if (w == null) return;

        if (gameProcessor.canPlaceWall(w[0], w[1], w[2], w[3])) {
            highlightWallPreview(i, j);
        }
    }

    private void tryPlaceWall(int i, int j) {
        int[] w = extractWall(i, j);
        if (w == null) return;

        if (!gameProcessor.tryPlaceWall(w[0], w[1], w[2], w[3])) {
            Notification.show("Некорректная стена", 1000, Notification.Position.MIDDLE);
            return;
        }

        renderBoard();
        ui.access(this::processTurn);
    }

    private int[] extractWall(int i, int j) {
        Game game = gameProcessor.getGame();
        if (game == null) return null;

        int ci = i / 2;
        int cj = j / 2;
        int size = game.getGameSize().getAmountOfTilesPerSide();

        if (i % 2 == 1 && j % 2 == 0) {
            if (cj + 1 >= size || ci + 1 >= size) return null;
            return new int[]{ci, cj, ci, cj + 1};
        }

        if (i % 2 == 0 && j % 2 == 1) {
            if (ci + 1 >= size || cj + 1 >= size) return null;
            return new int[]{ci, cj, ci + 1, cj};
        }

        return null;
    }

    private void highlightWallPreview(int i, int j) {
        if (i % 2 == 1 && j % 2 == 0) {
            setGapColor(i, j);
            setGapColor(i, j + 1);
            setGapColor(i, j + 2);
        }
        if (i % 2 == 0 && j % 2 == 1) {
            setGapColor(i, j);
            setGapColor(i + 1, j);
            setGapColor(i + 2, j);
        }
    }

    private void setGapColor(int i, int j) {
        int size = gameProcessor.getGame().getGameSize().getAmountOfTilesPerSide();
        int renderSize = size * 2 - 1;

        if (i < 0 || j < 0 || i >= renderSize || j >= renderSize) return;

        int index = i * renderSize + j;

        if (index < boardGrid.getChildren().count()) {
            Div cell = (Div) boardGrid.getChildren().toArray()[index];
            cell.getStyle().set("background", "orange");
        }
    }

    private String buildTemplate(int size) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; i++) {
            sb.append(i % 2 == 0 ? "50px " : "10px ");
        }
        return sb.toString();
    }

    private void processTurn() {
        Game game = gameProcessor.getGame();

        if (game != null && game.isFinished()) {
            VaadinSession.getCurrent().setAttribute("game", game);
            ui.navigate("statistics");
            turnLabel.setText("Игра окончена");
            return;
        }

        if (!(gameProcessor.getCurrentPlayer() instanceof HumanPlayer)) {

            Move move = gameProcessor.getCurrentPlayer().getMove(
                    game.getBoard(),
                    gameProcessor.getCurrentPlayer().getPlayerId(),
                    gameProcessor.getCurrentPlayer().getAmountOfWallsLeft()
            );

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