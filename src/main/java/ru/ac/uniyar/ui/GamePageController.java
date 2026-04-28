package ru.ac.uniyar.ui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import ru.ac.uniyar.model.*;
import ru.ac.uniyar.model.algorithms.AlgorithmReport;
import ru.ac.uniyar.model.players.HumanPlayer;
import ru.ac.uniyar.model.players.Player;
import ru.ac.uniyar.service.GameProcessor;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Route("/game")
public class GamePageController extends VerticalLayout {

    private UI ui;
    private final GameProcessor gameProcessor;

    private final Div boardGrid = new Div();
    private final H1 turnLabel = new H1();
    private final H1 wallsLabel = new H1();
    private final Button aiStepButton = new Button("Сделать ход ИИ");
    private final Div reportPanel = new Div();
    private final Div hintPanel = new Div();
    private final Div historyPanel = new Div();
    private final Div statisticsPanel = new Div();
    private Move hintedMove;

    public GamePageController(GameProcessor gameProcessor) {
        this.gameProcessor = gameProcessor;
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        getStyle()
                .set("background", "#f6f7f9")
                .set("padding", "20px")
                .set("box-sizing", "border-box");

        Button restartButton = new Button("Начать сначала");
        restartButton.addClickListener(e -> UI.getCurrent().navigate("start"));
        aiStepButton.addClickListener(e -> {
            if (gameProcessor.getGame() == null || gameProcessor.getGame().isFinished()) {
                return;
            }
            if (gameProcessor.getCurrentPlayer() instanceof HumanPlayer) {
                Notification.show("Сейчас ход человека", 1000, Notification.Position.MIDDLE);
                return;
            }
            gameProcessor.makeComputerMove();
            renderBoard();
            processTurn();
        });
        Button hintButton = new Button("Подсказка хода", e -> {
            if (!(gameProcessor.getCurrentPlayer() instanceof HumanPlayer) || gameProcessor.isReplayMode()) {
                Notification.show("Подсказка доступна на ходе игрока", 1000, Notification.Position.MIDDLE);
                return;
            }
            AlgorithmReport hint = gameProcessor.getHintForCurrentPlayer();
            hintedMove = hint == null ? null : hint.move();
            hintPanel.setText(hint == null
                    ? "Подсказка недоступна"
                    : "Подсказка подсвечена на поле"
                    + " | оценка: " + hint.score()
                    + " | глубина: " + hint.reachedDepth()
                    + " | узлы: " + hint.nodesVisited());
            renderBoard();
        });
        Button replayPreviousButton = new Button("Назад по истории", e -> {
            gameProcessor.replayPrevious();
            renderBoard();
        });
        Button replayNextButton = new Button("Вперед", e -> {
            gameProcessor.replayNext();
            renderBoard();
            processTurn();
        });
        Button replayLiveButton = new Button("К текущему ходу", e -> {
            gameProcessor.replayLive();
            renderBoard();
            processTurn();
        });

        H1 title = new H1("Игра");
        title.getStyle().set("margin", "0");
        HorizontalLayout actionBar = new HorizontalLayout(aiStepButton, hintButton, restartButton);
        actionBar.setWidth("min(980px, 100%)");
        actionBar.getStyle()
                .set("background", "white")
                .set("padding", "12px")
                .set("border-radius", "8px")
                .set("box-shadow", "0 8px 20px rgba(15, 23, 42, 0.08)")
                .set("flex-wrap", "wrap");

        styleInfoPanel(reportPanel);
        styleInfoPanel(hintPanel);
        styleInfoPanel(historyPanel);
        styleInfoPanel(statisticsPanel);

        Div leftColumn = new Div(turnLabel, wallsLabel, hintPanel, reportPanel, statisticsPanel);
        leftColumn.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "12px")
                .set("width", "300px")
                .set("min-width", "260px");

