package ru.ac.uniyar.ui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import ru.ac.uniyar.model.*;
import ru.ac.uniyar.model.players.HumanPlayer;
import ru.ac.uniyar.model.players.Player;
import ru.ac.uniyar.service.GameProcessor;

import java.util.*;

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
        restartButton.addClickListener(e -> goToStart());

        add(new H1("Игра"));
        add(turnLabel);
        add(wallsLabel);
        add(boardGrid);
        add(restartButton);
    }

    private void goToStart() {
        UI.getCurrent().navigate("start");
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

        turnLabel.setText("Ход игрока: P" + game.getCurrentPlayer());

        Player p1 = game.getPlayer1();
        Player p2 = game.getPlayer2();

        wallsLabel.setText("Стены: P1=" + p1.getAmountOfWallsLeft()
                + " | P2=" + p2.getAmountOfWallsLeft());

        Board board = game.getBoard();

        int size = game.getGameSize().getAmountOfTilesPerSide();
        int renderSize = size * 2 - 1;

        boardGrid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", buildTemplate(renderSize));

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
                            .set("justify-content", "center");

                    BoardTile tile = board.getTiles().get(pos);

                    if (!tile.isLeftMovementAvailable()) cell.getStyle().set("border-left", "5px solid brown");
                    if (!tile.isRightMovementAvailable()) cell.getStyle().set("border-right", "5px solid brown");
                    if (!tile.isForwardMovementAvailable()) cell.getStyle().set("border-top", "5px solid brown");
                    if (!tile.isBackwardsMovementAvailable()) cell.getStyle().set("border-bottom", "5px solid brown");

                    if (pos.equals(board.getPositionOfPlayer1())) cell.setText("P1");
                    if (pos.equals(board.getPositionOfPlayer2())) cell.setText("P2");

                    cell.addClickListener(e -> {
                        if (!(gameProcessor.getCurrentPlayer() instanceof HumanPlayer)) return;

                        int current = game.getCurrentPlayer();

                        String from = game.getCurrentPlayer() == 1 ? board.getPositionOfPlayer1() : board.getPositionOfPlayer2();

                        List<String> allowed = board.getAvailableMoves(from);

                        if (!allowed.contains(pos)) {
                            Notification.show("Нельзя ходить сюда", 1000, Notification.Position.MIDDLE);
                            return;
                        }

                        try {
                            gameProcessor.makeMove(
                                    Move.movePlayer(current, pos)
                            );
                        } catch (Exception ex) {
                            Notification.show("Некорректный ход", 1000, Notification.Position.MIDDLE);
                            return;
                        }

                        renderBoard();
                        processTurn();
                    });

                } else {

                    boolean horizontalGap = (i % 2 == 1 && j % 2 == 0);
                    boolean verticalGap = (i % 2 == 0 && j % 2 == 1);

                    if (horizontalGap) {
                        cell.setWidth("50px");
                        cell.setHeight("5px");
                    } else if (verticalGap) {
                        cell.setWidth("5px");
                        cell.setHeight("50px");
                    } else {
                        cell.setWidth("5px");
                        cell.setHeight("5px");
                    }

                    cell.getStyle()
                            .set("background", "#eaeaea")
                            .set("cursor", "pointer");

                    final int fi = i;
                    final int fj = j;

                    cell.addClickListener(e -> {
                        if (!(gameProcessor.getCurrentPlayer() instanceof HumanPlayer)) return;
                        tryPlaceWall(fi, fj);
                    });
                }

                boardGrid.add(cell);
            }
        }
    }

    private void tryPlaceWall(int i, int j) {

        Game game = gameProcessor.getGame();
        if (!gameProcessor.getCurrentPlayer().canPlaceWall()) return;

        int size = game.getGameSize().getAmountOfTilesPerSide();

        if (i % 2 == 1 && j % 2 == 0) {

            int ci = i / 2;
            int cj = j / 2;

            if (cj + 1 >= size) return;

            placeWall2x1(
                    ci, cj,
                    ci, cj + 1
            );
            return;
        }

        if (i % 2 == 0 && j % 2 == 1) {

            int ci = i / 2;
            int cj = j / 2;

            if (ci + 1 >= size) return;

            placeWall2x1(
                    ci, cj,
                    ci - 1, cj
            );
        }
    }

    private void placeWall2x1(int i1, int j1, int i2, int j2) {

        Game game = gameProcessor.getGame();
        Board copy = game.getBoard().copy();

        try {
            copy.placeWall(i1 + "" + j1, i2 + "" + j2);
        } catch (Exception e) {
            notifyInvalid();
            return;
        }

        if (!hasPath(copy, copy.getPositionOfPlayer1(), true)
                || !hasPath(copy, copy.getPositionOfPlayer2(), false)) {
            notifyInvalid();
            return;
        }

        gameProcessor.makeMove(
                Move.placeWall(gameProcessor.getCurrentPlayer().getPlayerId(),
                        i1 + "" + j1,
                        i2 + "" + j2)
        );

        renderBoard();
        processTurn();
    }

    private void notifyInvalid() {
        Notification.show("Некорректная стена", 1000, Notification.Position.MIDDLE);
    }

    private boolean hasPath(Board board, String start, boolean toBottom) {

        int size = gameProcessor.getGame().getGameSize().getAmountOfTilesPerSide();

        Set<String> visited = new HashSet<>();
        Queue<String> q = new LinkedList<>();

        q.add(start);
        visited.add(start);

        while (!q.isEmpty()) {

            String cur = q.poll();
            int i = cur.charAt(0) - '0';

            if (toBottom && i == size - 1) return true;
            if (!toBottom && i == 0) return true;

            for (String next : board.getAvailableMoves(cur)) {
                if (visited.add(next)) {
                    q.add(next);
                }
            }
        }

        return false;
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

        if (game.isFinished()) {
            turnLabel.setText("Игра окончена");
            return;
        }

        if (!(gameProcessor.getCurrentPlayer() instanceof HumanPlayer)) {

            Move move = gameProcessor.getCurrentPlayer().getMove(game.getBoard(), game.getCurrentPlayer());

            if (move != null) gameProcessor.makeMove(move);

            if (ui != null && ui.isAttached()) {
                ui.access(() -> {
                    renderBoard();
                    processTurn();
                });
            }
        }
    }
}