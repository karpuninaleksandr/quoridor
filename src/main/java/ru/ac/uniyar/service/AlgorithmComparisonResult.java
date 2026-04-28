package ru.ac.uniyar.service;

public record AlgorithmComparisonResult(
        String algorithm1,
        String algorithm2,
        int games,
        int wins1,
        int wins2,
        int draws,
        double averageMoves
) {
}
