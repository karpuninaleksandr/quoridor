package ru.ac.uniyar.model.algorithms;

import ru.ac.uniyar.model.Move;

//краткий отчет о последнем ходе ИИ
public record AlgorithmReport(
        String algorithm,
        Move move,
        int score,
        int reachedDepth,
        long nodesVisited,
        long consideredMoves,
        long cutoffs,
        String explanation
) {
}
