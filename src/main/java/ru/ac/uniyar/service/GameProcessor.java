package ru.ac.uniyar.service;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;
import ru.ac.uniyar.model.*;
import ru.ac.uniyar.model.algorithms.Algorithm;
import ru.ac.uniyar.model.algorithms.AlphaBetaAlgorithm;
import ru.ac.uniyar.model.algorithms.MinimaxAlgorithm;
import ru.ac.uniyar.model.algorithms.MonteCarloAlgorithm;
import ru.ac.uniyar.model.algorithms.RandomAlgorithm;
import ru.ac.uniyar.model.enums.GameSize;
import ru.ac.uniyar.model.algorithms.AlgorithmReport;
import ru.ac.uniyar.model.enums.ComputerPlayerHardnessLevel;
import ru.ac.uniyar.model.enums.MoveType;
import ru.ac.uniyar.model.players.ComputerPlayer;
import ru.ac.uniyar.model.players.ComputerPlayerFabric;
import ru.ac.uniyar.model.players.HumanPlayer;
import ru.ac.uniyar.model.players.Player;

import java.time.Instant;
import java.util.*;

@Service
@Getter
@SessionScope
public class GameProcessor {

    @Autowired
    private ComputerPlayerFabric computerPlayerFabric;
    @Autowired
    private GameRules gameRules;
    @Autowired
    private BoardFactory boardFactory;
    @Autowired
    private BoardAnalyzer boardAnalyzer;

    private Game game;
    private final List<Move> moveHistory = new ArrayList<>();
    private final List<Board> boardHistory = new ArrayList<>();
    private Integer replayIndex;
    private AlgorithmReport lastAiReport;

    public void startNewGame(String sizeDescription, String typeOfPlayer1, String typeOfPlayer2,
                             String hardness1, String hardness2) {
        GameSize gameSize = GameSize.findByDescription(sizeDescription);

        Player player1 = typeOfPlayer1.equals("Игрок")
                ? new HumanPlayer()
                : computerPlayerFabric.getComputerPlayer(typeOfPlayer1, hardness1);
        player1.setPlayerId(1);
        player1.setAmountOfWallsLeft(gameSize.getAmountOfWalls());

        Player player2 = computerPlayerFabric.getComputerPlayer(typeOfPlayer2, hardness2);
        player2.setPlayerId(2);
        player2.setAmountOfWallsLeft(gameSize.getAmountOfWalls());

        Board board = boardFactory.create(gameSize);

        game = new Game(
                gameSize,
                board,
                player1,
                player2,
                1,
                Instant.now(),
                0,
                false
        );

        game.setCurrentPlayer(Math.random() > 0.5 ? 1 : 2);
        moveHistory.clear();
        boardHistory.clear();
        boardHistory.add(board.copy());
        replayIndex = null;
        lastAiReport = null;
    }

    public void makeMove(Move move) {
        if (game.isFinished()) return;
        replayIndex = null;
        game.applyMove(move);
        moveHistory.add(move);
        boardHistory.add(game.getBoard().copy());
        game.setCurrentPlayer(game.getCurrentPlayer() == 1 ? 2 : 1);
    }

    public void makeComputerMove() {
        if (!(getCurrentPlayer() instanceof ComputerPlayer computerPlayer)) {
            return;
        }

        Board before = game.getBoard().copy();
        computerPlayer.getAlgorithm().setRecentPositions(getRecentPositions(computerPlayer.getPlayerId()));
        Move move = computerPlayer.getMove(
                game.getBoard(),
                computerPlayer.getPlayerId(),
                game.getPlayer1().getAmountOfWallsLeft(),
                game.getPlayer2().getAmountOfWallsLeft()
        );
        AlgorithmReport rawReport = computerPlayer.getLastReport();
        Move legalMove = ensureLegalComputerMove(move, computerPlayer);

        lastAiReport = enrichReport(adjustReportForFallback(rawReport, move, legalMove), before, computerPlayer.getPlayerId());

        if (legalMove != null) {
            makeMove(legalMove);
        }
    }

