package ru.ac.uniyar.model.algorithms;

import ru.ac.uniyar.model.Board;
import ru.ac.uniyar.model.Move;
import ru.ac.uniyar.model.Position;
import ru.ac.uniyar.model.enums.ComputerAlgorithmType;
import ru.ac.uniyar.model.enums.ComputerPlayerHardnessLevel;
import ru.ac.uniyar.model.enums.GameSize;
import ru.ac.uniyar.model.enums.MoveType;

import java.util.*;

public interface Algorithm {
    int WIN_SCORE = 100_000;

    ComputerAlgorithmType getType();
    Move getMove(Board board, ComputerPlayerHardnessLevel hardnessLevel, int playerId, int wallsLeft1, int wallsLeft2);

    default AlgorithmReport getLastReport() {
        return null;
    }

    default void setRecentPositions(List<Position> recentPositions) {
    }

    default int movementPreference(Move move, Board board, int playerId, int size, List<Position> recentPositions) {
        if (move.getMoveType() != MoveType.MOVE_PLAYER || move.getPlayerId() != playerId) {
            return 0;
        }

        int score = 0;
        Position current = getCurrentPosition(board, playerId);
        Position target = move.getEndPosition();
        int direction = playerId == 1 ? -1 : 1;
        int rowDelta = target.row() - current.row();

        if (rowDelta == direction) {
            score += 45;
        } else if (rowDelta == -direction) {
            score -= 120;
        }

        for (int index = 0; index < recentPositions.size(); ++index) {
            Position recent = recentPositions.get(index);
            if (recent.equals(current)) {
                continue;
            }
            if (target.equals(recent)) {
                score -= index <= 1 ? 500 : 180;
            }
        }

        int before = Math.abs(current.row() - getTargetRow(playerId, size));
        int after = Math.abs(target.row() - getTargetRow(playerId, size));
        score += (before - after) * 35;
        return score;
    }

