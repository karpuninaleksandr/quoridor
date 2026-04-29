package ru.ac.uniyar.model.algorithms;

import ru.ac.uniyar.model.Board;
import ru.ac.uniyar.model.Move;
import ru.ac.uniyar.model.Position;
import ru.ac.uniyar.model.enums.ComputerAlgorithmType;
import ru.ac.uniyar.model.enums.ComputerPlayerHardnessLevel;
import ru.ac.uniyar.model.enums.MoveType;

import java.util.*;

public class RandomAlgorithm implements Algorithm {
    private static final Random random = new Random();
    private AlgorithmReport lastReport;
    private List<Position> recentPositions = List.of();

    @Override
    public ComputerAlgorithmType getType() {
        return ComputerAlgorithmType.RANDOM;
    }

    @Override
    public AlgorithmReport getLastReport() {
        return lastReport;
    }

    @Override
    public void setRecentPositions(List<Position> recentPositions) {
        this.recentPositions = recentPositions == null ? List.of() : List.copyOf(recentPositions);
    }

    @Override
    public Move getMove(Board board, ComputerPlayerHardnessLevel hardnessLevel, int playerId, int wallsLeft1, int wallsLeft2) {
        long startedAt = System.currentTimeMillis();
        int amountOfWallsLeft = playerId == 1 ? wallsLeft1 : wallsLeft2;
        List<Move> moves = getMoves(board, playerId, amountOfWallsLeft);

        if (moves.isEmpty()) return null;

        Move tacticalMove = findEndgameMove(board, playerId, amountOfWallsLeft);
        if (tacticalMove != null) {
            lastReport = new AlgorithmReport(getType().getDescription(), tacticalMove,
                    evaluateAfterMove(board, tacticalMove, playerId, wallsLeft1, wallsLeft2),
                    0, 1, 1, 0, 0,
                    System.currentTimeMillis() - startedAt,
                    "Случайный алгоритм применил эндшпильное правило перед случайным выбором");
            return tacticalMove;
        }

        Move result;
        switch (hardnessLevel) {
            case MEDIUM -> {
                moves.sort((a, b) -> Integer.compare(
                        evaluateAfterMove(board, b, playerId, wallsLeft1, wallsLeft2),
                        evaluateAfterMove(board, a, playerId, wallsLeft1, wallsLeft2)
                ));
                int half = moves.size() / 2 + 1;
                result = moves.get(random.nextInt(half));
            }
            case HARD -> {
                moves.sort((a, b) -> Integer.compare(
                        evaluateAfterMove(board, b, playerId, wallsLeft1, wallsLeft2),
                        evaluateAfterMove(board, a, playerId, wallsLeft1, wallsLeft2)
                ));
                int top = Math.min(3, moves.size());
                result = moves.get(random.nextInt(top));
            }
            default -> result = moves.get(random.nextInt(moves.size()));
        }
        lastReport = new AlgorithmReport(getType().getDescription(), result,
                evaluateAfterMove(board, result, playerId, wallsLeft1, wallsLeft2),
                1, moves.size(), moves.size(), 0, 0,
                System.currentTimeMillis() - startedAt,
                describeHardness(hardnessLevel));
        return result;
    }

    private String describeHardness(ComputerPlayerHardnessLevel hardnessLevel) {
        return switch (hardnessLevel) {
            case EASY -> "Случайный выбор среди всех допустимых ходов";
            case MEDIUM -> "Случайный выбор из верхней половины ходов по функции оценки";
            case HARD -> "Случайный выбор из трех лучших ходов по функции оценки";
        };
    }

    private int evaluateAfterMove(Board board, Move move, int playerId, int wallsLeft1, int wallsLeft2) {
        Board copy = board.copy();
        applyMove(copy, move);
        int size = (int) Math.sqrt(copy.getTiles().size());
        int newWallsLeft1 = wallsLeft1;
        int newWallsLeft2 = wallsLeft2;
        if (move.getMoveType() == MoveType.PLACE_WALL) {
            if (move.getPlayerId() == 1) {
                --newWallsLeft1;
            } else {
                --newWallsLeft2;
            }
        }
        return evaluate(copy, playerId, size, newWallsLeft1, newWallsLeft2)
                + movementPreference(move, board, playerId, size, recentPositions);
    }
}
