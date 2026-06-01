package ru.ac.uniyar.service;

import org.springframework.stereotype.Service;
import ru.ac.uniyar.model.Board;
import ru.ac.uniyar.model.Game;
import ru.ac.uniyar.model.Move;
import ru.ac.uniyar.model.Position;
import ru.ac.uniyar.model.algorithms.EvaluationLearningSample;
import ru.ac.uniyar.model.algorithms.EvaluationTrainingUpdate;
import ru.ac.uniyar.model.algorithms.EvaluationWeights;
import ru.ac.uniyar.model.algorithms.EvaluationWeightsStore;
import ru.ac.uniyar.model.enums.GameSize;
import ru.ac.uniyar.model.enums.MoveType;
import ru.ac.uniyar.model.players.ComputerPlayer;
import ru.ac.uniyar.model.players.ComputerPlayerFabric;
import ru.ac.uniyar.model.players.Player;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
public class SelfPlayTrainingService {
    private final ComputerPlayerFabric computerPlayerFabric;
    private final BoardFactory boardFactory;

    public SelfPlayTrainingService(ComputerPlayerFabric computerPlayerFabric, BoardFactory boardFactory) {
        this.computerPlayerFabric = computerPlayerFabric;
        this.boardFactory = boardFactory;
    }

    public SelfPlayTrainingResult train(String algorithm1, String algorithm2,
                                        String hardness1, String hardness2,
                                        GameSize gameSize, int games,
                                        Consumer<String> logger) {
        EvaluationWeights before = EvaluationWeightsStore.current();
        int wins1 = 0;
        int wins2 = 0;
        int draws = 0;
        int updatedGames = 0;
        int totalMoves = 0;
        int moveLimit = 300;

        logger.accept("Старт self-play обучения: " + algorithm1 + " vs " + algorithm2);
        logger.accept("Поле: " + gameSize.getDescription()
                + ", P1 сложность: " + hardness1
                + ", P2 сложность: " + hardness2
                + ", партий: " + games);

        for (int i = 0; i < games; ++i) {
            int gameNumber = i + 1;
            int gamesLeftAfterCurrent = games - gameNumber;
            logger.accept("Партия " + gameNumber + "/" + games
                    + ": начинает P" + (i % 2 == 0 ? 1 : 2)
                    + ", после нее останется партий: " + gamesLeftAfterCurrent);
            Game game = createGame(algorithm1, algorithm2, hardness1, hardness2, gameSize, i % 2 == 0 ? 1 : 2);
            List<EvaluationLearningSample> samples = new ArrayList<>();
            int winner = play(game, moveLimit, samples, logger, gameNumber, games, gamesLeftAfterCurrent);
            totalMoves += game.getAmountOfMoves();

            if (winner == 1) {
                wins1++;
                logger.accept("  Победил P1 за " + game.getAmountOfMoves() + " ходов");
            } else if (winner == 2) {
                wins2++;
                logger.accept("  Победил P2 за " + game.getAmountOfMoves() + " ходов");
            } else {
                draws++;
                logger.accept("  Ничья/лимит после " + game.getAmountOfMoves() + " ходов");
            }

            if (winner != 0) {
                String source = "self-play " + algorithm1 + "(" + hardness1 + ") vs "
                        + algorithm2 + "(" + hardness2 + "), game " + gameNumber + "/" + games;
                EvaluationTrainingUpdate update = EvaluationWeightsStore.learnFromGame(samples, winner, source);
                if (update.applied()) {
                    updatedGames++;
                    logger.accept("  Веса обновлены: обучающих ходов " + update.sampleCount());
                } else {
                    logger.accept("  Веса не изменены: нулевой градиент или нет обучающих ходов");
                }
            }

            logger.accept("  Текущий счет: P1=" + wins1 + ", P2=" + wins2 + ", ничьи=" + draws);
            logger.accept("  Прогресс обучения: завершено " + gameNumber + "/" + games
                    + ", осталось партий: " + gamesLeftAfterCurrent
                    + ", партий с обновлением весов: " + updatedGames);
        }

        EvaluationWeights after = EvaluationWeightsStore.current();
        SelfPlayTrainingResult result = new SelfPlayTrainingResult(
                games,
                updatedGames,
                wins1,
                wins2,
                draws,
                games == 0 ? 0 : totalMoves * 1.0 / games,
                before,
                after
        );
        logger.accept("Итог:\n" + summary(result));
        return result;
    }

    public String summary(SelfPlayTrainingResult result) {
        return "Партий: " + result.games()
                + "\nПартий с обновлением весов: " + result.updatedGames()
                + "\nПобеды P1: " + result.wins1()
                + "\nПобеды P2: " + result.wins2()
                + "\nНичьи/лимит: " + result.draws()
                + "\nСредняя длина партии: " + String.format("%.1f", result.averageMoves())
                + "\nВеса до: " + describe(result.before())
                + "\nВеса после: " + describe(result.after());
    }

    private String describe(EvaluationWeights weights) {
        return "path=" + weights.pathAdvantage()
                + ", myMob=" + weights.myMobility()
                + ", enemyMob=" + weights.enemyMobility()
                + ", wall=" + weights.wallAdvantage()
                + ", progress=" + weights.progressAdvantage()
                + ", myEnd=" + weights.myEndgame()
                + ", enemyEnd=" + weights.enemyEndgame()
                + ", samples=" + weights.samples();
    }

