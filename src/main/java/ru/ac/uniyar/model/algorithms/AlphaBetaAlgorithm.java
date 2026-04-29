package ru.ac.uniyar.model.algorithms;

import ru.ac.uniyar.model.*;
import ru.ac.uniyar.model.enums.*;

import java.util.*;

public class AlphaBetaAlgorithm implements Algorithm {
    /**
     * transposition table хранит оценку позиции вместе с глубиной и типом границы
     */
    private final Map<String, TranspositionEntry> transpositionTable = new HashMap<>();
    /**
     * ходы, которые часто приводят к хорошим отсечениям
     */
    private final Map<String, Integer> goodMoves = new HashMap<>();
    /**
     * лучшие ходы, которые приводят к beta <= alpha
     */
    private final Move[][] bestMoves = new Move[50][2];
    private AlgorithmReport lastReport;
    private long nodesVisited;
    private long consideredMoves;
    private long cutoffs;
    private long tableHits;
    private List<Position> recentPositions = List.of();

    @Override
    public ComputerAlgorithmType getType() {
        return ComputerAlgorithmType.ALPHABETA;
    }

    @Override
    public AlgorithmReport getLastReport() {
        return lastReport;
    }

    @Override
    public void setRecentPositions(List<Position> recentPositions) {
        this.recentPositions = recentPositions == null ? List.of() : List.copyOf(recentPositions);
    }

    /**
     * пока есть время, итеративно увеличиваем глубину поиска решения
     * если время кончилось -> отдаем лучшее, что нашли
     * AlphaBeta ищет глубже MiniMax за счет сортировки ходов и отсечений
     */
    @Override
    public Move getMove(Board board, ComputerPlayerHardnessLevel level, int playerId, int wallsLeft1, int wallsLeft2) {
        long startedAt = System.currentTimeMillis();
        int size = (int) Math.sqrt(board.getTiles().size());
        int maxDepth = getMaxDepth(level, size);
        long timeLimit = getTimeLimit(level, size);
        long endTime = System.currentTimeMillis() + timeLimit;

        transpositionTable.clear();
        nodesVisited = 0;
        consideredMoves = 0;
        cutoffs = 0;
        tableHits = 0;

        int currentWalls = playerId == 1 ? wallsLeft1 : wallsLeft2;
        Move tacticalMove = findEndgameMove(board, playerId, currentWalls);
        if (tacticalMove != null) {
            int tacticalScore = scoreMoveAfterApply(board, tacticalMove, playerId, size, wallsLeft1, wallsLeft2);
            lastReport = new AlgorithmReport(
                    getType().getDescription(),
                    tacticalMove,
                    tacticalScore,
                    0,
                    nodesVisited,
                    1,
                    cutoffs,
                    tableHits,
                    System.currentTimeMillis() - startedAt,
                    "AlphaBeta применил эндшпильное правило: немедленная победа или срочная блокировка"
            );
            return tacticalMove;
        }

        Move bestMove = null;
        int bestScore = 0;
        int reachedDepth = 0;
        for (int depth = 1; depth <= maxDepth; ++depth) {
            if (System.currentTimeMillis() > endTime) {
                break;
            }
            SearchResult result = search(board, depth, playerId, size, wallsLeft1, wallsLeft2, endTime);
            if (result.move() != null) {
                bestMove = result.move();
                bestScore = result.score();
                reachedDepth = depth;
            }
        }
        lastReport = new AlgorithmReport(
                getType().getDescription(),
                bestMove,
                bestScore,
                reachedDepth,
                nodesVisited,
                consideredMoves,
                cutoffs,
                tableHits,
                System.currentTimeMillis() - startedAt,
                "AlphaBeta использует сортировку ходов, beta <= alpha отсечения и transposition table"
                        + " с depth-bound flags; попаданий в таблицу: " + tableHits
                        + "; лимит времени: " + timeLimit + " мс"
        );
        return bestMove;
    }

    private int getMaxDepth(ComputerPlayerHardnessLevel level, int size) {
        boolean smallBoard = size <= GameSize.SMALL.getAmountOfTilesPerSide();
        boolean largeBoard = size > GameSize.NORMAL.getAmountOfTilesPerSide();
        return switch (level) {
            case EASY -> smallBoard ? 5 : 4;
            case MEDIUM -> smallBoard ? 7 : 6;
            case HARD -> largeBoard ? 7 : 8;
        };
    }

    @Override
    public long getTimeLimit(ComputerPlayerHardnessLevel level, int size) {
        boolean largeBoard = size > GameSize.NORMAL.getAmountOfTilesPerSide();
        boolean smallBoard = size <= GameSize.SMALL.getAmountOfTilesPerSide();
        return switch (level) {
            case EASY -> smallBoard ? 1000L : 1300L;
            case MEDIUM -> largeBoard ? 4800L : 3600L;
            case HARD -> largeBoard ? 9500L : 7600L;
        };
    }

