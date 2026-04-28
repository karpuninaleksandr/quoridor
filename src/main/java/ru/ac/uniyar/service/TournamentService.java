package ru.ac.uniyar.service;

import org.springframework.stereotype.Service;
import ru.ac.uniyar.model.*;
import ru.ac.uniyar.model.enums.ComputerAlgorithmType;
import ru.ac.uniyar.model.enums.GameSize;
import ru.ac.uniyar.model.players.ComputerPlayer;
import ru.ac.uniyar.model.players.ComputerPlayerFabric;
import ru.ac.uniyar.model.players.Player;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
public class TournamentService {
    private final ComputerPlayerFabric computerPlayerFabric;
    private final BoardFactory boardFactory;

    public TournamentService(ComputerPlayerFabric computerPlayerFabric, BoardFactory boardFactory) {
        this.computerPlayerFabric = computerPlayerFabric;
        this.boardFactory = boardFactory;
    }

    public String runTournament(String algorithm1, String algorithm2, String hardness, GameSize gameSize, int games) {
        return runTournament(algorithm1, algorithm2, hardness, hardness, gameSize, games, ignored -> {
        });
    }

    public String runTournament(String algorithm1, String algorithm2, String hardness, GameSize gameSize,
                                int games, Consumer<String> logger) {
        return runTournament(algorithm1, algorithm2, hardness, hardness, gameSize, games, logger);
    }

    public String runTournament(String algorithm1, String algorithm2, String hardness1, String hardness2,
                                GameSize gameSize, int games, Consumer<String> logger) {
        int wins1 = 0;
        int wins2 = 0;
        int draws = 0;
        int totalMoves = 0;
        int moveLimit = 300;

        logger.accept("Старт турнира: " + algorithm1 + " vs " + algorithm2);
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
            int winner = play(game, moveLimit, logger, gameNumber, games, gamesLeftAfterCurrent);
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
            logger.accept("  Текущий счет: P1=" + wins1 + ", P2=" + wins2 + ", ничьи=" + draws);
            logger.accept("  Прогресс турнира: завершено " + gameNumber + "/" + games
                    + ", осталось партий: " + gamesLeftAfterCurrent);
        }

        String summary = "Турнир: " + algorithm1 + " vs " + algorithm2
                + "\nСложность P1: " + hardness1
                + "\nСложность P2: " + hardness2
                + "\nПартий: " + games
                + "\nПобеды P1: " + wins1
                + "\nПобеды P2: " + wins2
                + "\nНичьи/лимит ходов: " + draws
                + "\nСредняя длина партии: " + (games == 0 ? 0 : totalMoves * 1.0 / games);
        logger.accept("Итог:\n" + summary);
        return summary;
    }

    public List<AlgorithmComparisonResult> compareAlgorithms(String hardness1, String hardness2,
                                                             GameSize gameSize, int games,
                                                             Consumer<String> logger) {
        Map<String, String> hardnessByAlgorithm = Map.of(
                ComputerAlgorithmType.RANDOM.getDescription(), hardness1,
                ComputerAlgorithmType.MINIMAX.getDescription(), hardness1,
                ComputerAlgorithmType.MONTECARLO.getDescription(), hardness2,
                ComputerAlgorithmType.ALPHABETA.getDescription(), hardness2
        );
        return compareAlgorithms(hardnessByAlgorithm, gameSize, games, logger);
    }

    public List<AlgorithmComparisonResult> compareAlgorithms(Map<String, String> hardnessByAlgorithm,
                                                             GameSize gameSize, int games,
                                                             Consumer<String> logger) {
        List<AlgorithmComparisonResult> results = new ArrayList<>();
        ComputerAlgorithmType[] algorithms = ComputerAlgorithmType.values();
        int totalPairs = algorithms.length * (algorithms.length - 1) / 2;
        int pairNumber = 0;

        for (int firstIndex = 0; firstIndex < algorithms.length; ++firstIndex) {
            for (int secondIndex = firstIndex + 1; secondIndex < algorithms.length; ++secondIndex) {
                ComputerAlgorithmType first = algorithms[firstIndex];
                ComputerAlgorithmType second = algorithms[secondIndex];
                pairNumber++;
                logger.accept("Сравнение " + pairNumber + "/" + totalPairs + ": "
                        + first.getDescription() + " vs " + second.getDescription());
                results.add(runComparisonPair(
                        first.getDescription(),
                        second.getDescription(),
                        hardnessByAlgorithm.get(first.getDescription()),
                        hardnessByAlgorithm.get(second.getDescription()),
                        gameSize,
                        games
                ));
            }
        }
        return results;
    }

    private AlgorithmComparisonResult runComparisonPair(String algorithm1, String algorithm2,
                                                        String hardness1, String hardness2,
                                                        GameSize gameSize, int games) {
        int wins1 = 0;
        int wins2 = 0;
        int draws = 0;
        int totalMoves = 0;

        for (int i = 0; i < games; ++i) {
            Game game = createGame(algorithm1, algorithm2, hardness1, hardness2, gameSize, i % 2 == 0 ? 1 : 2);
            int winner = play(game, 300, ignored -> {
            }, i + 1, games, games - i - 1);
            totalMoves += game.getAmountOfMoves();
            if (winner == 1) {
                wins1++;
            } else if (winner == 2) {
                wins2++;
            } else {
                draws++;
            }
        }

        return new AlgorithmComparisonResult(
                algorithm1,
                algorithm2,
                games,
                wins1,
                wins2,
                draws,
                games == 0 ? 0 : totalMoves * 1.0 / games
        );
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

    private int play(Game game, int moveLimit, Consumer<String> logger, int gameNumber, int totalGames,
                     int gamesLeftAfterCurrent) {
        while (!game.isFinished() && game.getAmountOfMoves() < moveLimit) {
            Player player = game.getCurrentPlayer() == 1 ? game.getPlayer1() : game.getPlayer2();
            Move move = ((ComputerPlayer) player).getMove(
                    game.getBoard(),
                    player.getPlayerId(),
                    game.getPlayer1().getAmountOfWallsLeft(),
                    game.getPlayer2().getAmountOfWallsLeft()
            );
            if (move == null) {
                logger.accept("  Партия " + gameNumber + "/" + totalGames
                        + ": ИИ P" + player.getPlayerId() + " не нашел ход");
                return 0;
            }
            game.applyMove(move);
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
}
