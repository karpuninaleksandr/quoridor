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
import java.util.ArrayList;
import java.util.List;

@Route("/game")
public class GamePageController extends VerticalLayout {

    private UI ui;
    private final GameProcessor gameProcessor;

    private final Div boardGrid = new Div();
    private final Button aiStepButton = new Button("Сделать ход ИИ");
    private final GameInfoPanel infoPanel = new GameInfoPanel();
    private final GameHistoryPanel historyPanel;
    private final Button hintButton = new Button("Подсказка хода");
    private final Button restartButton = new Button("Начать сначала");
    private List<AlgorithmReport> hintedReports = List.of();
    private int tileSizePx = 50;
    private int gapSizePx = 10;

    public GamePageController(GameProcessor gameProcessor) {
        this.gameProcessor = gameProcessor;
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        getStyle()
                .set("background", "#f6f7f9")
                .set("padding", "10px 14px")
                .set("box-sizing", "border-box")
                .set("height", "100vh")
                .set("overflow", "hidden");

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
        hintButton.addClickListener(e -> {
            if (gameProcessor.getGame() == null) {
                Notification.show("Сначала начните игру", 1000, Notification.Position.MIDDLE);
                return;
            }
            if (!(gameProcessor.getCurrentPlayer() instanceof HumanPlayer) || gameProcessor.isReplayMode()) {
                Notification.show("Подсказка доступна на ходе игрока", 1000, Notification.Position.MIDDLE);
                return;
            }
            hintedReports = gameProcessor.getHintsFromAllAlgorithms();
            if (hintedReports.isEmpty()) {
                Notification.show("Подсказки недоступны", 1000, Notification.Position.MIDDLE);
            }
            infoPanel.setHintLegend(hintedReports, this::getAlgorithmColor);
            renderBoard();
        });
        Button replayPreviousButton = new Button("←", e -> {
            gameProcessor.replayPrevious();
            renderBoard();
        });
        Button replayNextButton = new Button("→", e -> {
            gameProcessor.replayNext();
            renderBoard();
            processTurn();
        });
        Button replayLiveButton = new Button("К текущему", e -> {
            gameProcessor.replayLive();
            renderBoard();
            processTurn();
        });
        replayPreviousButton.getElement().setProperty("title", "Предыдущий ход");
        replayNextButton.getElement().setProperty("title", "Следующий ход");
        replayLiveButton.getElement().setProperty("title", "Вернуться к текущему ходу");

        H1 title = new H1("Коридор");
        title.getStyle()
                .set("margin", "0 0 8px")
                .set("font-size", "28px");

        historyPanel = new GameHistoryPanel(replayPreviousButton, replayNextButton, replayLiveButton);

        HorizontalLayout boardActions = new HorizontalLayout(hintButton, aiStepButton, restartButton);
        boardActions.getStyle()
                .set("display", "flex")
                .set("justify-content", "center")
                .set("flex-wrap", "wrap")
                .set("gap", "8px");

        Div boardColumn = new Div(boardGrid, boardActions);
        boardColumn.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("justify-content", "center")
                .set("align-items", "center")
                .set("gap", "12px")
                .set("width", "620px")
                .set("min-width", "620px")
                .set("height", "calc(100vh - 88px)")
                .set("max-height", "calc(100vh - 88px)")
                .set("overflow", "hidden");

        Div gameLayout = new Div(infoPanel, boardColumn, historyPanel);
        gameLayout.setWidthFull();
        gameLayout.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "360px 620px 360px")
                .set("justify-content", "center")
                .set("align-items", "flex-start")
                .set("gap", "16px")
                .set("overflow", "hidden");

