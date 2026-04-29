package ru.ac.uniyar.model.algorithms;

public record EvaluationWeights(
        int pathAdvantage,
        int myMobility,
        int enemyMobility,
        int wallAdvantage,
        int progressAdvantage,
        int myEndgame,
        int enemyEndgame,
        long samples
) {
    public static EvaluationWeights defaults() {
        return new EvaluationWeights(
                65,
                4,
                3,
                18,
                6,
                165,
                175,
                0
        );
    }

    public EvaluationWeights adjustedByGameGradient(int[] gradient, int learningStep, int sampleCount) {
        return new EvaluationWeights(
                clamp(pathAdvantage + direction(gradient[0]) * learningStep, 25, 120),
                clamp(myMobility + direction(gradient[1]) * learningStep, 1, 18),
                clamp(enemyMobility + direction(gradient[2]) * learningStep, 1, 18),
                clamp(wallAdvantage + direction(gradient[3]) * learningStep, 4, 45),
                clamp(progressAdvantage + direction(gradient[4]) * learningStep, 2, 25),
                clamp(myEndgame + direction(gradient[5]) * learningStep, 80, 320),
                clamp(enemyEndgame + direction(gradient[6]) * learningStep, 90, 360),
                samples + sampleCount
        );
    }

    private static int direction(int value) {
        return Integer.compare(value, 0);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