        HorizontalLayout replayControls = new HorizontalLayout(replayPreviousButton, replayNextButton, replayLiveButton);
        replayControls.getStyle().set("flex-wrap", "wrap");

        Div rightColumn = new Div(historyPanel, replayControls);
        rightColumn.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "12px")
                .set("width", "300px")
                .set("min-width", "260px");

        Div boardColumn = new Div(boardGrid);
        boardColumn.getStyle()
                .set("display", "flex")
                .set("justify-content", "center")
                .set("align-items", "flex-start")
                .set("min-width", "0")
                .set("overflow", "auto");

        Div gameLayout = new Div(leftColumn, boardColumn, rightColumn);
        gameLayout.setWidthFull();
        gameLayout.getStyle()
                .set("display", "flex")
                .set("justify-content", "center")
                .set("align-items", "flex-start")
                .set("gap", "16px")
                .set("flex-wrap", "wrap");

        add(title);
        add(actionBar);
        add(gameLayout);
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

        if (game.isFinished()) {
            turnLabel.setText("Игра окончена. Победил " + getWinnerText(game));
        } else {
            turnLabel.setText(gameProcessor.isReplayMode()
                    ? "Просмотр хода #" + gameProcessor.getDisplayedMoveNumber()
                    : "Ход игрока: P" + gameProcessor.getCurrentPlayer().getPlayerId());
        }

        Player p1 = game.getPlayer1();
        Player p2 = game.getPlayer2();

        wallsLabel.setText("Стены: P1=" + p1.getAmountOfWallsLeft() + " | P2=" + p2.getAmountOfWallsLeft());
        updateAiStepButton();

        renderReport();
        renderHistory();
        renderStatistics();

        Board board = gameProcessor.getBoardForDisplay();
        int size = game.getGameSize().getAmountOfTilesPerSide();
        int renderSize = size * 2 - 1;

        boardGrid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", buildTemplate(renderSize))
                .set("background", "#d1d5db")
                .set("padding", "10px")
                .set("border-radius", "8px")
                .set("box-shadow", "0 12px 28px rgba(15, 23, 42, 0.14)");

        final List<Position> allowedMoves;
        if (gameProcessor.isReplayMode()) {
            allowedMoves = List.of();
        } else {
            Position currentPos = gameProcessor.getCurrentPlayer().getPlayerId() == 1
                    ? board.getPositionOfPlayer1()
                    : board.getPositionOfPlayer2();
            allowedMoves = board.getAvailableMoves(currentPos);
        }

        for (int i = 0; i < renderSize; i++) {
            for (int j = 0; j < renderSize; j++) {

                Div cell = new Div();
                boolean isTile = (i % 2 == 0 && j % 2 == 0);

                if (isTile) {
                    int ci = i / 2;
                    int cj = j / 2;
                    Position pos = new Position(ci, cj);

                    cell.setWidth("50px");
                    cell.setHeight("50px");

                    cell.getStyle()
                            .set("border", "1px solid #9ca3af")
                            .set("display", "flex")
                            .set("align-items", "center")
                            .set("justify-content", "center")
                            .set("background", "white")
                            .set("font-weight", "700");

                    if (pos.equals(board.getPositionOfPlayer1())) cell.setText("P1");
                    if (pos.equals(board.getPositionOfPlayer2())) cell.setText("P2");

                    if (isHintedMoveDestination(pos)) {
                        cell.getStyle()
                                .set("background", "#fde68a")
                                .set("box-shadow", "inset 0 0 0 4px #f59e0b");
                    }

                    if (allowedMoves.contains(pos)) {
                        cell.getElement().addEventListener("mouseover", e ->
                                cell.getStyle().set("background", "#a5d6a7"));
                        cell.getElement().addEventListener("mouseout", e ->
                                cell.getStyle().set("background", isHintedMoveDestination(pos) ? "#fde68a" : "white"));
                    }

                    cell.addClickListener(e -> {
                        if (!(gameProcessor.getCurrentPlayer() instanceof HumanPlayer)) return;
                        if (gameProcessor.isReplayMode()) return;

                        if (!allowedMoves.contains(pos)) {
                            Notification.show("Нельзя ходить сюда", 1000, Notification.Position.MIDDLE);
                            return;
                        }

                        gameProcessor.makeMove(
                                Move.movePlayer(gameProcessor.getCurrentPlayer().getPlayerId(), pos)
                        );
                        hintedMove = null;

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

                        BoardTile left = board.getTiles().get(new Position(ci, cj + 1));
                        BoardTile middle = board.getTiles().get(new Position(ci, cj));
                        BoardTile right = board.getTiles().get(new Position(ci, cj - 1));

                        isWall = ((left != null && !left.isBackwardsMovementAvailable()) ||
                                (right != null && !right.isBackwardsMovementAvailable()))
                                && (middle != null && !middle.isBackwardsMovementAvailable());
                    }

                    if (verticalGap) {
                        int ci = i / 2;
                        int cj = j / 2;

                        BoardTile top = board.getTiles().get(new Position(ci + 1, cj));
                        BoardTile middle = board.getTiles().get(new Position(ci, cj));
                        BoardTile bottom = board.getTiles().get(new Position(ci - 1, cj));

                        isWall = ((top != null && !top.isRightMovementAvailable()) ||
                                (bottom != null && !bottom.isRightMovementAvailable()))
                                && (middle != null && !middle.isRightMovementAvailable());
                    }

                    if (isHintedWallCell(i, j)) {
                        cell.getStyle()
                                .set("background", "#f59e0b")
                                .set("box-shadow", "0 0 0 2px #92400e");
                    } else {
                        cell.getStyle().set("background", isWall ? "#7c2d12" : "#e5e7eb");
                    }

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
        if (gameProcessor.isReplayMode()) return;
        Position[] w = gameProcessor.extractWall(i, j);
        if (w == null) return;

        if (gameProcessor.canPlaceWall(w[0], w[1])) {
            highlightWallPreview(i, j);
        }
    }

    private void tryPlaceWall(int i, int j) {
        if (gameProcessor.isReplayMode()) return;
        Position[] w = gameProcessor.extractWall(i, j);
        if (w == null) return;

        if (!gameProcessor.tryPlaceWall(w[0].row(), w[0].col(), w[1].row(), w[1].col())) {
            Notification.show("Некорректная стена", 1000, Notification.Position.MIDDLE);
            return;
        }

        hintedMove = null;
        renderBoard();
        ui.access(this::processTurn);
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
        if (game == null) return;
        if (gameProcessor.isReplayMode()) return;

        if (game != null && game.isFinished()) {
            renderBoard();
            return;
        }

        if (!(gameProcessor.getCurrentPlayer() instanceof HumanPlayer)) {

            if (isAiVsAi(game)) {
                updateAiStepButton();
                return;
            }

            gameProcessor.makeComputerMove();
            hintedMove = null;
            renderBoard();
            processTurn();
        }
    }

    private void updateAiStepButton() {
        Game game = gameProcessor.getGame();
        boolean visible = game != null && !game.isFinished()
                && !gameProcessor.isReplayMode()
                && isAiVsAi(game)
                && !(gameProcessor.getCurrentPlayer() instanceof HumanPlayer);
        aiStepButton.setVisible(visible);
        if (visible) {
            aiStepButton.setText("Сделать ход ИИ: P" + gameProcessor.getCurrentPlayer().getPlayerId());
        }
    }

    private boolean isAiVsAi(Game game) {
        return !(game.getPlayer1() instanceof HumanPlayer) && !(game.getPlayer2() instanceof HumanPlayer);
    }

    private void renderReport() {
        reportPanel.removeAll();
        AlgorithmReport report = gameProcessor.getLastAiReport();
        if (report == null) {
            reportPanel.setText("Ход ИИ еще не выполнен");
            return;
        }
        reportPanel.setText(
                "ИИ: " + report.algorithm()
                        + " | ход: " + describeMove(report.move())
                        + " | оценка: " + report.score()
                        + " | глубина/rollout: " + report.reachedDepth()
                        + " | узлы/итерации: " + report.nodesVisited()
                        + " | кандидаты: " + report.consideredMoves()
                        + " | отсечения: " + report.cutoffs()
                        + " | " + report.explanation()
        );
    }

    private void renderHistory() {
        historyPanel.removeAll();
        StringBuilder history = new StringBuilder("История ходов:\n");
        List<Move> moves = gameProcessor.getMoveHistory();
        for (int i = 0; i < moves.size(); ++i) {
            history.append(i + 1).append(". ").append(describeMove(moves.get(i))).append("\n");
        }
        historyPanel.getStyle().set("white-space", "pre-line");
        historyPanel.setText(history.toString());
    }

    private void renderStatistics() {
        statisticsPanel.removeAll();
        Game game = gameProcessor.getGame();
        if (game == null) {
            statisticsPanel.setText("Статистика появится после старта партии");
            return;
        }

        StringBuilder statistics = new StringBuilder();
        statistics.append("Статистика\n");
        statistics.append("Ходов: ").append(game.getAmountOfMoves()).append("\n");
        statistics.append("Время игры (с): ")
                .append(Duration.between(game.getGameTimeStart(), Instant.now()).toSeconds())
                .append("\n");
        statistics.append("Статус: ");
        if (game.isFinished()) {
            statistics.append("партия завершена, победил ").append(getWinnerText(game));
        } else {
            statistics.append("партия идет");
        }

        statisticsPanel.getStyle().set("white-space", "pre-line");
        statisticsPanel.setText(statistics.toString());
    }

    private void styleInfoPanel(Div panel) {
        panel.setWidthFull();
        panel.getStyle()
                .set("background", "white")
                .set("padding", "12px 14px")
                .set("border-radius", "8px")
                .set("box-shadow", "0 8px 20px rgba(15, 23, 42, 0.08)")
                .set("box-sizing", "border-box");
        if (panel == historyPanel) {
            panel.getStyle()
                    .set("height", "560px")
                    .set("max-height", "560px")
                    .set("overflow", "auto");
        }
    }

    private boolean isHintedMoveDestination(Position position) {
        return hintedMove != null
                && hintedMove.getMoveType() == ru.ac.uniyar.model.enums.MoveType.MOVE_PLAYER
                && position.equals(hintedMove.getEndPosition());
    }

    private boolean isHintedWallCell(int renderRow, int renderCol) {
        if (hintedMove == null || hintedMove.getMoveType() != ru.ac.uniyar.model.enums.MoveType.PLACE_WALL) {
            return false;
        }
        Position[] wall = gameProcessor.extractWall(renderRow, renderCol);
        if (wall == null) {
            return false;
        }
        return sameWall(wall[0], wall[1], hintedMove.getStartPosition(), hintedMove.getEndPosition());
    }

    private boolean sameWall(Position start1, Position end1, Position start2, Position end2) {
        return (start1.equals(start2) && end1.equals(end2)) || (start1.equals(end2) && end1.equals(start2));
    }

    private String getWinnerText(Game game) {
        if (game.isWonBy(1)) {
            return "P1";
        }
        if (game.isWonBy(2)) {
            return "P2";
        }
        return "не определен";
    }

    private String describeMove(Move move) {
        if (move == null) {
            return "-";
        }
        if (move.getMoveType() == ru.ac.uniyar.model.enums.MoveType.MOVE_PLAYER) {
            return "P" + move.getPlayerId() + " -> " + move.getEndPosition();
        }
        return "P" + move.getPlayerId() + " стена " + move.getStartPosition() + "-" + move.getEndPosition();
    }
}
