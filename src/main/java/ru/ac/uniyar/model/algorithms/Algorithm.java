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
        List<Position> enemyFront = enemyPath.subList(0, Math.min(12, enemyPath.size()));
        List<Position> myFront = myPath.subList(0, Math.min(7, myPath.size()));
        candidateCells.addAll(enemyFront);
        candidateCells.addAll(myFront);
        addNeighborhood(candidateCells, getCurrentPosition(board, 3 - playerId), size);
        for (Position cell : enemyFront.subList(0, Math.min(5, enemyFront.size()))) {
            addNeighborhood(candidateCells, cell, size, 1);
        }

        int beforeEnemy = calculateShortestPath(board, getCurrentPosition(board, 3 - playerId), getTargetRow(3 - playerId, size));
        int beforeMine = calculateShortestPath(board, getCurrentPosition(board, playerId), getTargetRow(playerId, size));
        List<Move> wallMoves = new ArrayList<>();

        for (Position cell : candidateCells) {
            int i = cell.row();
            int j = cell.col();

            if (i < size - 1) {
                Position start = new Position(i, j);
                Position end = new Position(i + 1, j);
                String wallKey = start + "-" + end;

                if (used.add(wallKey) && isWallPlaceable(board, start, end) && isValidWall(board, start, end)) {
                    if (isStrategicWall(board, start, end, playerId, size, beforeEnemy, beforeMine)) {
                        wallMoves.add(Move.placeWall(playerId, start, end));
                    }
                }
            }

            if (j < size - 1) {
                Position start = new Position(i, j);
                Position end = new Position(i, j + 1);
                String wallKey = start + "-" + end;

                if (used.add(wallKey) && isWallPlaceable(board, start, end) && isValidWall(board, start, end)) {
                    if (isStrategicWall(board, start, end, playerId, size, beforeEnemy, beforeMine)) {
                        wallMoves.add(Move.placeWall(playerId, start, end));
                    }
                }
            }
        }
        wallMoves.sort((a, b) -> Integer.compare(
                wallImpact(board, b, playerId, size),
                wallImpact(board, a, playerId, size)
        ));
        moves.addAll(wallMoves);
        return moves;
    }

    default void addNeighborhood(List<Position> cells, Position center, int size) {
        addNeighborhood(cells, center, size, 2);
    }

    default void addNeighborhood(List<Position> cells, Position center, int size, int radius) {
        for (int di = -radius; di <= radius; ++di) {
            for (int dj = -radius; dj <= radius; ++dj) {
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

        int enemyGain = afterEnemy - beforeEnemy;
        int myCost = Math.max(0, afterMine - beforeMine);

        if (enemyGain >= 2) {
            return true;
        }
        if (beforeEnemy <= 3 && enemyGain > 0) {
            return true;
        }
        return enemyGain > 0 && enemyGain >= myCost;
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

            for (Position next : board.getPathNeighbors(curr)) {
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
        Collections.reverse(path);
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

            for (Position next : board.getPathNeighbors(curr)) {
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

    default Move findEndgameMove(Board board, int playerId, int wallsLeft) {
        int size = (int) Math.sqrt(board.getTiles().size());
        Position current = getCurrentPosition(board, playerId);
        for (Position next : board.getAvailableMoves(current)) {
            if (next.row() == getTargetRow(playerId, size)) {
                return Move.movePlayer(playerId, next);
            }
        }

        if (wallsLeft <= 0) {
            return null;
        }

        int enemy = 3 - playerId;
        int enemyDistance = calculateShortestPath(board, getCurrentPosition(board, enemy), getTargetRow(enemy, size));
        if (enemyDistance > 2) {
            return null;
        }

        Move bestWall = null;
        int bestImpact = 0;
        for (Move move : getMoves(board, playerId, wallsLeft)) {
            if (move.getMoveType() != MoveType.PLACE_WALL) {
                continue;
            }
            int impact = wallImpact(board, move, playerId, size);
            if (impact > bestImpact) {
                bestImpact = impact;
                bestWall = move;
            }
        }
        return bestImpact > 0 ? bestWall : null;
    }

    default boolean isNoisyPosition(Board board, int playerId, int size) {
        int myDist = calculateShortestPath(board, getCurrentPosition(board, playerId), getTargetRow(playerId, size));
        int enemyDist = calculateShortestPath(board, getCurrentPosition(board, 3 - playerId), getTargetRow(3 - playerId, size));
        return myDist <= 2 || enemyDist <= 2;
    }

    default List<Move> getQuiescenceMoves(Board board, int currentPlayer, int wallsLeft, int rootPlayerId, int size) {
        List<Move> moves = new ArrayList<>();
        Position current = getCurrentPosition(board, currentPlayer);
        for (Position next : board.getAvailableMoves(current)) {
            if (next.row() == getTargetRow(currentPlayer, size)) {
                moves.add(Move.movePlayer(currentPlayer, next));
            }
        }

        if (wallsLeft > 0) {
            List<Move> tacticalWalls = getMoves(board, currentPlayer, wallsLeft).stream()
                    .filter(move -> move.getMoveType() == MoveType.PLACE_WALL)
                    .sorted((a, b) -> Integer.compare(
                            wallImpact(board, b, currentPlayer, size),
                            wallImpact(board, a, currentPlayer, size)
                    ))
                    .limit(8)
                    .toList();
            moves.addAll(tacticalWalls);
        }
        return moves;
    }

    default int wallImpact(Board board, Move move, int playerId, int size) {
        int enemy = 3 - playerId;
        int beforeEnemy = calculateShortestPath(board, getCurrentPosition(board, enemy), getTargetRow(enemy, size));
        int beforeMine = calculateShortestPath(board, getCurrentPosition(board, playerId), getTargetRow(playerId, size));
        Board copy = board.copy();
        applyMove(copy, move);
        int afterEnemy = calculateShortestPath(copy, getCurrentPosition(copy, enemy), getTargetRow(enemy, size));
        int afterMine = calculateShortestPath(copy, getCurrentPosition(copy, playerId), getTargetRow(playerId, size));
        return (afterEnemy - beforeEnemy) * 100 - Math.max(0, afterMine - beforeMine) * 45;
    }

    default int evaluate(Board board, int playerId, int size, int wallsLeft1, int wallsLeft2) {
        Integer terminal = terminalScore(board, playerId, size, 0);
        if (terminal != null) {
            return terminal;
        }

        int myDist = calculateShortestPath(board, getCurrentPosition(board, playerId), getTargetRow(playerId, size));
        int enemyDist = calculateShortestPath(board, getCurrentPosition(board, 3 - playerId), getTargetRow(3 - playerId, size));

        int score = (enemyDist - myDist) * 65;
        score += endgameDistanceScore(myDist, enemyDist);

        score += board.getAvailableMoves(getCurrentPosition(board, playerId)).size() * 4;
        score -= board.getAvailableMoves(getCurrentPosition(board, 3 - playerId)).size() * 3;

        int myWalls = playerId == 1 ? wallsLeft1 : wallsLeft2;
        int enemyWalls = playerId == 1 ? wallsLeft2 : wallsLeft1;
        int wallWeight = enemyDist <= 4 ? 24 : 14;
        score += myWalls * wallWeight - enemyWalls * 10;

        int myRow = getCurrentPosition(board, playerId).row();
        int enemyRow = getCurrentPosition(board, 3 - playerId).row();
        score += playerId == 1 ? (size - 1 - myRow) * 6 : myRow * 6;
        score -= playerId == 1 ? enemyRow * 6 : (size - 1 - enemyRow) * 6;

        return score;
    }

    default int endgameDistanceScore(int myDist, int enemyDist) {
        int score = 0;
        if (myDist <= 1) {
            score += 1600;
        } else if (myDist == 2) {
            score += 520;
        } else if (myDist == 3) {
            score += 180;
        }

        if (enemyDist <= 1) {
            score -= 2100;
        } else if (enemyDist == 2) {
            score -= 700;
        } else if (enemyDist == 3) {
            score -= 240;
        }
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

                for (Position next : board.getPathNeighbors(curr)) {
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
