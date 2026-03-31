package ru.ac.uniyar.model.algorithms;

import ru.ac.uniyar.model.Board;
import ru.ac.uniyar.model.Move;
import ru.ac.uniyar.model.enums.*;

import java.util.*;

public class MinimaxAlgorithm implements Algorithm {
    /**
     * уже посчитанные ранее позиции
     */
    private final Map<String, Integer> cache = new HashMap<>();
    /**
     * хорошие ходы на определённой глубине, которые ранее давали лучший результат
     */
    private final Map<Integer, Move> bestMoves = new HashMap<>();

    @Override
    public ComputerAlgorithmType getType() {
        return ComputerAlgorithmType.MINIMAX;
    }

    /**
     * пока есть время, итеративно увеличиваем глубину поиска решения
     * если время кончилось -> отдаем лучшее, что нашли
     */
    @Override
    public Move getMove(Board board, ComputerPlayerHardnessLevel hardnessLevel, int playerId, int wallsLeft) {
        int size = (int) Math.sqrt(board.getTiles().size());
        long endTime = System.currentTimeMillis() + getTimeLimit(hardnessLevel, size);
        int maxDepth = switch (hardnessLevel) {
            case EASY -> 3 * (size / GameSize.NORMAL.getAmountOfTilesPerSide());
            case MEDIUM -> 5 * (size / GameSize.NORMAL.getAmountOfTilesPerSide());
            case HARD -> 7 * (size / GameSize.NORMAL.getAmountOfTilesPerSide());
        };

        Move best = null;
        for (int depth = 1; depth <= maxDepth; depth++) {
            if (System.currentTimeMillis() > endTime) {
                break;
            }
            best = search(board, depth, playerId, size, wallsLeft, wallsLeft, endTime);
        }
        return best;
    }

    /**
     * генерируем всевозможные ходы, сортируем и отбираем из них топ 20
     * потом прогоняем через minimax функцию оценки
     */
    private Move search(Board board, int depth, int playerId, int size, int wallsLeft1, int wallsLeft2, long endTime) {
        List<Move> moves = getMoves(board, playerId, wallsLeft1);
        moves.sort((a, b) -> Integer.compare(
                evaluateMove(board, b, playerId, size, wallsLeft1, wallsLeft2),
                evaluateMove(board, a, playerId, size, wallsLeft1, wallsLeft2)
        ));
        if (moves.size() > 20) {
            moves = moves.subList(0, 20);
        }

        Move bestMove = null;
        int bestValue = Integer.MIN_VALUE;
        for (Move move : moves) {
            if (System.currentTimeMillis() > endTime) {
                return bestMove;
            }

            Board copy = board.copy();
            applyMove(copy, move);

            int newWallsLeft1 = wallsLeft1, newWallsLeft2 = wallsLeft2;
            if (move.getMoveType() == MoveType.PLACE_WALL) {
                if (playerId == 1) {
                    --newWallsLeft1;
                }
                else --newWallsLeft2;
            }

            int value = minimax(copy, depth - 1, false, playerId, size, newWallsLeft1, newWallsLeft2, endTime);
            if (value > bestValue) {
                bestValue = value;
                bestMove = move;
            }
        }
        return bestMove;
    }

    /**
     * если max = true (наш ход) - максимизируем оценку - результат в alpha (нижней границе)
     * если max = false (ход противника) - минимизируем - результат в beta (верхней границе)
     */
    private int minimax(Board board, int depth, boolean maximizing, int playerId, int size, int wallsLeft1, int wallsLeft2, long endTime) {
        if (System.currentTimeMillis() > endTime) {
            return evaluate(board, playerId, size, wallsLeft1, wallsLeft2);
        }
        String key = board.hashCode() + "|" + depth + "|" + maximizing;
        if (cache.containsKey(key)) {
            return cache.get(key);
        }

        if (depth == 0) {
            int val = evaluate(board, playerId, size, wallsLeft1, wallsLeft2);
            cache.put(key, val);
            return val;
        }

        int current = maximizing ? playerId : (3 - playerId);
        int walls = current == 1 ? wallsLeft1 : wallsLeft2;
        List<Move> moves = getMoves(board, current, walls);

        Move nestMove = bestMoves.get(depth);
        if (nestMove != null && moves.remove(nestMove)) {
            moves.add(0, nestMove);
        }

        int best = maximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        for (Move move : moves) {
            Board copy = board.copy();
            applyMove(copy, move);

            int newWallsLeft1 = wallsLeft1, newWallsLeft2 = wallsLeft2;
            if (move.getMoveType() == MoveType.PLACE_WALL) {
                if (current == 1) {
                    --newWallsLeft1;
                } else {
                    --newWallsLeft2;
                }
            }

            int val = minimax(copy, depth - 1, !maximizing, playerId, size, newWallsLeft1, newWallsLeft2, endTime);
            if (maximizing) {
                if (val > best) {
                    best = val;
                    bestMoves.put(depth, move);
                }
            } else {
                if (val < best) {
                    best = val;
                    bestMoves.put(depth, move);
                }
            }
        }
        cache.put(key, best);
        return best;
    }

    /**
     * оценка одного хода для сортировки ходов
     */
    private int evaluateMove(Board board, Move move, int playerId, int size, int wallsLeft1, int wallsLeft2) {
        Board copy = board.copy();
        applyMove(copy, move);
        return evaluate(copy, playerId, size, wallsLeft1, wallsLeft2);
    }
}