    /**
     * генерируем всевозможные ходы, сортируем и отбираем из них топ кандидатов
     * потом прогоняем через alpha-beta функцию оценки
     */
    private SearchResult search(Board board, int depth, int playerId, int size,
                                int wallsLeft1, int wallsLeft2, long endTime) {
        List<Move> moves = new ArrayList<>(getMoves(board, playerId, wallsLeft1).stream()
                .filter(move -> isRootMoveLegal(board, move, playerId, wallsLeft1))
                .toList());
        orderMoves(moves, board, playerId, size, wallsLeft1, wallsLeft2, 0, null);
        if (moves.size() > 36) {
            moves = moves.subList(0, 36);
        }
        consideredMoves += moves.size();

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
            val += rootMovePreference(move, board, playerId, size);
            if (val > best) {
                best = val;
                bestMove = move;
            }
        }
        return new SearchResult(bestMove, best);
    }

    /**
     * если max = true (наш ход) - максимизируем оценку - результат в alpha (нижней границе)
     * если max = false (ход противника) - минимизируем - результат в beta (верхней границе)
     * если beta <= alpha -> можно не исследовать дальше, отсечение
     */
    private int alphaBeta(Board board, int depth, int alpha, int beta, boolean max,
                          int playerId, int size, int wallsLeft1, int wallsLeft2,
                          long endTime, int ply) {
        nodesVisited++;
        if (System.currentTimeMillis() > endTime) {
            return evaluate(board, playerId, size, wallsLeft1, wallsLeft2);
        }
        String key = boardKey(board) + "|" + max + "|" + wallsLeft1 + "|" + wallsLeft2;
        int originalAlpha = alpha;
        int originalBeta = beta;

        TranspositionEntry cached = transpositionTable.get(key);
        if (cached != null && cached.depth >= depth) {
            if (cached.bound == Bound.EXACT) {
                tableHits++;
                return cached.score;
            }
            if (cached.bound == Bound.LOWER) {
                alpha = Math.max(alpha, cached.score);
            } else if (cached.bound == Bound.UPPER) {
                beta = Math.min(beta, cached.score);
            }
            if (alpha >= beta) {
                tableHits++;
                return cached.score;
            }
        }

        Integer terminal = terminalScore(board, playerId, size, depth);
        if (terminal != null) {
            transpositionTable.put(key, new TranspositionEntry(depth, terminal, Bound.EXACT, null));
            return terminal;
        }

        if (depth == 0) {
            if (isNoisyPosition(board, playerId, size)) {
                int calmScore = quiescence(board, alpha, beta, max, playerId, size,
                        wallsLeft1, wallsLeft2, endTime, 2);
                transpositionTable.put(key, new TranspositionEntry(depth, calmScore, Bound.EXACT, null));
                return calmScore;
            }
            int eval = evaluate(board, playerId, size, wallsLeft1, wallsLeft2);
            transpositionTable.put(key, new TranspositionEntry(depth, eval, Bound.EXACT, null));
            return eval;
        }

        int current = max ? playerId : 3 - playerId;
        int walls = current == 1 ? wallsLeft1 : wallsLeft2;

        List<Move> moves = getMoves(board, current, walls);
        Move tableBestMove = cached == null ? null : cached.bestMove;
        orderMoves(moves, board, playerId, size, wallsLeft1, wallsLeft2, ply, tableBestMove);
        if (moves.size() > 36) {
            moves = moves.subList(0, 36);
        }
        consideredMoves += moves.size();

        int best = max ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        Move bestMove = null;

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
                if (val > best) {
                    best = val;
                    bestMove = move;
                }
                alpha = Math.max(alpha, val);
            } else {
                if (val < best) {
                    best = val;
                    bestMove = move;
                }
                beta = Math.min(beta, val);
            }

            if (beta <= alpha) {
                cutoffs++;
                updateBestMoves(move, ply);
                updateGoodMoves(move);
                break;
            }
        }

        Bound bound = Bound.EXACT;
        if (best <= originalAlpha) {
            bound = Bound.UPPER;
        } else if (best >= originalBeta) {
            bound = Bound.LOWER;
        }
        transpositionTable.put(key, new TranspositionEntry(depth, best, bound, bestMove));
        return best;
    }

    private int quiescence(Board board, int alpha, int beta, boolean max,
                           int playerId, int size, int wallsLeft1, int wallsLeft2,
                           long endTime, int extensionDepth) {
        nodesVisited++;
        int standPat = evaluate(board, playerId, size, wallsLeft1, wallsLeft2);
        if (extensionDepth == 0 || System.currentTimeMillis() > endTime) {
            return standPat;
        }

        if (max) {
            if (standPat >= beta) {
                return beta;
            }
            alpha = Math.max(alpha, standPat);
        } else {
            if (standPat <= alpha) {
                return alpha;
            }
            beta = Math.min(beta, standPat);
        }

        int current = max ? playerId : 3 - playerId;
        int walls = current == 1 ? wallsLeft1 : wallsLeft2;
        List<Move> moves = getQuiescenceMoves(board, current, walls, playerId, size);
        if (moves.isEmpty()) {
            return standPat;
        }
        orderMoves(moves, board, playerId, size, wallsLeft1, wallsLeft2, 0, null);

        for (Move move : moves) {
            if (System.currentTimeMillis() > endTime) {
                break;
            }
            Board copy = board.copy();
            applyMove(copy, move);

            int newWallsLeft1 = wallsLeft1;
            int newWallsLeft2 = wallsLeft2;
            if (move.getMoveType() == MoveType.PLACE_WALL) {
                if (current == 1) {
                    --newWallsLeft1;
                } else {
                    --newWallsLeft2;
                }
            }

            int value = quiescence(copy, alpha, beta, !max, playerId, size,
                    newWallsLeft1, newWallsLeft2, endTime, extensionDepth - 1);
            if (max) {
                if (value >= beta) {
                    cutoffs++;
                    return beta;
                }
                alpha = Math.max(alpha, value);
            } else {
                if (value <= alpha) {
                    cutoffs++;
                    return alpha;
                }
                beta = Math.min(beta, value);
            }
        }
        return max ? alpha : beta;
    }

    /**
     * сортируем ходы
     */
    private void orderMoves(List<Move> moves, Board board, int playerId, int size,
                            int wallsLeft1, int wallsLeft2, int ply, Move tableBestMove) {
        moves.sort((a, b) -> Integer.compare(
                scoreMove(b, board, playerId, size, wallsLeft1, wallsLeft2, ply, tableBestMove),
                scoreMove(a, board, playerId, size, wallsLeft1, wallsLeft2, ply, tableBestMove)
        ));
    }

    /**
     * оцениваем ход для корректной сортировки с помощью функции evaluate из класса Algorithm
     * будет плюсом наличие этого хода в истории хороших или лучших ходов
     */
    private int scoreMove(Move move, Board board, int playerId, int size,
                          int wallsLeft1, int wallsLeft2, int ply, Move tableBestMove) {
        int score = 0;

        if (move.equals(tableBestMove)) {
            score += 20000;
        }

        if (ply < bestMoves.length) {
            if (bestMoves[ply][0] != null && move.equals(bestMoves[ply][0])) {
                score += 10000;
            }
            if (bestMoves[ply][1] != null && move.equals(bestMoves[ply][1])) {
                score += 8000;
            }
        }

        score += goodMoves.getOrDefault(move.toString(), 0) * 2;
        score += rootMovePreference(move, board, playerId, size);

        score += scoreMoveAfterApply(board, move, playerId, size, wallsLeft1, wallsLeft2);

        return score;
    }

    private int scoreMoveAfterApply(Board board, Move move, int playerId, int size, int wallsLeft1, int wallsLeft2) {
        Board copy = board.copy();
        applyMove(copy, move);
        int newWallsLeft1 = wallsLeft1;
        int newWallsLeft2 = wallsLeft2;
        if (move.getMoveType() == MoveType.PLACE_WALL) {
            if (move.getPlayerId() == 1) {
                --newWallsLeft1;
            } else {
                --newWallsLeft2;
            }
        }
        return evaluate(copy, playerId, size, newWallsLeft1, newWallsLeft2);
    }

    private int rootMovePreference(Move move, Board board, int playerId, int size) {
        return movementPreference(move, board, playerId, size, recentPositions);
    }

    private boolean isRootMoveLegal(Board board, Move move, int playerId, int wallsLeft) {
        if (move.getPlayerId() != playerId) {
            return false;
        }
        if (move.getMoveType() == MoveType.MOVE_PLAYER) {
            return board.getAvailableMoves(getCurrentPosition(board, playerId)).contains(move.getEndPosition());
        }
        if (wallsLeft <= 0) {
            return false;
        }
        return isWallPlaceable(board, move.getStartPosition(), move.getEndPosition())
                && isValidWall(board, move.getStartPosition(), move.getEndPosition());
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

    private record SearchResult(Move move, int score) {
    }

    private enum Bound {
        EXACT,
        LOWER,
        UPPER
    }

    private record TranspositionEntry(int depth, int score, Bound bound, Move bestMove) {
    }
}
