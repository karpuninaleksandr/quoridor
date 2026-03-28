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
        return moves.get(random.nextInt(getMoves(board, playerId, amountOfWallsLeft).size()));
    }
}