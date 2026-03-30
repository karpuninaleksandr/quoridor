package ru.ac.uniyar.model.algorithms;

import ru.ac.uniyar.model.Board;
import ru.ac.uniyar.model.Move;
import ru.ac.uniyar.model.enums.*;

import java.util.*;

public class AlphaBetaAlgorithm implements Algorithm {
    @Override
    public ComputerAlgorithmType getType() {
        return ComputerAlgorithmType.ALPHABETA;
    }

    @Override
    public Move getMove(Board board, ComputerPlayerHardnessLevel hardnessLevel, int playerId, int wallsLeft) {
        int size = (int) Math.sqrt(board.getTiles().size());

        long timeLimit = switch (hardnessLevel) {
            case EASY -> 1000L * (size / GameSize.NORMAL.getAmountOfTilesPerSide());
            case MEDIUM -> 2000L * (size / GameSize.NORMAL.getAmountOfTilesPerSide());
            case HARD -> 3000L * (size / GameSize.NORMAL.getAmountOfTilesPerSide());
        };

        int maxDepth = switch (hardnessLevel) {
            case EASY -> 2;
            case MEDIUM -> 4;
            case HARD -> 10;
        };

        long endTime = System.currentTimeMillis() + timeLimit;

        Move bestMove = null;

        for (int depth = 1; depth <= maxDepth; depth++) {
            if (System.currentTimeMillis() > endTime) break;
            try {
                Move move = search(board, depth, playerId, size, wallsLeft, wallsLeft, endTime);

                if (move != null) bestMove = move;
            } catch (RuntimeException e) {
                break;
            }
        }

        return bestMove;
    }

    private Move search(Board board, int depth, int playerId, int size, int walls1, int walls2, long endTime) {
        List<Move> moves = getMoves(board, playerId, walls1);

        Move bestMove = null;
        int bestValue = Integer.MIN_VALUE;

        for (Move move : moves) {
            checkTime(endTime);

            Board copy = board.copy();
            applyMove(copy, move);

            int nextWalls1 = walls1;
            if (move.getMoveType() == MoveType.PLACE_WALL) nextWalls1--;

            int eval = alphaBeta(copy, depth - 1,
                    Integer.MIN_VALUE, Integer.MAX_VALUE,
                    false, playerId, size,
                    nextWalls1, walls2, endTime);

            if (eval > bestValue) {
                bestValue = eval;
                bestMove = move;
            }
        }

        return bestMove;
    }

    private int alphaBeta(Board board, int depth, int alpha, int beta,
                          boolean maximizing, int playerId, int size,
                          int walls1, int walls2, long endTime) {

        checkTime(endTime);

        if (depth == 0) {
            return evaluate(board, playerId, size, walls1, walls2);
        }

        int currentPlayer = maximizing ? playerId : (3 - playerId);
        int currentWalls = currentPlayer == 1 ? walls1 : walls2;

        List<Move> moves = getMoves(board, currentPlayer, currentWalls);

        if (maximizing) {
            int maxEval = Integer.MIN_VALUE;

            for (Move move : moves) {
                Board copy = board.copy();
                applyMove(copy, move);

                int w1 = walls1;
                int w2 = walls2;

                if (move.getMoveType() == MoveType.PLACE_WALL) {
                    if (currentPlayer == 1) w1--;
                    else w2--;
                }

                int eval = alphaBeta(copy, depth - 1, alpha, beta,
                        false, playerId, size, w1, w2, endTime);

                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, eval);

                if (beta <= alpha) break;
            }
            return maxEval;

        } else {
            int minEval = Integer.MAX_VALUE;

            for (Move move : moves) {
                Board copy = board.copy();
                applyMove(copy, move);

                int w1 = walls1;
                int w2 = walls2;

                if (move.getMoveType() == MoveType.PLACE_WALL) {
                    if (currentPlayer == 1) w1--;
                    else w2--;
                }

                int eval = alphaBeta(copy, depth - 1, alpha, beta,
                        true, playerId, size, w1, w2, endTime);

                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);

                if (beta <= alpha) break;
            }
            return minEval;
        }
    }

    private void checkTime(long endTime) {
        if (System.currentTimeMillis() > endTime) {
            throw new RuntimeException("timeout");
        }
    }
}