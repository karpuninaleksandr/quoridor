package ru.ac.uniyar.ui;

import com.vaadin.flow.component.AttachEvent;
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

    // ================== РЕНДЕР ==================

    private void renderBoard() {
        boardGrid.removeAll();

        Game game = gameProcessor.getGame();
        if (game == null) return;

        turnLabel.setText("Ход игрока: P" + gameProcessor.getCurrentPlayer().getPlayerId());

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

        String currentPos = gameProcessor.getCurrentPlayer().getPlayerId() == 1
                ? board.getPositionOfPlayer1()
                : board.getPositionOfPlayer2();

        List<String> allowedMoves = board.getAvailableMoves(currentPos);

        for (int i = 0; i < renderSize; i++) {
            for (int j = 0; j < renderSize; j++) {

                Div cell = new Div();
                boolean isTile = (i % 2 == 0 && j % 2 == 0);

                // ================== TILE ==================
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
                                cell.getStyle().set("background", "#a5d6a7")
                        );
                        cell.getElement().addEventListener("mouseout", e ->
                                cell.getStyle().set("background", "white")
                        );
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

                }
                // ================== GAP ==================
                else {

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

                    cell.getStyle().set("cursor", "pointer");

                    // 🎯 РИСУЕМ СТЕНЫ В GAP
                    if (horizontalGap && hasHorizontalWall(i, j, board)) {
                        cell.getStyle().set("background", "brown");
                        fillWallCenter(i, j, "brown"); // 👈 фикс
                    }
                    else if (verticalGap && hasVerticalWall(i, j, board)) {
                        cell.getStyle().set("background", "brown");
                        fillWallCenter(i, j, "brown"); // 👈 фикс
                    }
                    else {
                        cell.getStyle().set("background", "#eaeaea");
                    }

                    final int fi = i;
                    final int fj = j;

                    cell.getElement().addEventListener("mouseover", e ->
                            highlightWallPreview(fi, fj, true)
                    );

                    cell.getElement().addEventListener("mouseout", e ->
                            highlightWallPreview(fi, fj, false)
                    );

                    cell.addClickListener(e -> {
                        if (!(gameProcessor.getCurrentPlayer() instanceof HumanPlayer)) return;
                        tryPlaceWall(fi, fj);
                    });
                }

                boardGrid.add(cell);
            }
        }
    }

    // ================== ЛОГИКА СТЕН ==================

    private boolean hasHorizontalWall(int i, int j, Board board) {
        int ci = i / 2;
        int cj = j / 2;
        BoardTile tile = board.getTiles().get(ci + "" + cj);
        return tile != null && !tile.isBackwardsMovementAvailable();
    }

    private boolean hasVerticalWall(int i, int j, Board board) {
        int ci = i / 2;
        int cj = j / 2;
        BoardTile tile = board.getTiles().get(ci + "" + cj);
        return tile != null && !tile.isRightMovementAvailable();
    }

    private void tryPlaceWall(int i, int j) {

        Game game = gameProcessor.getGame();
        if (game == null) return;

        if (!(gameProcessor.getCurrentPlayer() instanceof HumanPlayer)) return;
        if (!gameProcessor.getCurrentPlayer().canPlaceWall()) return;

        int size = game.getGameSize().getAmountOfTilesPerSide();

        int ci = i / 2;
        int cj = j / 2;

        // горизонтальная
        if (i % 2 == 1 && j % 2 == 0) {
            if (cj + 1 >= size || ci + 1 >= size) return;
            placeWall(ci, cj, ci, cj + 1);
            fillWallCenter(i, j, "brown");
            return;
        }

        // вертикальная
        if (i % 2 == 0 && j % 2 == 1) {
            if (ci + 1 >= size || cj + 1 >= size) return;
            placeWall(ci, cj, ci + 1, cj);
            fillWallCenter(i, j, "brown");
        }
    }

    private void placeWall(int i1, int j1, int i2, int j2) {

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
        UI.getCurrent().access(this::processTurn);
    }

    private void highlightWallPreview(int i, int j, boolean on) {

        int renderSize = gameProcessor.getGame().getGameSize().getAmountOfTilesPerSide() * 2 - 1;
        String color = on ? "rgba(150,0,0,0.4)" : "#eaeaea";

        if (i % 2 == 1 && j % 2 == 0) {
            if (j + 2 >= renderSize) return;
            setGapColor(i, j, color);
            setGapColor(i, j + 2, color);
            fillWallCenter(i, j, color);
        }

        if (i % 2 == 0 && j % 2 == 1) {
            if (i + 2 >= renderSize) return;
            setGapColor(i, j, color);
            setGapColor(i + 2, j, color);
            fillWallCenter(i, j, color);
        }
    }

    private void setGapColor(int i, int j, String color) {
        int renderSize = gameProcessor.getGame().getGameSize().getAmountOfTilesPerSide() * 2 - 1;
        int index = i * renderSize + j;

        if (index >= 0 && index < boardGrid.getChildren().count()) {
            Div cell = (Div) boardGrid.getChildren().toArray()[index];
            cell.getStyle().set("background", color);
        }
    }

    // ================== PATH CHECK ==================

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
                if (visited.add(next)) q.add(next);
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

            Move move = gameProcessor.getCurrentPlayer().getMove(
                    game.getBoard(),
                    gameProcessor.getCurrentPlayer().getPlayerId(),
                    gameProcessor.getCurrentPlayer().getAmountOfWallsLeft()
            );

            if (move != null) gameProcessor.makeMove(move);

            if (ui != null && ui.isAttached()) {
                ui.access(() -> {
                    renderBoard();
                    processTurn();
                });
            }
        }
    }

    private void notifyInvalid() {
        Notification.show("Некорректная стена", 1000, Notification.Position.MIDDLE);
    }

    private void fillWallCenter(int i, int j, String color) {
        if (i % 2 == 1 && j % 2 == 0) {
            int centerJ = j + 1;

            setGapColor(i, centerJ, color);
        }

        if (i % 2 == 0 && j % 2 == 1) {
            int centerI = i + 1;

            setGapColor(centerI, j, color);
        }
    }
}