package ru.ac.uniyar.model.algorithms;

import ru.ac.uniyar.model.Board;
import ru.ac.uniyar.model.Move;
import ru.ac.uniyar.model.enums.ComputerAlgorithmType;
import ru.ac.uniyar.model.enums.ComputerPlayerHardnessLevel;
import ru.ac.uniyar.model.enums.MoveType;

import java.util.*;

//интерфейс для взаимодействия с любым из алгоритмов для ComputerPlayer
public interface Algorithm {
    ComputerAlgorithmType getType();
    Move getMove(Board board, ComputerPlayerHardnessLevel hardnessLevel, int playerId, int amountOfWallsLeft);

    default List<Move> getMoves(Board board, int playerId, int wallsLeft) {

        List<Move> moves = new ArrayList<>();

        String pos = getMyPosition(board, playerId);

        for (String p : board.getAvailableMoves(pos)) {
            moves.add(Move.movePlayer(playerId, p));
        }

        // стены
        if (wallsLeft <= 0) return moves;

        int size = (int) Math.sqrt(board.getTiles().size());

        for (int i = 0; i < size - 1; i++) {
            for (int j = 0; j < size - 1; j++) {

                String a1 = i + "" + j;
                String a2 = i + "" + (j + 1);

                if (isValidWall(board, a1, a2)) {
                    moves.add(Move.placeWall(playerId, a1, a2));
                }

                String b1 = i + "" + j;
                String b2 = (i + 1) + "" + j;

                if (isValidWall(board, b1, b2)) {
                    moves.add(Move.placeWall(playerId, b1, b2));
                }
            }
        }

        return moves;
    }

    default boolean isValidWall(Board board, String a, String b) {

        try {
            Board copy = board.copy();
            copy.placeWall(a, b);

            return hasPath(copy, copy.getPositionOfPlayer1(), true)
                    && hasPath(copy, copy.getPositionOfPlayer2(), false);

        } catch (Exception e) {
            return false;
        }
    }

    private boolean hasPath(Board board, String start, boolean toBottom) {

        int size = (int) Math.sqrt(board.getTiles().size());

        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();

        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {

            String cur = queue.poll();
            int i = cur.charAt(0) - '0';

            if (toBottom && i == size - 1) return true;
            if (!toBottom && i == 0) return true;

            for (String next : board.getAvailableMoves(cur)) {
                if (visited.add(next)) {
                    queue.add(next);
                }
            }
        }

        return false;
    }

    default String getMyPosition(Board board, int playerId) {
        return playerId == 1
                ? board.getPositionOfPlayer1()
                : board.getPositionOfPlayer2();
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

        if (move.getMoveType() == MoveType.PLACE_WALL) {
            board.placeWall(move.getStartPosition(), move.getEndPosition());
        }
    }

    default int evaluate(Board board, int playerId, int size) {

        int myDist = shortestPath(board,
                getMyPosition(board, playerId),
                getTargetRow(playerId, size));

        int enemyDist = shortestPath(board,
                getMyPosition(board, 3 - playerId),
                getTargetRow(3 - playerId, size));

        return enemyDist - myDist;
    }

    default int shortestPath(Board board, String start, int targetRow) {

        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();

        queue.add(start);
        visited.add(start);

        int depth = 0;

        while (!queue.isEmpty()) {

            int size = queue.size();

            for (int k = 0; k < size; k++) {

                String cur = queue.poll();

                int i = cur.charAt(0) - '0';
                if (i == targetRow) return depth;

                for (String next : board.getAvailableMoves(cur)) {
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
