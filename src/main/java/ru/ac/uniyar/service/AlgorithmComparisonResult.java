package ru.ac.uniyar.service;

public record AlgorithmComparisonResult(
        String algorithm1,
        String algorithm2,
        int games,
        int wins1,
        int wins2,
        int draws,
        double averageMoves,
        long reports1,
        double averageTimeMs1,
        double averageDepth1,
        double averageNodes1,
        double averageCutoffs1,
        double averageTableHits1,
        long reports2,
        double averageTimeMs2,
        double averageDepth2,
        double averageNodes2,
        double averageCutoffs2,
        double averageTableHits2
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
                reports2,
                averageTimeMs2,
                averageDepth2,
                averageNodes2,
                averageCutoffs2,
                averageTableHits2,
                reports1,
                averageTimeMs1,
                averageDepth1,
                averageNodes1,
                averageCutoffs1,
                averageTableHits1
        );
    }
}
