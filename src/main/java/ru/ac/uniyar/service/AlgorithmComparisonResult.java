package ru.ac.uniyar.service;

public record AlgorithmComparisonResult(
        String algorithm1,
        String algorithm2,
        int games,
        int wins1,
        int wins2,
        int draws,
        double averageMoves,
        double averageTimeMs,
        double averageDepth,
        double averageNodes,
        double averageCutoffs,
        double averageTableHits
) {
    public AlgorithmComparisonResult reversed() {
        return new AlgorithmComparisonResult(
                algorithm2,
                algorithm1,
                games,
                wins2,
                wins1,
                draws,
                averageMoves,
                averageTimeMs,
                averageDepth,
                averageNodes,
                averageCutoffs,
                averageTableHits
        );
    }
}
