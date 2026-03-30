package ru.ac.uniyar.model.algorithms;

import ru.ac.uniyar.model.Board;
import ru.ac.uniyar.model.Move;
import ru.ac.uniyar.model.enums.ComputerAlgorithmType;
import ru.ac.uniyar.model.enums.ComputerPlayerHardnessLevel;

import java.util.*;

public class RandomAlgorithm implements Algorithm {
    private static final Random random = new Random();

    @Override
    public ComputerAlgorithmType getType() {
        return ComputerAlgorithmType.RANDOM;
    }

    @Override
    public Move getMove(Board board, ComputerPlayerHardnessLevel hardnessLevel, int playerId, int amountOfWallsLeft) {
        List<Move> moves = getMoves(board, playerId, amountOfWallsLeft);

        if (moves.isEmpty()) return null;

        switch (hardnessLevel) {
            case EASY -> {
                return moves.get(random.nextInt(moves.size()));
            }
            case MEDIUM -> {
                moves.sort((a, b) -> Integer.compare(
                        evaluateAfterMove(board, a, playerId, amountOfWallsLeft),
                        evaluateAfterMove(board, b, playerId, amountOfWallsLeft)
                ));
                int half = moves.size() / 2 + 1;
                return moves.get(random.nextInt(half));
            }
            case HARD -> {
                moves.sort((a, b) -> Integer.compare(
                        evaluateAfterMove(board, b, playerId, amountOfWallsLeft),
                        evaluateAfterMove(board, a, playerId, amountOfWallsLeft)
                ));
                int top = Math.min(3, moves.size());
                return moves.get(random.nextInt(top));
            }
        }
        return moves.get(random.nextInt(moves.size()));
    }

    private int evaluateAfterMove(Board board, Move move, int playerId, int wallsLeft) {
        Board copy = board.copy();
        applyMove(copy, move);
        int size = (int) Math.sqrt(copy.getTiles().size());
        return evaluate(copy, playerId, size, playerId == 1 ? wallsLeft : 0, playerId == 2 ? wallsLeft : 0);
    }
}