package ru.ac.uniyar.model.algorithms;

import ru.ac.uniyar.model.*;
import ru.ac.uniyar.model.enums.*;

import java.util.*;

public class AlphaBetaAlgorithm implements Algorithm {
    private final Map<String, Integer> transposition = new HashMap<>();
    private final Map<String, Integer> history = new HashMap<>();
    private final Move[][] bestMovesInHistory = new Move[50][2];

    @Override
    public ComputerAlgorithmType getType() {
        return ComputerAlgorithmType.ALPHABETA;
    }

    @Override
    public Move getMove(Board board, ComputerPlayerHardnessLevel level, int playerId, int wallsLeft) {
        int size = (int) Math.sqrt(board.getTiles().size());

        int maxDepth = switch (level) {
            case EASY -> 4 * (size / GameSize.NORMAL.getAmountOfTilesPerSide());
            case MEDIUM -> 7 * (size / GameSize.NORMAL.getAmountOfTilesPerSide());
            case HARD -> 12 * (size / GameSize.NORMAL.getAmountOfTilesPerSide());
        };

        long endTime = System.currentTimeMillis() + getTimeLimit(level, size);

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

    private Move search(Board board, int depth, int playerId, int size, int wallsLeft1, int wallsLeft2, long endTime) {
        List<Move> moves = getMoves(board, playerId, wallsLeft1);
        orderMoves(moves, board, playerId, size, wallsLeft1, wallsLeft2, 0);

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
                }
                else --newWallsLeft2;
            }

            int val = alphaBeta(copy, depth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, false, playerId, size,
                    newWallsLeft1, newWallsLeft2, endTime, 1);

            if (val > best) {
                best = val;
                bestMove = move;
            }
        }

        return bestMove;
    }

    private int alphaBeta(Board board, int depth, int alpha, int beta, boolean max, int playerId, int size, int wallsLeft1,
                          int wallsLeft2, long endTime, int ply) {
        if (System.currentTimeMillis() > endTime) return 0;

        String key = board.toString() + depth + wallsLeft1 + wallsLeft2;
        Integer cached = transposition.get(key);
        if (cached != null) return cached;

        if (depth == 0) {
            int eval = evaluate(board, playerId, size, wallsLeft1, wallsLeft2);
            transposition.put(key, eval);
            return eval;
        }

        int current = max ? playerId : 3 - playerId;
        int walls = current == 1 ? wallsLeft1 : wallsLeft2;

        List<Move> moves = getMoves(board, current, walls);
        orderMoves(moves, board, playerId, size, wallsLeft1, wallsLeft2, ply);

        int best = max ? Integer.MIN_VALUE : Integer.MAX_VALUE;

        for (Move move : moves) {
            Board copy = board.copy();
            applyMove(copy, move);

            int newWallsLeft1 = wallsLeft1, newWallsLeft2 = wallsLeft2;
            if (move.getMoveType() == MoveType.PLACE_WALL) {
                if (current == 1) {
                    --newWallsLeft1;
                }
                else --newWallsLeft2;
            }

            int val = alphaBeta(copy, depth - 1, alpha, beta, !max, playerId, size, newWallsLeft1, newWallsLeft2,
                    endTime, ply + 1);

            if (max) {
                if (val > best) best = val;
                alpha = Math.max(alpha, val);
            } else {
                if (val < best) best = val;
                beta = Math.min(beta, val);
            }

            if (beta <= alpha) {
                storeKiller(move, ply);
                updateHistory(move, depth);
                break;
            }
        }

        transposition.put(key, best);
        return best;
    }

    private void orderMoves(List<Move> moves, Board board, int playerId, int size, int wallsLeft1, int wallsLeft2, int ply) {
        moves.sort((a, b) -> {
            int scoreA = scoreMove(a, board, playerId, size, wallsLeft1, wallsLeft2, ply);
            int scoreB = scoreMove(b, board, playerId, size, wallsLeft1, wallsLeft2, ply);
            return Integer.compare(scoreB, scoreA);
        });
    }

    private int scoreMove(Move move, Board board, int playerId, int size, int wallsLeft1, int wallsLeft2, int ply) {
        int score = 0;

        if (move.equals(bestMovesInHistory[ply][0])) {
            score += 10000;
        }
        if (move.equals(bestMovesInHistory[ply][1])) {
            score += 8000;
        }

        score += history.getOrDefault(move.toString(), 0);

        Board copy = board.copy();
        applyMove(copy, move);
        score += evaluate(copy, playerId, size, wallsLeft1, wallsLeft2);

        return score;
    }

    private void storeKiller(Move move, int ply) {
        bestMovesInHistory[ply][1] = bestMovesInHistory[ply][0];
        bestMovesInHistory[ply][0] = move;
    }

    private void updateHistory(Move move, int depth) {
        history.put(move.toString(), history.getOrDefault(move.toString(), 0) + depth * depth);
    }
}