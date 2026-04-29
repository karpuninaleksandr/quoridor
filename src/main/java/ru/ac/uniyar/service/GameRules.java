package ru.ac.uniyar.service;

import org.springframework.stereotype.Service;
import ru.ac.uniyar.model.Board;
import ru.ac.uniyar.model.Game;
import ru.ac.uniyar.model.Position;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

@Service
public class GameRules {
    public Position[] extractWall(Game game, int renderRow, int renderCol) {
        if (game == null) return null;

        int boardRow = renderRow / 2;
        int boardCol = renderCol / 2;
        int size = game.getGameSize().getAmountOfTilesPerSide();

        if (renderRow % 2 == 1 && renderCol % 2 == 0) {
            if (boardCol + 1 >= size || boardRow + 1 >= size) return null;
            return new Position[]{new Position(boardRow, boardCol), new Position(boardRow, boardCol + 1)};
        }

        if (renderRow % 2 == 0 && renderCol % 2 == 1) {
            if (boardRow + 1 >= size || boardCol + 1 >= size) return null;
            return new Position[]{new Position(boardRow, boardCol), new Position(boardRow + 1, boardCol)};
        }

        return null;
    }

    public boolean canPlaceWall(Game game, Position start, Position end) {
        if (game == null) return false;

        Board copy = game.getBoard().copy();

        try {
            copy.placeWall(start, end);
        } catch (Exception e) {
            return false;
        }

        int size = game.getGameSize().getAmountOfTilesPerSide();
        return hasPath(copy, copy.getPositionOfPlayer1(), 1, size)
                && hasPath(copy, copy.getPositionOfPlayer2(), 2, size);
    }

    private boolean hasPath(Board board, Position start, int playerId, int size) {
        int targetRow = (playerId == 1) ? 0 : size - 1;

        Set<Position> visited = new HashSet<>();
        Queue<Position> queue = new LinkedList<>();
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            Position cur = queue.poll();

            if (cur.row() == targetRow) return true;

            for (Position next : board.getPathNeighbors(cur)) {
                if (visited.add(next)) {
                    queue.add(next);
                }
            }
        }
        return false;
    }
}
