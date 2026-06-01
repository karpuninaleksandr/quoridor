package ru.ac.uniyar.service;

public record WeightsComparisonResult(
        int games,
        int defaultWins,
        int trainedWins,
        int draws,
        double averageMoves,
        double averageTimeMs,
        double averageDepth,
        double averageNodes,
        double averageCutoffs
) {
}
