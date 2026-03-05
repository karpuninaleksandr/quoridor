package ru.ac.uniyar.model.enums;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum ComputerAlgorithmType {
    RANDOM("Случайный"),
    MINIMAX("MiniMax"),
    MONTECARLO("MonteCarlo"),
    ALPHABETA("AlphaBeta");

    private final String description;

    ComputerAlgorithmType(String description) {
        this.description = description;
    }

    public static ComputerAlgorithmType findByDescription(String description) {
        return Arrays.stream(ComputerAlgorithmType.values()).filter(it -> it.description.equals(description)).findFirst().get();
    }
}
