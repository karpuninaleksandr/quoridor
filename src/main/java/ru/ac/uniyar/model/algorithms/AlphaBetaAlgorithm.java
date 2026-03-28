package ru.ac.uniyar.model.algorithms;

import ru.ac.uniyar.model.Board;
import ru.ac.uniyar.model.Move;
import ru.ac.uniyar.model.enums.ComputerAlgorithmType;
import ru.ac.uniyar.model.enums.ComputerPlayerHardnessLevel;
import ru.ac.uniyar.model.enums.MoveType;

import java.util.*;

public class AlphaBetaAlgorithm implements Algorithm {
    @Override
    public ComputerAlgorithmType getType() {
        return ComputerAlgorithmType.ALPHABETA;
    }

    @Override
    public Move getMove(Board board, ComputerPlayerHardnessLevel hardnessLevel, int playerId, int amountOfWallsLeft) {
        int depth = switch (hardnessLevel) {
            case EASY -> 1;
            case MEDIUM -> 2;
            case HARD -> 3;
        };

        int size = (int) Math.sqrt(board.getTiles().size());

        List<Move> moves = getMoves(board, playerId, amountOfWallsLeft);

        Move bestMove = null;
        int bestValue = Integer.MIN_VALUE;

        for (Move move : moves) {
            Board copy = board.copy();
            applyMove(copy, move);

            int nextWalls = amountOfWallsLeft;
            if (move.getMoveType() == MoveType.PLACE_WALL) {
                --nextWalls;
            }

            int eval = alphaBeta(copy, depth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, false, playerId,
                    size, amountOfWallsLeft, nextWalls);

            if (eval > bestValue) {
                bestValue = eval;
                bestMove = move;
            }
        }
        return bestMove;
    }

    private int alphaBeta(Board board, int depth, int alpha, int beta, boolean maximizing, int playerId, int size, int wallsLeft1, int wallsLeft2) {
        if (depth == 0) {
            return evaluate(board, playerId, size);
        }

        int currentPlayer = maximizing ? playerId : (3 - playerId);
        int currentWalls = currentPlayer == 1 ? wallsLeft1 : wallsLeft2;

        List<Move> moves = getMoves(board, currentPlayer, currentWalls);

        if (maximizing) {
            int maxEval = Integer.MIN_VALUE;

            for (Move move : moves) {
                Board copy = board.copy();
                applyMove(copy, move);

                int nextWalls1 = wallsLeft1;
                int nextWalls2 = wallsLeft2;

                if (move.getMoveType() == MoveType.PLACE_WALL) {
                    if (currentPlayer == 1) nextWalls1--;
                    else nextWalls2--;
                }

                int eval = alphaBeta(copy, depth - 1, alpha, beta, false, playerId, size, nextWalls1, nextWalls2);

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

                int nextWalls1 = wallsLeft1;
                int nextWalls2 = wallsLeft2;

                if (move.getMoveType() == MoveType.PLACE_WALL) {
                    if (currentPlayer == 1) nextWalls1--;
                    else nextWalls2--;
                }

                int eval = alphaBeta(copy, depth - 1, alpha, beta, true, playerId, size, nextWalls1, nextWalls2);

                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);

                if (beta <= alpha) break;
            }
            return minEval;
        }
    }
}