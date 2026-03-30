package ru.ac.uniyar.service;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;
import ru.ac.uniyar.model.*;
import ru.ac.uniyar.model.enums.GameSize;
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

    private Game game;

    public Board initBoard(GameSize gameSize) {
        Board board = new Board();
        int size = gameSize.getAmountOfTilesPerSide();

        for (int i = 0; i < size; ++i) {
            for (int j = 0; j < size; ++j) {
                board.getTiles().put(i + "" + j, new BoardTile(
                        j != 0,
                        i != 0,
                        j != size - 1,
                        i != size - 1
                ));
            }
        }

        int mid = (size - 1) / 2;
        board.setPositionOfPlayer1((size - 1) + "" + mid);
        board.setPositionOfPlayer2("0" + mid);

        return board;
    }

    public void startNewGame(String sizeDescription, String typeOfPlayer1, String typeOfPlayer2, String gameHardness) {
        GameSize gameSize = GameSize.findByDescription(sizeDescription);

        Player player1 = typeOfPlayer1.equals("Игрок")
                ? new HumanPlayer()
                : computerPlayerFabric.getComputerPlayer(typeOfPlayer1, gameHardness);
        player1.setPlayerId(1);
        player1.setAmountOfWallsLeft(gameSize.getAmountOfWalls());

        Player player2 = computerPlayerFabric.getComputerPlayer(typeOfPlayer2, gameHardness);
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
    }

    public void makeMove(Move move) {
        if (game.isFinished()) return;
        game.applyMove(move);
        game.setCurrentPlayer(game.getCurrentPlayer() == 1 ? 2 : 1);
    }

    public Player getCurrentPlayer() {
        return game.getCurrentPlayer() == 1 ? game.getPlayer1() : game.getPlayer2();
    }

    public boolean canPlaceWall(int i1, int j1, int i2, int j2) {
        if (game == null) return false;

        Board copy = game.getBoard().copy();

        try {
            copy.placeWall(i1 + "" + j1, i2 + "" + j2);
        } catch (Exception e) {
            return false;
        }

        return hasPath(copy, copy.getPositionOfPlayer1(), 1)
                && hasPath(copy, copy.getPositionOfPlayer2(), 2);
    }

    public boolean tryPlaceWall(int i1, int j1, int i2, int j2) {
        if (game == null) return false;

        Player player = getCurrentPlayer();
        if (!(player instanceof HumanPlayer)) return false;
        if (!player.canPlaceWall()) return false;

        if (!canPlaceWall(i1, j1, i2, j2)) return false;

        makeMove(Move.placeWall(player.getPlayerId(), i1 + "" + j1, i2 + "" + j2));
        return true;
    }

    private boolean hasPath(Board board, String start, int playerId) {
        int size = game.getGameSize().getAmountOfTilesPerSide();
        int targetRow = (playerId == 1) ? 0 : size - 1;

        Set<String> visited = new HashSet<>();
        Queue<String> q = new LinkedList<>();
        q.add(start);
        visited.add(start);

        while (!q.isEmpty()) {
            String cur = q.poll();
            int i = cur.charAt(0) - '0';

            if (i == targetRow) return true;

            for (String next : board.getAvailableMoves(cur)) {
                if (visited.add(next)) q.add(next);
            }
        }
        return false;
    }
}