package ru.ac.uniyar.model.algorithms;

import ru.ac.uniyar.model.Board;
import ru.ac.uniyar.model.Move;
import ru.ac.uniyar.model.enums.*;

import java.util.*;

public class MinimaxAlgorithm implements Algorithm {
    private final Map<String, Integer> cache = new HashMap<>();
    private final Map<Integer, Move> bestMovesInHistory = new HashMap<>();

    @Override
    public ComputerAlgorithmType getType() {
        return ComputerAlgorithmType.MINIMAX;
    }

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
            if (System.currentTimeMillis() > endTime) break;

            best = search(board, depth, playerId, size, wallsLeft, wallsLeft, endTime);
        }

        return best;
    }

    private Move search(Board board, int depth, int playerId, int size, int w1, int w2, long endTime) {
        List<Move> moves = getMoves(board, playerId, w1);

        moves.sort((a, b) -> Integer.compare(
                evaluateMove(board, b, playerId, size, w1, w2),
                evaluateMove(board, a, playerId, size, w1, w2)
        ));

        Move bestMove = null;
        int bestValue = Integer.MIN_VALUE;

        for (Move move : moves) {
            checkTime(endTime);

            Board copy = board.copy();
            applyMove(copy, move);

            int nw1 = w1, nw2 = w2;
            if (move.getMoveType() == MoveType.PLACE_WALL) {
                if (playerId == 1) nw1--;
                else nw2--;
            }

            int value = minimax(copy, depth - 1, false, playerId, size, nw1, nw2, Integer.MIN_VALUE,
                    Integer.MAX_VALUE, endTime);

            if (value > bestValue) {
                bestValue = value;
                bestMove = move;
            }
        }

        return bestMove;
    }

    private int minimax(Board board, int depth, boolean maximizing, int playerId, int size, int w1, int w2, int alpha,
                        int beta, long endTime) {
        checkTime(endTime);

        String key = board.hashCode() + "|" + depth + "|" + maximizing;
        if (cache.containsKey(key)) return cache.get(key);

        if (depth == 0) {
            int val = evaluate(board, playerId, size, w1, w2);
            cache.put(key, val);
            return val;
        }

        int current = maximizing ? playerId : (3 - playerId);
        int walls = current == 1 ? w1 : w2;

        List<Move> moves = getMoves(board, current, walls);

        Move killer = bestMovesInHistory.get(depth);
        if (killer != null && moves.remove(killer)) {
            moves.add(0, killer);
        }

        int best = maximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;

        for (Move move : moves) {
            Board copy = board.copy();
            applyMove(copy, move);

            int nw1 = w1, nw2 = w2;
            if (move.getMoveType() == MoveType.PLACE_WALL) {
                if (current == 1) nw1--;
                else nw2--;
            }

            int val = minimax(copy, depth - 1, !maximizing, playerId, size, nw1, nw2, alpha, beta, endTime);

            if (maximizing) {
                if (val > best) {
                    best = val;
                    bestMovesInHistory.put(depth, move);
                }
                alpha = Math.max(alpha, val);
            } else {
                if (val < best) {
                    best = val;
                    bestMovesInHistory.put(depth, move);
                }
                beta = Math.min(beta, val);
            }

            if (beta <= alpha) break;
        }

        cache.put(key, best);
        return best;
    }

    private int evaluateMove(Board board, Move move, int playerId, int size, int w1, int w2) {
        Board copy = board.copy();
        applyMove(copy, move);
        return evaluate(copy, playerId, size, w1, w2);
    }

    private void checkTime(long endTime) {
        if (System.currentTimeMillis() > endTime) {
            throw new RuntimeException("timeout");
        }
    }
}