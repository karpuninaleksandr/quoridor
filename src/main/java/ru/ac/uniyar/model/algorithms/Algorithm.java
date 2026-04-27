package ru.ac.uniyar.model.algorithms;

import ru.ac.uniyar.model.Board;
import ru.ac.uniyar.model.Move;
import ru.ac.uniyar.model.enums.ComputerAlgorithmType;
import ru.ac.uniyar.model.enums.ComputerPlayerHardnessLevel;
import ru.ac.uniyar.model.enums.GameSize;
import ru.ac.uniyar.model.enums.MoveType;

import java.util.*;

public interface Algorithm {
    ComputerAlgorithmType getType();
    Move getMove(Board board, ComputerPlayerHardnessLevel hardnessLevel, int playerId, int amountOfWallsLeft);

    default List<Move> getMoves(Board board, int playerId, int wallsLeft) {
        List<Move> moves = new ArrayList<>();
        String pos = getCurrentPosition(board, playerId);

        for (String next : board.getAvailableMoves(pos)) {
            moves.add(Move.movePlayer(playerId, next));
        }

        if (wallsLeft <= 0) {
            return moves;
        }

        int size = (int) Math.sqrt(board.getTiles().size());
        Set<String> used = new HashSet<>();
        List<String> enemyPath = getShortestPathCells(board, getCurrentPosition(board, 3 - playerId),
                getTargetRow(3 - playerId, size));
        List<String> myPath = getShortestPathCells(board, getCurrentPosition(board, playerId),
                getTargetRow(playerId, size));
        List<String> candidateCells = new ArrayList<>();
        candidateCells.addAll(enemyPath.subList(0, Math.min(10, enemyPath.size())));
        candidateCells.addAll(myPath.subList(0, Math.min(6, myPath.size())));
        addNeighborhood(candidateCells, getCurrentPosition(board, 3 - playerId), size);

        int beforeEnemy = calculateShortestPath(board, getCurrentPosition(board, 3 - playerId), getTargetRow(3 - playerId, size));
        int beforeMine = calculateShortestPath(board, getCurrentPosition(board, playerId), getTargetRow(playerId, size));

        for (String cell : candidateCells) {
            int i = cell.charAt(0) - '0';
            int j = cell.charAt(1) - '0';

            if (i < size - 1) {
                String start = i + "" + j;
                String end = (i + 1) + "" + j;
                String wallKey = start + "-" + end;

                if (used.add(wallKey) && isWallPlaceable(board, start, end) && isValidWall(board, start, end)) {
                    if (isStrategicWall(board, start, end, playerId, size, beforeEnemy, beforeMine)) {
                        moves.add(Move.placeWall(playerId, start, end));
                    }
                }
            }

            if (j < size - 1) {
                String start = i + "" + j;
                String end = i + "" + (j + 1);
                String wallKey = start + "-" + end;

                if (used.add(wallKey) && isWallPlaceable(board, start, end) && isValidWall(board, start, end)) {
                    if (isStrategicWall(board, start, end, playerId, size, beforeEnemy, beforeMine)) {
                        moves.add(Move.placeWall(playerId, start, end));
                    }
                }
            }
        }
        return moves;
    }

    default void addNeighborhood(List<String> cells, String center, int size) {
        int row = center.charAt(0) - '0';
        int col = center.charAt(1) - '0';

        for (int di = -2; di <= 2; ++di) {
            for (int dj = -2; dj <= 2; ++dj) {
                int nextRow = row + di;
                int nextCol = col + dj;
                if (nextRow >= 0 && nextCol >= 0 && nextRow < size && nextCol < size) {
                    cells.add(nextRow + "" + nextCol);
                }
            }
        }
    }

    default boolean isStrategicWall(Board board, String start, String end, int playerId, int size,
                                    int beforeEnemy, int beforeMine) {
        Board copy = board.copy();
        copy.placeWall(start, end);

        int afterEnemy = calculateShortestPath(copy, getCurrentPosition(copy, 3 - playerId), getTargetRow(3 - playerId, size));
        int afterMine = calculateShortestPath(copy, getCurrentPosition(copy, playerId), getTargetRow(playerId, size));

        return afterEnemy > beforeEnemy || afterEnemy - beforeEnemy >= afterMine - beforeMine;
    }

    default boolean isWallPlaceable(Board board, String start, String end) {
        try {
            Board copy = board.copy();
            copy.placeWall(start, end);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

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

    default boolean isValidWall(Board board, String start, String end) {
        try {
            Board copy = board.copy();
            copy.placeWall(start, end);

            int size = (int) Math.sqrt(board.getTiles().size());

            return hasPath(copy, copy.getPositionOfPlayer1(), 0) && hasPath(copy, copy.getPositionOfPlayer2(), size - 1);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean hasPath(Board board, String start, int targetRow) {
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new ArrayDeque<>();

        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            String curr = queue.poll();
            int i = curr.charAt(0) - '0';

            if (i == targetRow) {
                return true;
            }

            for (String next : board.getAvailableMoves(curr)) {
                if (visited.add(next)) {
                    queue.add(next);
                }
            }
        }
        return false;
    }

    default void applyMove(Board board, Move move) {
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

    default String getCurrentPosition(Board board, int playerId) {
        return playerId == 1 ? board.getPositionOfPlayer1() : board.getPositionOfPlayer2();
    }

    default int getTargetRow(int playerId, int size) {
        return playerId == 1 ? 0 : size - 1;
    }

    default int evaluate(Board board, int playerId, int size, int wallsLeft1, int wallsLeft2) {
        int myDist = calculateShortestPath(board, getCurrentPosition(board, playerId), getTargetRow(playerId, size));
        int enemyDist = calculateShortestPath(board, getCurrentPosition(board, 3 - playerId), getTargetRow(3 - playerId, size));

        int score = (enemyDist - myDist) * 50;

        score += board.getAvailableMoves(getCurrentPosition(board, playerId)).size() * 3;
        score -= board.getAvailableMoves(getCurrentPosition(board, 3 - playerId)).size() * 2;

        score += (wallsLeft1 - wallsLeft2) * 15;

        int myRow = getCurrentPosition(board, playerId).charAt(0) - '0';
        int enemyRow = getCurrentPosition(board, 3 - playerId).charAt(0) - '0';
        score += playerId == 1 ? (size - 1 - myRow) * 4 : myRow * 4;
        score -= playerId == 1 ? enemyRow * 4 : (size - 1 - enemyRow) * 4;

        return score;
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

    default long getTimeLimit(ComputerPlayerHardnessLevel level, int size) {
        int multiplier = Math.max(1, size / GameSize.NORMAL.getAmountOfTilesPerSide());
        return switch (level) {
            case EASY -> 700L * multiplier;
            case MEDIUM -> 1600L * multiplier;
            case HARD -> 2800L * multiplier;
        };
    }

    default String boardKey(Board board) {
        StringBuilder key = new StringBuilder();
        key.append(board.getPositionOfPlayer1()).append('|').append(board.getPositionOfPlayer2()).append('|');

        board.getTiles().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    key.append(entry.getKey());
                    key.append(entry.getValue().isLeftMovementAvailable() ? '1' : '0');
                    key.append(entry.getValue().isForwardMovementAvailable() ? '1' : '0');
                    key.append(entry.getValue().isRightMovementAvailable() ? '1' : '0');
                    key.append(entry.getValue().isBackwardsMovementAvailable() ? '1' : '0');
                });

        return key.toString();
    }
}