    public List<AlgorithmReport> getHintsFromAllAlgorithms() {
        if (game == null || game.isFinished()) {
            return List.of();
        }

        int playerId = getCurrentPlayer().getPlayerId();
        Board base = game.getBoard().copy();
        List<Algorithm> algorithms = List.of(
                new RandomAlgorithm(),
                new MinimaxAlgorithm(),
                new AlphaBetaAlgorithm(),
                new MonteCarloAlgorithm()
        );
        ComputerPlayerHardnessLevel hintHardness = getHintHardnessLevel();

        List<AlgorithmReport> reports = new ArrayList<>();
        for (Algorithm algorithm : algorithms) {
            Move move = algorithm.getMove(
                    base.copy(),
                    hintHardness,
                    playerId,
                    game.getPlayer1().getAmountOfWallsLeft(),
                    game.getPlayer2().getAmountOfWallsLeft()
            );
            AlgorithmReport report = algorithm.getLastReport();
            if (report != null) {
                reports.add(enrichReport(new AlgorithmReport(
                        report.algorithm(),
                        move,
                        report.score(),
                        report.reachedDepth(),
                        report.nodesVisited(),
                        report.consideredMoves(),
                        report.cutoffs(),
                        report.tableHits(),
                        report.timeMs(),
                        report.explanation()
                ), base, playerId));
            }
        }
        return reports;
    }

    private ComputerPlayerHardnessLevel getHintHardnessLevel() {
        Player opponent = game.getCurrentPlayer() == 1 ? game.getPlayer2() : game.getPlayer1();
        if (opponent instanceof ComputerPlayer computerPlayer) {
            return computerPlayer.getHardnessLevel();
        }
        return ComputerPlayerHardnessLevel.MEDIUM;
    }

    public Player getCurrentPlayer() {
        return game.getCurrentPlayer() == 1 ? game.getPlayer1() : game.getPlayer2();
    }

    public boolean canPlaceWall(Position start, Position end) {
        return gameRules.canPlaceWall(game, start, end);
    }

    public boolean tryPlaceWall(int i1, int j1, int i2, int j2) {
        if (game == null) return false;

        Player player = getCurrentPlayer();
        if (!(player instanceof HumanPlayer)) return false;
        if (!player.canPlaceWall()) return false;

        Position start = new Position(i1, j1);
        Position end = new Position(i2, j2);
        if (!canPlaceWall(start, end)) return false;

        makeMove(Move.placeWall(player.getPlayerId(), start, end));
        return true;
    }

    public Position[] extractWall(int renderRow, int renderCol) {
        return gameRules.extractWall(game, renderRow, renderCol);
    }

    public Board getBoardForDisplay() {
        if (replayIndex != null && replayIndex >= 0 && replayIndex < boardHistory.size()) {
            return boardHistory.get(replayIndex);
        }
        return game == null ? null : game.getBoard();
    }

    public boolean isReplayMode() {
        return replayIndex != null;
    }

    public int getDisplayedMoveNumber() {
        return replayIndex == null ? moveHistory.size() : replayIndex;
    }

    public void replayPrevious() {
        if (boardHistory.isEmpty()) return;
        replayIndex = replayIndex == null ? Math.max(0, boardHistory.size() - 2) : Math.max(0, replayIndex - 1);
    }

    public void replayNext() {
        if (replayIndex == null) return;
        replayIndex++;
        if (replayIndex >= boardHistory.size() - 1) {
            replayIndex = null;
        }
    }

    public void replayLive() {
        replayIndex = null;
    }

