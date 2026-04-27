package ru.ac.uniyar.service;

import org.springframework.stereotype.Service;
import ru.ac.uniyar.model.*;
import ru.ac.uniyar.model.enums.GameSize;
import ru.ac.uniyar.model.players.ComputerPlayer;
import ru.ac.uniyar.model.players.ComputerPlayerFabric;
import ru.ac.uniyar.model.players.Player;

import java.time.Instant;
import java.util.function.Consumer;

@Service
public class TournamentService {
    private final ComputerPlayerFabric computerPlayerFabric;

    public TournamentService(ComputerPlayerFabric computerPlayerFabric) {
        this.computerPlayerFabric = computerPlayerFabric;
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

    private Game createGame(String algorithm1, String algorithm2, String hardness1, String hardness2,
                            GameSize gameSize, int firstPlayer) {
        Player player1 = computerPlayerFabric.getComputerPlayer(algorithm1, hardness1);
        player1.setPlayerId(1);
        player1.setAmountOfWallsLeft(gameSize.getAmountOfWalls());

        Player player2 = computerPlayerFabric.getComputerPlayer(algorithm2, hardness2);
        player2.setPlayerId(2);
        player2.setAmountOfWallsLeft(gameSize.getAmountOfWalls());

        return new Game(gameSize, initBoard(gameSize), player1, player2, firstPlayer, Instant.now(), 0, false);
    }

    private Board initBoard(GameSize gameSize) {
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
