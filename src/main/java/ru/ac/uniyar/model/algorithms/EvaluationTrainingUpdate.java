package ru.ac.uniyar.model.algorithms;

public record EvaluationTrainingUpdate(
        boolean applied,
        int winnerId,
        int sampleCount,
        int[] gradient,
        EvaluationWeights before,
        EvaluationWeights after
) {
}
