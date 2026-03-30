package ru.ac.uniyar.model.algorithms;

import ru.ac.uniyar.model.Board;
import ru.ac.uniyar.model.Move;
import ru.ac.uniyar.model.enums.ComputerAlgorithmType;
import ru.ac.uniyar.model.enums.ComputerPlayerHardnessLevel;
import ru.ac.uniyar.model.enums.MoveType;

import java.util.*;

public class MinimaxAlgorithm implements Algorithm {
    @Override
    public ComputerAlgorithmType getType() {
        return ComputerAlgorithmType.MINIMAX;
    }

    @Override
    public Move getMove(Board board, ComputerPlayerHardnessLevel hardnessLevel, int playerId, int amountOfWallsLeft) {
        int depth = switch (hardnessLevel) {
            case EASY -> 2;
            case MEDIUM -> 4;
            case HARD -> 10;
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
                nextWalls--;
            }

            int eval = minimax(copy, depth - 1, false, playerId, size, amountOfWallsLeft, nextWalls);

            if (eval > bestValue) {
                bestValue = eval;
                bestMove = move;
            }
        }
        return bestMove;
    }

    private int minimax(Board board, int depth, boolean maximizing, int playerId, int size, int wallsLeft1, int wallsLeft2) {
        if (depth == 0) {
            return evaluate(board, playerId, size, wallsLeft1, wallsLeft2);
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

                maxEval = Math.max(maxEval, minimax(copy, depth - 1, false, playerId, size, nextWalls1, nextWalls2));
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

                minEval = Math.min(minEval, minimax(copy, depth - 1, true, playerId, size, nextWalls1, nextWalls2));
            }
            return minEval;
        }
    }
}