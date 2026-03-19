package ru.ac.uniyar.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.ac.uniyar.model.Board;
import ru.ac.uniyar.model.BoardTile;
import ru.ac.uniyar.model.Game;
import ru.ac.uniyar.model.enums.GameSize;
import ru.ac.uniyar.model.players.ComputerPlayerFabric;
import ru.ac.uniyar.model.players.HumanPlayer;
import ru.ac.uniyar.model.players.Player;

import java.time.Instant;

@Service
public class GameProcessor {
    @Autowired
    private ComputerPlayerFabric computerPlayerFabric;
    private Game game;

    public Board initBoard(GameSize gameSize) {
        Board board = new Board();

        for (int i = 0; i < gameSize.getAmountOfTilesPerSide(); ++i) {
            for (int j = 0; j < gameSize.getAmountOfTilesPerSide(); ++j) {
                board.getTiles().put(i + "" + j, new BoardTile(
                        j != 0,
                        i != 0,
                        j != gameSize.getAmountOfWalls() - 1,
                        i != gameSize.getAmountOfTilesPerSide() - 1
                ));
            }
        }

        board.setPositionOfPlayer1("0".concat(String.valueOf((gameSize.getAmountOfTilesPerSide() - 1) / 2)));
        board.setPositionOfPlayer2(String.valueOf(gameSize.getAmountOfTilesPerSide() - 1).concat(String.valueOf((gameSize.getAmountOfTilesPerSide() - 1) / 2)));

        return board;
    }

    public void startNewGame(String sizeDescription, String typeOfPlayer1, String typeOfPlayer2, String gameHardness) {
        GameSize gameSize = GameSize.findByDescription(sizeDescription);
        Player player1 = typeOfPlayer1.equals("Игрок") ? new HumanPlayer() : computerPlayerFabric.getComputerPlayer(typeOfPlayer1, gameHardness);
        player1.setAmountOfWallsLeft(gameSize.getAmountOfWalls());
        Player player2 = computerPlayerFabric.getComputerPlayer(typeOfPlayer2, gameHardness);
        player2.setAmountOfWallsLeft(gameSize.getAmountOfWalls());

        Board board = initBoard(gameSize);

        game = new Game(
                gameSize,
                board,
                player1,
                player2,
                Instant.now(),
                0,
                false
        );

        initGame();
    }

    private void initGame() {
        int currentPlayer = Math.random() > 0.5 ? 2 : 1;

        while (!game.isFinished()) {
            if ((currentPlayer == 1)) {
                game.applyMove(game.getPlayer1().getMove(game.getBoard()));
                currentPlayer = 2;
            } else {
                game.applyMove(game.getPlayer2().getMove(game.getBoard()));
                currentPlayer = 1;
            }
            //todo обновить отображение
        }
        //todo отобразить статистику игры
    }
}