        add(title);
        add(gameLayout);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        this.ui = attachEvent.getUI();
        if (gameProcessor.getGame() == null) {
            this.ui.navigate("start");
            return;
        }
        renderBoard();
        processTurn();
    }

    private void renderBoard() {
        boardGrid.removeAll();

        Game game = gameProcessor.getGame();
        if (game == null) {
            UI.getCurrent().navigate("start");
            return;
        }

        if (game.isFinished()) {
            infoPanel.setTurnStatus("Игра окончена. Победил " + getWinnerText(game), 0);
        } else if (gameProcessor.isReplayMode()) {
            infoPanel.setTurnStatus("Просмотр хода #" + gameProcessor.getDisplayedMoveNumber(), 0);
        } else {
            infoPanel.setTurnStatus("Ходит игрок", gameProcessor.getCurrentPlayer().getPlayerId());
        }

        Player p1 = game.getPlayer1();
        Player p2 = game.getPlayer2();

        infoPanel.setWallsCounts(p1.getAmountOfWallsLeft(), p2.getAmountOfWallsLeft());
        infoPanel.setHintLegend(hintedReports, this::getAlgorithmColor);
        updateAiStepButton();

        renderReport();
        renderHistory();
        renderStatistics();

        Board board = gameProcessor.getBoardForDisplay();
        int size = game.getGameSize().getAmountOfTilesPerSide();
        updateBoardScale(size);
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

                    cell.setWidth(tileSizePx + "px");
                    cell.setHeight(tileSizePx + "px");

                    cell.getStyle()
                            .set("border", "1px solid #9ca3af")
                            .set("display", "flex")
                            .set("align-items", "center")
                            .set("justify-content", "center")
                            .set("background", "white")
                            .set("font-weight", "700");

                    if (pos.equals(board.getPositionOfPlayer1())) {
                        renderPiece(cell, "P1", "#2563eb");
                    }
                    if (pos.equals(board.getPositionOfPlayer2())) {
                        renderPiece(cell, "P2", "#dc2626");
                    }

                    List<AlgorithmReport> cellHints = getMoveHintsForPosition(pos);
                    if (!cellHints.isEmpty()) {
                        cell.getStyle()
                                .set("background", buildHintBackground(cellHints))
                                .set("box-shadow", "inset 0 0 0 4px #f59e0b");
                    }

                    if (allowedMoves.contains(pos)) {
                        cell.getElement().addEventListener("mouseover", e ->
                                cell.getStyle().set("background", "#a5d6a7"));
                        cell.getElement().addEventListener("mouseout", e ->
                                cell.getStyle().set("background", cellHints.isEmpty() ? "white" : buildHintBackground(cellHints)));
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
                        hintedReports = List.of();

                        renderBoard();
                        processTurn();
                    });

                } else {
                    boolean horizontalGap = (i % 2 == 1 && j % 2 == 0);
                    boolean verticalGap = (i % 2 == 0 && j % 2 == 1);

                    if (horizontalGap) {
                        cell.setWidth(tileSizePx + "px");
                        cell.setHeight(gapSizePx + "px");
                    } else if (verticalGap) {
                        cell.setWidth(gapSizePx + "px");
                        cell.setHeight(tileSizePx + "px");
                    } else {
                        cell.setWidth(gapSizePx + "px");
                        cell.setHeight(gapSizePx + "px");
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

                    if (!horizontalGap && !verticalGap) {
                        isWall = isWallCenter(board, i, j);
                    }

                    List<AlgorithmReport> wallHints = getWallHintsForCell(i, j);
                    if (!wallHints.isEmpty()) {
                        cell.getStyle()
                                .set("background", buildHintBackground(wallHints))
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

        hintedReports = List.of();
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
            sb.append(i % 2 == 0 ? tileSizePx + "px " : gapSizePx + "px ");
        }
        return sb.toString();
    }

    private void updateBoardScale(int size) {
        if (size >= 11) {
            tileSizePx = 34;
            gapSizePx = 8;
        } else if (size >= 9) {
            tileSizePx = 42;
            gapSizePx = 8;
        } else {
            tileSizePx = 50;
            gapSizePx = 9;
        }
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
            hintedReports = List.of();
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
        AlgorithmReport report = gameProcessor.getLastAiReport();
        infoPanel.setReport(report, describeMove(report == null ? null : report.move()));
    }

    private void renderHistory() {
        historyPanel.render(gameProcessor.getMoveHistory());
    }

    private void renderStatistics() {
        Game game = gameProcessor.getGame();
        if (game == null) {
            infoPanel.setStatistics("Статистика появится после старта партии");
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

        infoPanel.setStatistics(statistics.toString());
    }

    private void renderPiece(Div cell, String text, String color) {
        Div piece = new Div();
        piece.setText(text);
        int pieceSize = Math.max(24, tileSizePx - 14);
        piece.getStyle()
                .set("width", pieceSize + "px")
                .set("height", pieceSize + "px")
                .set("border-radius", "50%")
                .set("background", color)
                .set("color", "white")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("font-size", "13px")
                .set("font-weight", "700")
                .set("box-shadow", "0 4px 10px rgba(15, 23, 42, 0.28)");
        cell.add(piece);
    }

    private List<AlgorithmReport> getMoveHintsForPosition(Position position) {
        List<AlgorithmReport> result = new ArrayList<>();
        for (AlgorithmReport report : hintedReports) {
            Move move = report.move();
            if (move != null
                    && move.getMoveType() == ru.ac.uniyar.model.enums.MoveType.MOVE_PLAYER
                    && position.equals(move.getEndPosition())) {
                result.add(report);
            }
        }
        return result;
    }

    private List<AlgorithmReport> getWallHintsForCell(int renderRow, int renderCol) {
        List<AlgorithmReport> result = new ArrayList<>();
        for (AlgorithmReport report : hintedReports) {
            Move move = report.move();
            if (move != null
                    && move.getMoveType() == ru.ac.uniyar.model.enums.MoveType.PLACE_WALL
                    && isRenderCellPartOfWall(renderRow, renderCol, move.getStartPosition(), move.getEndPosition())) {
                result.add(report);
            }
        }
        return result;
    }

    private boolean isRenderCellPartOfWall(int renderRow, int renderCol, Position start, Position end) {
        int row1 = start.row();
        int col1 = start.col();
        int row2 = end.row();
        int col2 = end.col();

        if (row1 == row2 && Math.abs(col1 - col2) == 1) {
            int wallRow = row1 * 2 + 1;
            int firstCol = Math.min(col1, col2) * 2;
            return renderRow == wallRow && renderCol >= firstCol && renderCol <= firstCol + 2;
        }

        if (col1 == col2 && Math.abs(row1 - row2) == 1) {
            int wallCol = col1 * 2 + 1;
            int firstRow = Math.min(row1, row2) * 2;
            return renderCol == wallCol && renderRow >= firstRow && renderRow <= firstRow + 2;
        }

        return false;
    }

    private boolean isWallCenter(Board board, int renderRow, int renderCol) {
        int row = renderRow / 2;
        int col = renderCol / 2;

        BoardTile topLeft = board.getTiles().get(new Position(row, col));
        BoardTile topRight = board.getTiles().get(new Position(row, col + 1));
        BoardTile bottomLeft = board.getTiles().get(new Position(row + 1, col));

        boolean horizontalWall = topLeft != null
                && topRight != null
                && !topLeft.isBackwardsMovementAvailable()
                && !topRight.isBackwardsMovementAvailable();
        boolean verticalWall = topLeft != null
                && bottomLeft != null
                && !topLeft.isRightMovementAvailable()
                && !bottomLeft.isRightMovementAvailable();

        return horizontalWall || verticalWall;
    }

    private String buildHintBackground(List<AlgorithmReport> reports) {
        if (reports.size() == 1) {
            return getAlgorithmColor(reports.get(0).algorithm());
        }
        StringBuilder gradient = new StringBuilder("linear-gradient(90deg");
        for (int i = 0; i < reports.size(); ++i) {
            int from = i * 100 / reports.size();
            int to = (i + 1) * 100 / reports.size();
            String color = getAlgorithmColor(reports.get(i).algorithm());
            gradient.append(", ").append(color).append(" ").append(from).append("% ").append(to).append("%");
        }
        gradient.append(")");
        return gradient.toString();
    }

    private String getAlgorithmColor(String algorithm) {
        if (algorithm.contains("MiniMax")) {
            return "#60a5fa";
        }
        if (algorithm.contains("AlphaBeta")) {
            return "#f59e0b";
        }
        if (algorithm.contains("MonteCarlo")) {
            return "#34d399";
        }
        return "#f472b6";
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
            return "P" + move.getPlayerId() + " -> " + formatPosition(move.getEndPosition());
        }
        return "P" + move.getPlayerId()
                + " стена "
                + formatPosition(move.getStartPosition())
                + " - "
                + formatPosition(move.getEndPosition());
    }

    private String formatPosition(Position position) {
        return "(" + (position.row() + 1) + ", " + (position.col() + 1) + ")";
    }
}
