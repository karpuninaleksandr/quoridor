package ru.ac.uniyar.model.algorithms;

public record EvaluationLearningSample(
        int playerId,
        EvaluationFeatures before,
        EvaluationFeatures after
) {
    public EvaluationFeatures delta() {
        return after.minus(before);
    }
}
