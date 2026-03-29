package ru.ac.uniyar.model.algorithms;

import ru.ac.uniyar.model.Board;
import ru.ac.uniyar.model.Move;
import ru.ac.uniyar.model.enums.ComputerAlgorithmType;
import ru.ac.uniyar.model.enums.ComputerPlayerHardnessLevel;
import ru.ac.uniyar.model.enums.MoveType;

import java.util.*;

public interface Algorithm {
    ComputerAlgorithmType getType();
    Move getMove(Board board, ComputerPlayerHardnessLevel hardnessLevel, int playerId, int amountOfWallsLeft);

    default List<Move> getMoves(Board board, int playerId, int wallsLeft) {
        List<Move> moves = new ArrayList<>();
        String pos = getCurrentPosition(board, playerId);

        // 🎯 1. Обычные ходы (всегда приоритет)
        for (String availableMove : board.getAvailableMoves(pos)) {
            moves.add(Move.movePlayer(playerId, availableMove));
        }

        if (wallsLeft <= 0) return moves;

        int size = (int) Math.sqrt(board.getTiles().size());

        // 🔥 2. Стены вдоль кратчайшего пути противника
        List<String> enemyPath = getShortestPathCells(
                board,
                getCurrentPosition(board, 3 - playerId),
                getTargetRow(3 - playerId, size)
        );

        Set<String> used = new HashSet<>();

        for (String cell : enemyPath) {
            int i = cell.charAt(0) - '0';
            int j = cell.charAt(1) - '0';

            // вертикальная стена
            if (i < size - 1) {
                String b1 = i + "" + j;
                String b2 = (i + 1) + "" + j;

                String key = b1 + "-" + b2;
                if (used.add(key) && isValidWall(board, b1, b2)) {
                    moves.add(Move.placeWall(playerId, b1, b2));
                }
            }

            // горизонтальная стена
            if (j < size - 1) {
                String a1 = i + "" + j;
                String a2 = i + "" + (j + 1);

                String key = a1 + "-" + a2;
                if (used.add(key) && isValidWall(board, a1, a2)) {
                    moves.add(Move.placeWall(playerId, a1, a2));
                }
            }
        }

        return moves;
    }

    // 🔥 восстановление кратчайшего пути (ключевая часть)
    default List<String> getShortestPathCells(Board board, String start, int targetRow) {
        Map<String, String> parent = new HashMap<>();
        Queue<String> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();

        queue.add(start);
        visited.add(start);

        String end = null;

        while (!queue.isEmpty()) {
            String curr = queue.poll();
            int i = curr.charAt(0) - '0';

            if (i == targetRow) {
                end = curr;
                break;
            }

            for (String next : board.getAvailableMoves(curr)) {
                if (visited.add(next)) {
                    parent.put(next, curr);
                    queue.add(next);
                }
            }
        }

        List<String> path = new ArrayList<>();

        while (end != null) {
            path.add(end);
            end = parent.get(end);
        }

        return path;
    }

    default boolean isValidWall(Board board, String startPosition, String endPosition) {
        try {
            Board copy = board.copy();
            copy.placeWall(startPosition, endPosition);

            return hasPathToTheEnd(copy, copy.getPositionOfPlayer1(), true)
                    && hasPathToTheEnd(copy, copy.getPositionOfPlayer2(), false);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean hasPathToTheEnd(Board board, String start, boolean toBottom) {
        int size = (int) Math.sqrt(board.getTiles().size());

        Set<String> visited = new HashSet<>();
        Queue<String> queue = new ArrayDeque<>();

        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            String curr = queue.poll();
            int i = curr.charAt(0) - '0';

            if (toBottom && i == size - 1) return true;
            if (!toBottom && i == 0) return true;

            for (String next : board.getAvailableMoves(curr)) {
                if (visited.add(next)) {
                    queue.add(next);
                }
            }
        }
        return false;
    }

    default String getCurrentPosition(Board board, int playerId) {
        return playerId == 1 ? board.getPositionOfPlayer1() : board.getPositionOfPlayer2();
    }

    default int getTargetRow(int playerId, int size) {
        return playerId == 1 ? size - 1 : 0;
    }

    default void applyMove(Board board, Move move) {
        if (move.getMoveType() == MoveType.MOVE_PLAYER) {
            if (move.getPlayerId() == 1) {
                board.setPositionOfPlayer1(move.getEndPosition());
            } else {
                board.setPositionOfPlayer2(move.getEndPosition());
            }
            return;
        }

        board.placeWall(move.getStartPosition(), move.getEndPosition());
    }

    // ⚡ оценка позиции
    default int evaluate(Board board, int playerId, int size) {
        int myDist = calculateShortestPath(board,
                getCurrentPosition(board, playerId),
                getTargetRow(playerId, size));

        int enemyDist = calculateShortestPath(board,
                getCurrentPosition(board, 3 - playerId),
                getTargetRow(3 - playerId, size));

        return enemyDist - myDist;
    }

    default int calculateShortestPath(Board board, String start, int targetRow) {
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new ArrayDeque<>();

        queue.add(start);
        visited.add(start);

        int depth = 0;

        while (!queue.isEmpty()) {
            for (int k = queue.size(); k > 0; --k) {
                String curr = queue.poll();

                int i = curr.charAt(0) - '0';
                if (i == targetRow) return depth;

                for (String next : board.getAvailableMoves(curr)) {
                    if (visited.add(next)) {
                        queue.add(next);
                    }
                }
            }
            depth++;
        }
        return 1000;
    }
}