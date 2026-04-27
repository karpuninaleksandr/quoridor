package ru.ac.uniyar.model.algorithms;

import ru.ac.uniyar.model.*;
import ru.ac.uniyar.model.enums.*;

import java.util.*;

public class AlphaBetaAlgorithm implements Algorithm {
    /**
     * уже посчитанные ранее позиции
     */
    private final Map<String, Integer> cache = new HashMap<>();
    /**
     * ходы, которые часто приводят к хорошим отсечениям
     */
    private final Map<String, Integer> goodMoves = new HashMap<>();
    /**
     * лучшие ходы, которые приводят к beta <= alpha
     */
    private final Move[][] bestMoves = new Move[50][2];

    @Override
    public ComputerAlgorithmType getType() {
        return ComputerAlgorithmType.ALPHABETA;
    }

    /**
     * пока есть время, итеративно увеличиваем глубину поиска решения
     * если время кончилось -> отдаем лучшее, что нашли
     * AlphaBeta ищет глубже MiniMax за счет сортировки ходов и отсечений
     */
    @Override
    public Move getMove(Board board, ComputerPlayerHardnessLevel level, int playerId, int wallsLeft) {
        int size = (int) Math.sqrt(board.getTiles().size());
        int multiplier = Math.max(1, size / GameSize.NORMAL.getAmountOfTilesPerSide());
        int maxDepth = switch (level) {
            case EASY -> 4 * multiplier;
            case MEDIUM -> 6 * multiplier;
            case HARD -> 8 * multiplier;
        };
        long endTime = System.currentTimeMillis() + getTimeLimit(level, size);

        cache.clear();

        Move bestMove = null;
        for (int depth = 1; depth <= maxDepth; ++depth) {
            if (System.currentTimeMillis() > endTime) {
                break;
            }
            Move move = search(board, depth, playerId, size, wallsLeft, wallsLeft, endTime);
            if (move != null) {
                bestMove = move;
            }
        }
        return bestMove;
    }

    /**
     * генерируем всевозможные ходы, сортируем и отбираем из них топ кандидатов
     * потом прогоняем через alpha-beta функцию оценки
     */
    private Move search(Board board, int depth, int playerId, int size,
                        int wallsLeft1, int wallsLeft2, long endTime) {
        List<Move> moves = getMoves(board, playerId, wallsLeft1);
        orderMoves(moves, board, playerId, size, wallsLeft1, wallsLeft2, 0);
        if (moves.size() > 36) {
            moves = moves.subList(0, 36);
        }

        Move bestMove = null;
        int best = Integer.MIN_VALUE;
        for (Move move : moves) {
            if (System.currentTimeMillis() > endTime) {
                break;
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
            int val = alphaBeta(copy, depth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, false,
                    playerId, size, newWallsLeft1, newWallsLeft2, endTime, 1);
            if (val > best) {
                best = val;
                bestMove = move;
            }
        }
        return bestMove;
    }

    /**
     * если max = true (наш ход) - максимизируем оценку - результат в alpha (нижней границе)
     * если max = false (ход противника) - минимизируем - результат в beta (верхней границе)
     * если beta <= alpha -> можно не исследовать дальше, отсечение
     */
    private int alphaBeta(Board board, int depth, int alpha, int beta, boolean max,
                          int playerId, int size, int wallsLeft1, int wallsLeft2,
                          long endTime, int ply) {
        if (System.currentTimeMillis() > endTime) {
            return evaluate(board, playerId, size, wallsLeft1, wallsLeft2);
        }
        String key = boardKey(board) + "|" + depth + "|" + max + "|" + wallsLeft1 + "|" + wallsLeft2;
        Integer cached = cache.get(key);
        if (cached != null) {
            return cached;
        }

        if (depth == 0) {
            int eval = evaluate(board, playerId, size, wallsLeft1, wallsLeft2);
            cache.put(key, eval);
            return eval;
        }

        int current = max ? playerId : 3 - playerId;
        int walls = current == 1 ? wallsLeft1 : wallsLeft2;

        List<Move> moves = getMoves(board, current, walls);
        orderMoves(moves, board, playerId, size, wallsLeft1, wallsLeft2, ply);
        if (moves.size() > 36) {
            moves = moves.subList(0, 36);
        }

        int best = max ? Integer.MIN_VALUE : Integer.MAX_VALUE;

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

            int val = alphaBeta(copy, depth - 1, alpha, beta, !max,
                    playerId, size, newWallsLeft1, newWallsLeft2, endTime, ply + 1);

            if (max) {
                best = Math.max(best, val);
                alpha = Math.max(alpha, val);
            } else {
                best = Math.min(best, val);
                beta = Math.min(beta, val);
            }

            if (beta <= alpha) {
                updateBestMoves(move, ply);
                updateGoodMoves(move);
                break;
            }
        }

        cache.put(key, best);
        return best;
    }

    /**
     * сортируем ходы
     */
    private void orderMoves(List<Move> moves, Board board, int playerId, int size,
                            int wallsLeft1, int wallsLeft2, int ply) {
        moves.sort((a, b) -> Integer.compare(
                scoreMove(b, board, playerId, size, wallsLeft1, wallsLeft2, ply),
                scoreMove(a, board, playerId, size, wallsLeft1, wallsLeft2, ply)
        ));
    }

    /**
     * оцениваем ход для корректной сортировки с помощью функции evaluate из класса Algorithm
     * будет плюсом наличие этого хода в истории хороших или лучших ходов
     */
    private int scoreMove(Move move, Board board, int playerId, int size,
                          int wallsLeft1, int wallsLeft2, int ply) {
        int score = 0;

        if (ply < bestMoves.length) {
            if (bestMoves[ply][0] != null && move.equals(bestMoves[ply][0])) {
                score += 10000;
            }
            if (bestMoves[ply][1] != null && move.equals(bestMoves[ply][1])) {
                score += 8000;
            }
        }

        score += goodMoves.getOrDefault(move.toString(), 0) * 2;

        Board copy = board.copy();
        applyMove(copy, move);
        score += evaluate(copy, playerId, size, wallsLeft1, wallsLeft2);

        return score;
    }

    /**
     * сохраняем лучший ход
     */
    private void updateBestMoves(Move move, int ply) {
        if (ply >= bestMoves.length || move.equals(bestMoves[ply][0])) {
            return;
        }
        bestMoves[ply][1] = bestMoves[ply][0];
        bestMoves[ply][0] = move;
    }

    /**
     * добавляем хороший ход в историю
     */
    private void updateGoodMoves(Move move) {
        goodMoves.merge(move.toString(), 1, Integer::sum);
    }
}
