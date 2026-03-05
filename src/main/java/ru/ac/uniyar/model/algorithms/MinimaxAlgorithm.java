package ru.ac.uniyar.model.algorithms;

import ru.ac.uniyar.model.Board;
import ru.ac.uniyar.model.Move;
import ru.ac.uniyar.model.enums.ComputerAlgorithmType;
import ru.ac.uniyar.model.enums.ComputerPlayerHardnessLevel;

public class MinimaxAlgorithm implements Algorithm {
    @Override
    public ComputerAlgorithmType getType() {
        return ComputerAlgorithmType.MINIMAX;
    }

    @Override
    public Move getMove(Board board, ComputerPlayerHardnessLevel hardnessLevel) {
        return null;
    }
}