    private AlgorithmReport enrichReport(AlgorithmReport report, Board before, int playerId) {
        if (report == null) {
            return null;
        }
        String explanation = report.explanation() + ". "
                + boardAnalyzer.explainMove(before, report.move(), playerId, game.getGameSize().getAmountOfTilesPerSide());
        return new AlgorithmReport(
                report.algorithm(),
                report.move(),
                report.score(),
                report.reachedDepth(),
                report.nodesVisited(),
                report.consideredMoves(),
                report.cutoffs(),
                report.tableHits(),
                report.timeMs(),
                explanation
        );
    }

    private Move ensureLegalComputerMove(Move move, ComputerPlayer player) {
        if (isLegalMove(move, player)) {
            return move;
        }
        Position currentPosition = player.getPlayerId() == 1
                ? game.getBoard().getPositionOfPlayer1()
                : game.getBoard().getPositionOfPlayer2();
        List<Position> fallbackMoves = game.getBoard().getAvailableMoves(currentPosition);
        if (fallbackMoves.isEmpty()) {
            return null;
        }
        Position fallback = fallbackMoves.stream()
                .max(Comparator.comparingInt(position -> scoreFallbackPosition(position, currentPosition, player.getPlayerId())))
                .orElse(fallbackMoves.get(0));
        return Move.movePlayer(player.getPlayerId(), fallback);
    }

    private boolean isLegalMove(Move move, Player player) {
        if (move == null || move.getPlayerId() != player.getPlayerId()) {
            return false;
        }
        if (move.getMoveType() == MoveType.PLACE_WALL) {
            return player.canPlaceWall() && gameRules.canPlaceWall(game, move.getStartPosition(), move.getEndPosition());
        }
        Position currentPosition = player.getPlayerId() == 1
                ? game.getBoard().getPositionOfPlayer1()
                : game.getBoard().getPositionOfPlayer2();
        return game.getBoard().getAvailableMoves(currentPosition).contains(move.getEndPosition());
    }

    private AlgorithmReport adjustReportForFallback(AlgorithmReport report, Move originalMove, Move legalMove) {
        if (report == null || Objects.equals(originalMove, legalMove)) {
            return report;
        }
        return new AlgorithmReport(
                report.algorithm(),
                legalMove,
                report.score(),
                report.reachedDepth(),
                report.nodesVisited(),
                report.consideredMoves(),
                report.cutoffs(),
                report.tableHits(),
                report.timeMs(),
                report.explanation() + ". Алгоритм вернул недопустимый ход, поэтому применен лучший доступный ход фишкой"
        );
    }

    private int scoreFallbackPosition(Position position, Position currentPosition, int playerId) {
        int size = game.getGameSize().getAmountOfTilesPerSide();
        int targetRow = playerId == 1 ? 0 : size - 1;
        int currentDistance = Math.abs(currentPosition.row() - targetRow);
        int nextDistance = Math.abs(position.row() - targetRow);
        int score = (currentDistance - nextDistance) * 100;

        int direction = playerId == 1 ? -1 : 1;
        int rowDelta = position.row() - currentPosition.row();
        if (rowDelta == direction) {
            score += 60;
        } else if (rowDelta == -direction) {
            score -= 180;
        }

        List<Position> recent = getRecentPositions(playerId);
        for (int index = 0; index < recent.size(); ++index) {
            Position recentPosition = recent.get(index);
            if (recentPosition.equals(currentPosition)) {
                continue;
            }
            if (recentPosition.equals(position)) {
                score -= index <= 1 ? 700 : 220;
            }
        }
        return score;
    }

    private List<Position> getRecentPositions(int playerId) {
        List<Position> positions = new ArrayList<>();
        positions.add(playerId == 1 ? game.getBoard().getPositionOfPlayer1() : game.getBoard().getPositionOfPlayer2());
        for (int index = moveHistory.size() - 1; index >= 0 && positions.size() < 6; --index) {
            Move move = moveHistory.get(index);
            if (move.getPlayerId() == playerId && move.getMoveType() == MoveType.MOVE_PLAYER) {
                positions.add(move.getEndPosition());
            }
        }
        return positions;
    }
}
