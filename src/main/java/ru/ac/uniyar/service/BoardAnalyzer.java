package ru.ac.uniyar.service;

import org.springframework.stereotype.Service;
import ru.ac.uniyar.model.Board;
import ru.ac.uniyar.model.Move;
import ru.ac.uniyar.model.Position;
import ru.ac.uniyar.model.enums.MoveType;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

@Service
public class BoardAnalyzer {
    public int shortestPath(Board board, int playerId, int size) {
        Position start = playerId == 1 ? board.getPositionOfPlayer1() : board.getPositionOfPlayer2();
        int targetRow = playerId == 1 ? 0 : size - 1;
        Set<Position> visited = new HashSet<>();
        Queue<Position> queue = new ArrayDeque<>();
        queue.add(start);
        visited.add(start);

        int depth = 0;
        while (!queue.isEmpty()) {
            for (int i = queue.size(); i > 0; --i) {
                Position current = queue.poll();
                if (current.row() == targetRow) {
                    return depth;
                }
                for (Position next : board.getAvailableMoves(current)) {
                    if (visited.add(next)) {
                        queue.add(next);
                    }
                }
            }
            depth++;
        }
        return 1000;
    }

    public String explainMove(Board before, Move move, int playerId, int size) {
        if (move == null) {
            return "Алгоритм не нашел допустимый ход";
        }

        int myBefore = shortestPath(before, playerId, size);
        int enemyBefore = shortestPath(before, 3 - playerId, size);
        Board after = before.copy();
        applyMove(after, move);
        int myAfter = shortestPath(after, playerId, size);
        int enemyAfter = shortestPath(after, 3 - playerId, size);

        int myDelta = myAfter - myBefore;
        int enemyDelta = enemyAfter - enemyBefore;
        StringBuilder explanation = new StringBuilder();
        if (move.getMoveType() == MoveType.MOVE_PLAYER) {
            explanation.append("Ход фишкой ");
            if (myDelta < 0) {
                explanation.append("сокращает путь до цели на ").append(-myDelta);
            } else if (myDelta == 0) {
                explanation.append("сохраняет кратчайший путь, но меняет позицию");
            } else {
                explanation.append("увеличивает путь до цели на ").append(myDelta);
            }
        } else {
            explanation.append("Стена ");
            if (enemyDelta > 0) {
                explanation.append("удлиняет путь соперника на ").append(enemyDelta);
            } else {
                explanation.append("не удлиняет кратчайший путь соперника напрямую");
            }
            if (myDelta > 0) {
                explanation.append(", цена для своего пути: +").append(myDelta);
            }
        }
        explanation.append(". Пути после хода: P").append(playerId).append("=").append(myAfter)
                .append(", P").append(3 - playerId).append("=").append(enemyAfter);
        return explanation.toString();
    }

    private void applyMove(Board board, Move move) {
        if (move.getMoveType() == MoveType.MOVE_PLAYER) {
            if (move.getPlayerId() == 1) {
                board.setPositionOfPlayer1(move.getEndPosition());
            } else {
                board.setPositionOfPlayer2(move.getEndPosition());
            }
        } else {
            board.placeWall(move.getStartPosition(), move.getEndPosition());
        }
    }
}
