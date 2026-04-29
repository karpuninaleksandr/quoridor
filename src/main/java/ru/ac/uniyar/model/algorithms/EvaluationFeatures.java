package ru.ac.uniyar.model.algorithms;

public record EvaluationFeatures(
        int pathAdvantage,
        int myMobility,
        int enemyMobilityPenalty,
        int wallAdvantage,
        int progressAdvantage,
        int myEndgame,
        int enemyEndgamePenalty
) {
    public int score(EvaluationWeights weights) {
        return pathAdvantage * weights.pathAdvantage()
                + myMobility * weights.myMobility()
                + enemyMobilityPenalty * weights.enemyMobility()
                + wallAdvantage * weights.wallAdvantage()
                + progressAdvantage * weights.progressAdvantage()
                + myEndgame * weights.myEndgame()
                + enemyEndgamePenalty * weights.enemyEndgame();
    }

    public EvaluationFeatures minus(EvaluationFeatures other) {
        return new EvaluationFeatures(
                pathAdvantage - other.pathAdvantage,
                myMobility - other.myMobility,
                enemyMobilityPenalty - other.enemyMobilityPenalty,
                wallAdvantage - other.wallAdvantage,
                progressAdvantage - other.progressAdvantage,
                myEndgame - other.myEndgame,
                enemyEndgamePenalty - other.enemyEndgamePenalty
        );
    }
}
