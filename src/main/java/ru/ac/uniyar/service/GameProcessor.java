package ru.ac.uniyar.service;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;
import ru.ac.uniyar.model.*;
import ru.ac.uniyar.model.algorithms.AlphaBetaAlgorithm;
import ru.ac.uniyar.model.enums.GameSize;
import ru.ac.uniyar.model.algorithms.AlgorithmReport;
import ru.ac.uniyar.model.enums.ComputerPlayerHardnessLevel;
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

    private Game game;
    private final List<Move> moveHistory = new ArrayList<>();
    private final List<Board> boardHistory = new ArrayList<>();
    private Integer replayIndex;
    private AlgorithmReport lastAiReport;

    public Board initBoard(GameSize gameSize) {
        Board board = new Board();
        int size = gameSize.getAmountOfTilesPerSide();

        for (int i = 0; i < size; ++i) {
            for (int j = 0; j < size; ++j) {
                board.getTiles().put(new Position(i, j), new BoardTile(
                        j != 0,
                        i != 0,
                        j != size - 1,
                        i != size - 1
                ));
            }
        }

        int mid = (size - 1) / 2;
        board.setPositionOfPlayer1(new Position(size - 1, mid));
        board.setPositionOfPlayer2(new Position(0, mid));

        return board;
    }

    public void startNewGame(String sizeDescription, String typeOfPlayer1, String typeOfPlayer2, String gameHardness) {
        startNewGame(sizeDescription, typeOfPlayer1, typeOfPlayer2, gameHardness, gameHardness);
    }

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

        Board board = initBoard(gameSize);

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

    public Move makeComputerMove() {
        if (!(getCurrentPlayer() instanceof ComputerPlayer computerPlayer)) {
            return null;
        }

        Move move = computerPlayer.getMove(
                game.getBoard(),
                computerPlayer.getPlayerId(),
                game.getPlayer1().getAmountOfWallsLeft(),
                game.getPlayer2().getAmountOfWallsLeft()
        );

        lastAiReport = computerPlayer.getLastReport();

        if (move != null) {
            makeMove(move);
        }
        return move;
    }

    public AlgorithmReport getHintForCurrentPlayer() {
        if (game == null || game.isFinished()) {
            return null;
        }

        AlphaBetaAlgorithm advisor = new AlphaBetaAlgorithm();
        Move move = advisor.getMove(
                game.getBoard().copy(),
                ComputerPlayerHardnessLevel.MEDIUM,
                getCurrentPlayer().getPlayerId(),
                game.getPlayer1().getAmountOfWallsLeft(),
                game.getPlayer2().getAmountOfWallsLeft()
        );
        AlgorithmReport report = advisor.getLastReport();
        if (report == null) {
            return null;
        }
        return new AlgorithmReport(
                "Подсказка: " + report.algorithm(),
                move,
                report.score(),
                report.reachedDepth(),
                report.nodesVisited(),
                report.consideredMoves(),
                report.cutoffs(),
                "Советник использует AlphaBeta средней глубины"
        );
    }

    public Player getCurrentPlayer() {
        return game.getCurrentPlayer() == 1 ? game.getPlayer1() : game.getPlayer2();
    }

    public boolean canPlaceWall(int i1, int j1, int i2, int j2) {
        if (game == null) return false;

        return canPlaceWall(new Position(i1, j1), new Position(i2, j2));
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
}
