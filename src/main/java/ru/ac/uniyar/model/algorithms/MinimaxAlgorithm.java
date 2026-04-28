package ru.ac.uniyar.model.algorithms;

import ru.ac.uniyar.model.Board;
import ru.ac.uniyar.model.Move;
import ru.ac.uniyar.model.enums.*;

import java.util.*;

public class MinimaxAlgorithm implements Algorithm {
    /**
     * уже посчитанные ранее позиции
     */
    private final Map<String, Integer> cache = new HashMap<>();
    private AlgorithmReport lastReport;
    private long nodesVisited;
    private long consideredMoves;

    @Override
    public ComputerAlgorithmType getType() {
        return ComputerAlgorithmType.MINIMAX;
    }

    @Override
    public AlgorithmReport getLastReport() {
        return lastReport;
    }

    /**
     * пока есть время, итеративно увеличиваем глубину поиска решения
     * если время кончилось -> отдаем лучшее, что нашли
     * MiniMax оставлен как честный перебор без сортировки и отсечений
     */
    @Override
    public Move getMove(Board board, ComputerPlayerHardnessLevel hardnessLevel, int playerId, int wallsLeft1, int wallsLeft2) {
        int size = (int) Math.sqrt(board.getTiles().size());
        long timeLimit = getTimeLimit(hardnessLevel, size);
        long endTime = System.currentTimeMillis() + timeLimit;
        int maxDepth = getMaxDepth(hardnessLevel, size);

        cache.clear();
        nodesVisited = 0;
        consideredMoves = 0;

        Move best = null;
        int bestScore = 0;
        int reachedDepth = 0;
        for (int depth = 1; depth <= maxDepth; depth++) {
            if (System.currentTimeMillis() > endTime) {
                break;
            }
            SearchResult result = search(board, depth, playerId, size, wallsLeft1, wallsLeft2, endTime);
            if (result.move() != null) {
                best = result.move();
                bestScore = result.score();
                reachedDepth = depth;
            }
        }
        lastReport = new AlgorithmReport(
                getType().getDescription(),
                best,
                bestScore,
                reachedDepth,
                nodesVisited,
                consideredMoves,
                0,
                "MiniMax перебрал дерево без alpha-beta отсечений"
                        + "; лимит времени: " + timeLimit + " мс"
        );
        return best;
    }

    private int getMaxDepth(ComputerPlayerHardnessLevel level, int size) {
        boolean smallBoard = size <= GameSize.SMALL.getAmountOfTilesPerSide();
        boolean largeBoard = size > GameSize.NORMAL.getAmountOfTilesPerSide();
        return switch (level) {
            case EASY -> 2;
            case MEDIUM -> smallBoard ? 4 : 3;
            case HARD -> largeBoard ? 3 : 4;
        };
    }

    @Override
    public long getTimeLimit(ComputerPlayerHardnessLevel level, int size) {
        boolean largeBoard = size > GameSize.NORMAL.getAmountOfTilesPerSide();
        return switch (level) {
            case EASY -> largeBoard ? 500L : 400L;
            case MEDIUM -> largeBoard ? 1100L : 900L;
            case HARD -> largeBoard ? 1900L : 1600L;
        };
    }

    /**
     * генерируем всевозможные ходы, отбираем из них ограниченное количество
     * потом прогоняем через minimax функцию оценки
     */
    private SearchResult search(Board board, int depth, int playerId, int size,
                                int wallsLeft1, int wallsLeft2, long endTime) {
        List<Move> moves = limitMoves(getMoves(board, playerId, wallsLeft1));
        consideredMoves += moves.size();

        Move bestMove = null;
        int bestValue = Integer.MIN_VALUE;
        for (Move move : moves) {
            if (System.currentTimeMillis() > endTime) {
                return new SearchResult(bestMove, bestValue);
            }

            Board copy = board.copy();
            applyMove(copy, move);

            int newWallsLeft1 = wallsLeft1, newWallsLeft2 = wallsLeft2;
            if (move.getMoveType() == MoveType.PLACE_WALL) {
                if (playerId == 1) {
                    --newWallsLeft1;
                } else {
                    --newWallsLeft2;
                }
            }

            int value = minimax(copy, depth - 1, false,
                    playerId, size, newWallsLeft1, newWallsLeft2, endTime);
            if (value > bestValue) {
                bestValue = value;
                bestMove = move;
            }
        }
        return new SearchResult(bestMove, bestValue);
    }

    /**
     * если max = true (наш ход) - максимизируем оценку
     * если max = false (ход противника) - минимизируем
     */
    private int minimax(Board board, int depth, boolean maximizing,
                        int playerId, int size,
                        int wallsLeft1, int wallsLeft2, long endTime) {
        nodesVisited++;
        if (System.currentTimeMillis() > endTime) {
            return evaluate(board, playerId, size, wallsLeft1, wallsLeft2);
        }
        String key = boardKey(board) + "|" + depth + "|" + maximizing + "|" + wallsLeft1 + "|" + wallsLeft2;
        if (cache.containsKey(key)) {
            return cache.get(key);
        }

        Integer terminal = terminalScore(board, playerId, size, depth);
        if (terminal != null) {
            cache.put(key, terminal);
            return terminal;
        }

        if (depth == 0) {
            int val = evaluate(board, playerId, size, wallsLeft1, wallsLeft2);
            cache.put(key, val);
            return val;
        }

        int current = maximizing ? playerId : (3 - playerId);
        int walls = current == 1 ? wallsLeft1 : wallsLeft2;
        List<Move> moves = limitMoves(getMoves(board, current, walls));
        consideredMoves += moves.size();

        int best = maximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        for (Move move : moves) {
            Board copy = board.copy();
            applyMove(copy, move);

            int newWallsLeft1 = wallsLeft1, newWallsLeft2 = wallsLeft2;
            if (move.getMoveType() == MoveType.PLACE_WALL) {
                if (current == 1) {
                    --newWallsLeft1;
                } else {
                    --newWallsLeft2;
                }
            }

            int val = minimax(copy, depth - 1, !maximizing,
                    playerId, size, newWallsLeft1, newWallsLeft2, endTime);
            if (maximizing) {
                best = Math.max(best, val);
            } else {
                best = Math.min(best, val);
            }
        }
        cache.put(key, best);
        return best;
    }

    /**
     * ограничение ширины перебора, чтобы обычный MiniMax не зависал на большом поле
     */
    private List<Move> limitMoves(List<Move> moves) {
        if (moves.size() <= 16) {
            return moves;
        }
        return moves.subList(0, 16);
    }

    private record SearchResult(Move move, int score) {
    }
}
