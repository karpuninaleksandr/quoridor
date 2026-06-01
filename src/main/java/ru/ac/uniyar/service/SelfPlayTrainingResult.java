package ru.ac.uniyar.service;

import ru.ac.uniyar.model.algorithms.EvaluationWeights;

public record SelfPlayTrainingResult(
        int games,
        int updatedGames,
        int wins1,
        int wins2,
        int draws,
        double averageMoves,
        EvaluationWeights before,
        EvaluationWeights after
) {
}