    default List<Move> getMoves(Board board, int playerId, int wallsLeft) {
        List<Move> moves = new ArrayList<>();
        Position pos = getCurrentPosition(board, playerId);

        for (Position next : board.getAvailableMoves(pos)) {
            moves.add(Move.movePlayer(playerId, next));
        }

        if (wallsLeft <= 0) {
            return moves;
        }

        int size = (int) Math.sqrt(board.getTiles().size());
        Set<String> used = new HashSet<>();
        List<Position> enemyPath = getShortestPathCells(board, getCurrentPosition(board, 3 - playerId),
                getTargetRow(3 - playerId, size));
        List<Position> myPath = getShortestPathCells(board, getCurrentPosition(board, playerId),
                getTargetRow(playerId, size));
        List<Position> candidateCells = new ArrayList<>();
        candidateCells.addAll(enemyPath.subList(0, Math.min(10, enemyPath.size())));
        candidateCells.addAll(myPath.subList(0, Math.min(6, myPath.size())));
        addNeighborhood(candidateCells, getCurrentPosition(board, 3 - playerId), size);

        int beforeEnemy = calculateShortestPath(board, getCurrentPosition(board, 3 - playerId), getTargetRow(3 - playerId, size));
        int beforeMine = calculateShortestPath(board, getCurrentPosition(board, playerId), getTargetRow(playerId, size));

        for (Position cell : candidateCells) {
            int i = cell.row();
            int j = cell.col();

            if (i < size - 1) {
                Position start = new Position(i, j);
                Position end = new Position(i + 1, j);
                String wallKey = start + "-" + end;

                if (used.add(wallKey) && isWallPlaceable(board, start, end) && isValidWall(board, start, end)) {
                    if (isStrategicWall(board, start, end, playerId, size, beforeEnemy, beforeMine)) {
                        moves.add(Move.placeWall(playerId, start, end));
                    }
                }
            }

            if (j < size - 1) {
                Position start = new Position(i, j);
                Position end = new Position(i, j + 1);
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

    default void addNeighborhood(List<Position> cells, Position center, int size) {
        for (int di = -2; di <= 2; ++di) {
            for (int dj = -2; dj <= 2; ++dj) {
                int nextRow = center.row() + di;
                int nextCol = center.col() + dj;
                if (nextRow >= 0 && nextCol >= 0 && nextRow < size && nextCol < size) {
                    cells.add(new Position(nextRow, nextCol));
                }
            }
        }
    }

    default boolean isStrategicWall(Board board, Position start, Position end, int playerId, int size,
                                    int beforeEnemy, int beforeMine) {
        Board copy = board.copy();
        copy.placeWall(start, end);

        int afterEnemy = calculateShortestPath(copy, getCurrentPosition(copy, 3 - playerId), getTargetRow(3 - playerId, size));
        int afterMine = calculateShortestPath(copy, getCurrentPosition(copy, playerId), getTargetRow(playerId, size));

        return afterEnemy > beforeEnemy || afterEnemy - beforeEnemy >= afterMine - beforeMine;
    }

    default boolean isWallPlaceable(Board board, Position start, Position end) {
        try {
            Board copy = board.copy();
            copy.placeWall(start, end);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    default List<Position> getShortestPathCells(Board board, Position start, int targetRow) {
        Map<Position, Position> parent = new HashMap<>();
        Queue<Position> queue = new ArrayDeque<>();
        Set<Position> visited = new HashSet<>();

        queue.add(start);
        visited.add(start);

        Position end = null;
        while (!queue.isEmpty()) {
            Position curr = queue.poll();

            if (curr.row() == targetRow) {
                end = curr;
                break;
            }

            for (Position next : board.getAvailableMoves(curr)) {
                if (visited.add(next)) {
                    parent.put(next, curr);
                    queue.add(next);
                }
            }
        }

        List<Position> path = new ArrayList<>();
        while (end != null) {
            path.add(end);
            end = parent.get(end);
        }
        return path;
    }

    default boolean isValidWall(Board board, Position start, Position end) {
        try {
            Board copy = board.copy();
            copy.placeWall(start, end);

            int size = (int) Math.sqrt(board.getTiles().size());

            return hasPath(copy, copy.getPositionOfPlayer1(), 0) && hasPath(copy, copy.getPositionOfPlayer2(), size - 1);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean hasPath(Board board, Position start, int targetRow) {
        Set<Position> visited = new HashSet<>();
        Queue<Position> queue = new ArrayDeque<>();

        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            Position curr = queue.poll();

            if (curr.row() == targetRow) {
                return true;
            }

            for (Position next : board.getAvailableMoves(curr)) {
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

    default Position getCurrentPosition(Board board, int playerId) {
        return playerId == 1 ? board.getPositionOfPlayer1() : board.getPositionOfPlayer2();
    }

    default int getTargetRow(int playerId, int size) {
        return playerId == 1 ? 0 : size - 1;
    }

    default boolean isWin(Board board, int playerId, int size) {
        return getCurrentPosition(board, playerId).row() == getTargetRow(playerId, size);
    }

    default Integer terminalScore(Board board, int rootPlayerId, int size, int depth) {
        if (isWin(board, rootPlayerId, size)) {
            return WIN_SCORE + depth;
        }
        if (isWin(board, 3 - rootPlayerId, size)) {
            return -WIN_SCORE - depth;
        }
        return null;
    }

    default int evaluate(Board board, int playerId, int size, int wallsLeft1, int wallsLeft2) {
        Integer terminal = terminalScore(board, playerId, size, 0);
        if (terminal != null) {
            return terminal;
        }

        int myDist = calculateShortestPath(board, getCurrentPosition(board, playerId), getTargetRow(playerId, size));
        int enemyDist = calculateShortestPath(board, getCurrentPosition(board, 3 - playerId), getTargetRow(3 - playerId, size));

        int score = (enemyDist - myDist) * 50;

        score += board.getAvailableMoves(getCurrentPosition(board, playerId)).size() * 3;
        score -= board.getAvailableMoves(getCurrentPosition(board, 3 - playerId)).size() * 2;

        int myWalls = playerId == 1 ? wallsLeft1 : wallsLeft2;
        int enemyWalls = playerId == 1 ? wallsLeft2 : wallsLeft1;
        score += (myWalls - enemyWalls) * 15;

        int myRow = getCurrentPosition(board, playerId).row();
        int enemyRow = getCurrentPosition(board, 3 - playerId).row();
        score += playerId == 1 ? (size - 1 - myRow) * 4 : myRow * 4;
        score -= playerId == 1 ? enemyRow * 4 : (size - 1 - enemyRow) * 4;

        return score;
    }

    default int calculateShortestPath(Board board, Position start, int targetRow) {
        Set<Position> visited = new HashSet<>();
        Queue<Position> queue = new ArrayDeque<>();

        queue.add(start);
        visited.add(start);

        int depth = 0;

        while (!queue.isEmpty()) {
            for (int k = queue.size(); k > 0; --k) {
                Position curr = queue.poll();

                if (curr.row() == targetRow) return depth;

                for (Position next : board.getAvailableMoves(curr)) {
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
                .sorted(Comparator
                        .comparingInt((Map.Entry<Position, ?> entry) -> entry.getKey().row())
                        .thenComparingInt(entry -> entry.getKey().col()))
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