    private Game createGame(String algorithm1, String algorithm2, String hardness1, String hardness2,
                            GameSize gameSize, int firstPlayer) {
        Player player1 = computerPlayerFabric.getComputerPlayer(algorithm1, hardness1);
        player1.setPlayerId(1);
        player1.setAmountOfWallsLeft(gameSize.getAmountOfWalls());

        Player player2 = computerPlayerFabric.getComputerPlayer(algorithm2, hardness2);
        player2.setPlayerId(2);
        player2.setAmountOfWallsLeft(gameSize.getAmountOfWalls());

        return new Game(gameSize, boardFactory.create(gameSize), player1, player2, firstPlayer, Instant.now(), 0, false);
    }

    private int play(Game game, int moveLimit, List<EvaluationLearningSample> samples,
                     Consumer<String> logger, int gameNumber, int totalGames, int gamesLeftAfterCurrent) {
        Map<Integer, List<Position>> recentPositions = new HashMap<>();
        recentPositions.put(1, new ArrayList<>(List.of(game.getBoard().getPositionOfPlayer1())));
        recentPositions.put(2, new ArrayList<>(List.of(game.getBoard().getPositionOfPlayer2())));

        while (!game.isFinished() && game.getAmountOfMoves() < moveLimit) {
            Player player = game.getCurrentPlayer() == 1 ? game.getPlayer1() : game.getPlayer2();
            ComputerPlayer computerPlayer = (ComputerPlayer) player;
            computerPlayer.getAlgorithm().setRecentPositions(recentPositions.get(player.getPlayerId()));

            Board before = game.getBoard().copy();
            int wallsLeft1 = game.getPlayer1().getAmountOfWallsLeft();
            int wallsLeft2 = game.getPlayer2().getAmountOfWallsLeft();
            Move move = player.getMove(game.getBoard(), player.getPlayerId(), wallsLeft1, wallsLeft2);
            if (move == null || !isLegalMove(game, move, player)) {
                logger.accept("  Партия " + gameNumber + "/" + totalGames
                        + ": P" + player.getPlayerId() + " не смог сделать допустимый ход");
                return 0;
            }

            EvaluationLearningSample sample = computerPlayer.getAlgorithm().buildLearningSample(
                    before,
                    move,
                    player.getPlayerId(),
                    game.getGameSize().getAmountOfTilesPerSide(),
                    wallsLeft1,
                    wallsLeft2
            );
            if (sample != null) {
                samples.add(sample);
            }

            game.applyMove(move);
            if (move.getMoveType() == MoveType.MOVE_PLAYER) {
                List<Position> positions = recentPositions.get(move.getPlayerId());
                positions.add(0, move.getEndPosition());
                if (positions.size() > 6) {
                    positions.remove(positions.size() - 1);
                }
            }
            logGameProgress(game, moveLimit, logger, gameNumber, totalGames, gamesLeftAfterCurrent, move);

            if (!game.isFinished()) {
                game.setCurrentPlayer(game.getCurrentPlayer() == 1 ? 2 : 1);
            }
        }

        if (game.isWonBy(1)) return 1;
        if (game.isWonBy(2)) return 2;
        return 0;
    }

    private void logGameProgress(Game game, int moveLimit, Consumer<String> logger, int gameNumber, int totalGames,
                                 int gamesLeftAfterCurrent, Move move) {
        int moveNumber = game.getAmountOfMoves();
        if (moveNumber <= 5 || moveNumber % 10 == 0 || game.isFinished() || moveNumber >= moveLimit) {
            logger.accept("  Партия " + gameNumber + "/" + totalGames
                    + ", ход " + moveNumber + "/" + moveLimit
                    + ", до лимита партии: " + Math.max(0, moveLimit - moveNumber)
                    + ", после партии останется партий: " + gamesLeftAfterCurrent
                    + ", последний ход: P" + move.getPlayerId());
        }
    }

    private boolean isLegalMove(Game game, Move move, Player player) {
        if (move.getPlayerId() != player.getPlayerId()) {
            return false;
        }
        if (move.getMoveType() == MoveType.PLACE_WALL) {
            if (!player.canPlaceWall()) {
                return false;
            }
            try {
                Board copy = game.getBoard().copy();
                copy.placeWall(move.getStartPosition(), move.getEndPosition());
                int size = game.getGameSize().getAmountOfTilesPerSide();
                return hasPath(copy, copy.getPositionOfPlayer1(), 0)
                        && hasPath(copy, copy.getPositionOfPlayer2(), size - 1);
            } catch (Exception e) {
                return false;
            }
        }

        Position currentPosition = player.getPlayerId() == 1
                ? game.getBoard().getPositionOfPlayer1()
                : game.getBoard().getPositionOfPlayer2();
        return game.getBoard().getAvailableMoves(currentPosition).contains(move.getEndPosition());
    }

    private boolean hasPath(Board board, Position start, int targetRow) {
        java.util.Set<Position> visited = new java.util.HashSet<>();
        java.util.Queue<Position> queue = new java.util.ArrayDeque<>();
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            Position current = queue.poll();
            if (current.row() == targetRow) {
                return true;
            }
            for (Position next : board.getPathNeighbors(current)) {
                if (visited.add(next)) {
                    queue.add(next);
                }
            }
        }
        return false;
    }
}
