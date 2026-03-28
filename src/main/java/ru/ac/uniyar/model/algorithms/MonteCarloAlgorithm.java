package ru.ac.uniyar.model.algorithms;

import ru.ac.uniyar.model.Board;
import ru.ac.uniyar.model.Move;
import ru.ac.uniyar.model.enums.ComputerAlgorithmType;
import ru.ac.uniyar.model.enums.ComputerPlayerHardnessLevel;
import ru.ac.uniyar.model.enums.MoveType;

import java.util.*;

public class MonteCarloAlgorithm implements Algorithm {

    private static final Random random = new Random();

    @Override
    public ComputerAlgorithmType getType() {
        return ComputerAlgorithmType.MONTECARLO;
    }

    @Override
    public Move getMove(Board board,
                        ComputerPlayerHardnessLevel hardnessLevel,
                        int playerId,
                        int amountOfWallsLeft) {

        int simulations = switch (hardnessLevel) {
            case EASY -> 20;
            case MEDIUM -> 100;
            case HARD -> 300;
        };

        List<Move> moves = getMoves(board, playerId, amountOfWallsLeft);

        Move bestMove = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (Move move : moves) {

            int wins = 0;

            for (int i = 0; i < simulations; i++) {

                Board copy = board.copy();
                applyMove(copy, move);

                int result = simulateGame(copy,
                        3 - playerId,
                        amountOfWallsLeft,
                        amountOfWallsLeft);

                if (result == playerId) {
                    wins++;
                }
            }

            double score = (double) wins / simulations;

            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }

        return bestMove;
    }

    private int simulateGame(Board board,
                             int currentPlayer,
                             int walls1,
                             int walls2) {

        int size = (int) Math.sqrt(board.getTiles().size());

        for (int step = 0; step < 200; step++) {

            if (isWin(board, 1, size)) return 1;
            if (isWin(board, 2, size)) return 2;

            int walls = currentPlayer == 1 ? walls1 : walls2;
            List<Move> moves = getMoves(board, currentPlayer, walls);

            if (moves.isEmpty()) return 3 - currentPlayer;

            Move move = moves.get(random.nextInt(moves.size()));
            applyMove(board, move);

            if (move.getMoveType() == MoveType.PLACE_WALL) {
                if (currentPlayer == 1) walls1--;
                else walls2--;
            }

            currentPlayer = 3 - currentPlayer;
        }

        return evaluateWinner(board, size);
    }

    private boolean isWin(Board board, int playerId, int size) {
        String pos = getMyPosition(board, playerId);
        int row = pos.charAt(0) - '0';
        return (playerId == 1 && row == size - 1)
                || (playerId == 2 && row == 0);
    }

    private int evaluateWinner(Board board, int size) {
        int d1 = shortestPath(board, getMyPosition(board, 1), size - 1);
        int d2 = shortestPath(board, getMyPosition(board, 2), 0);
        return d1 < d2 ? 1 : 2;
    }

}