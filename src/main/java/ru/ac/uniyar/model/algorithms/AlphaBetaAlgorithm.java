package ru.ac.uniyar.model.algorithms;

import ru.ac.uniyar.model.Board;
import ru.ac.uniyar.model.Move;
import ru.ac.uniyar.model.enums.ComputerAlgorithmType;
import ru.ac.uniyar.model.enums.ComputerPlayerHardnessLevel;

import java.util.*;

public class AlphaBetaAlgorithm implements Algorithm {
    @Override
    public ComputerAlgorithmType getType() {
        return ComputerAlgorithmType.ALPHABETA;
    }

    @Override
    public Move getMove(Board board, ComputerPlayerHardnessLevel hardnessLevel, int playerId) {

        int depth = switch (hardnessLevel) {
            case EASY -> 1;
            case MEDIUM -> 2;
            case HARD -> 3;
        };

        int size = (int) Math.sqrt(board.getTiles().size());

        List<Move> moves = getMoves(board, playerId);

        Move bestMove = null;
        int bestValue = Integer.MIN_VALUE;

        for (Move move : moves) {
            Board copy = board.copy();
            applyMove(copy, move);

            int eval = alphaBeta(copy, depth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, false, playerId, size);

            if (eval > bestValue) {
                bestValue = eval;
                bestMove = move;
            }
        }

        return bestMove;
    }

    private void applyMove(Board board, Move move) {
        if (move.getPlayerId() == 1) {
            board.setPositionOfPlayer1(move.getEndPosition());
        } else {
            board.setPositionOfPlayer2(move.getEndPosition());
        }
    }

    private int alphaBeta(Board board, int depth, int alpha, int beta, boolean maximizing, int playerId, int size) {
        if (depth == 0) {
            return evaluate(board, playerId, size);
        }

        List<Move> moves = getMoves(board, maximizing ? playerId : (3 - playerId));

        if (maximizing) {
            int maxEval = Integer.MIN_VALUE;

            for (Move move : moves) {
                Board copy = board.copy();
                applyMove(copy, move);

                int eval = alphaBeta(copy, depth - 1, alpha, beta, false, playerId, size);

                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, eval);

                if (beta <= alpha) break;
            }

            return maxEval;
        } else {
            int minEval = Integer.MAX_VALUE;

            for (Move move : moves) {
                Board copy = board.copy();
                applyMove(copy, move);

                int eval = alphaBeta(copy, depth - 1, alpha, beta, true, playerId, size);

                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);

                if (beta <= alpha) break;
            }

            return minEval;
        }
    }

    private List<Move> getMoves(Board board, int playerId) {
        String pos = playerId == 1
                ? board.getPositionOfPlayer1()
                : board.getPositionOfPlayer2();

        return board.getAvailableMoves(pos).stream()
                .map(p -> Move.movePlayer(playerId, p))
                .toList();
    }

    private int shortestPath(Board board, String start, int targetRow) {
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();

        queue.add(start);
        visited.add(start);

        int depth = 0;

        while (!queue.isEmpty()) {
            int size = queue.size();

            for (int k = 0; k < size; k++) {
                String cur = queue.poll();

                int i = cur.charAt(0) - '0';
                if (i == targetRow) return depth;

                for (String next : board.getAvailableMoves(cur)) {
                    if (!visited.contains(next)) {
                        visited.add(next);
                        queue.add(next);
                    }
                }
            }
            depth++;
        }

        return 1000;
    }

    private int evaluate(Board board, int playerId, int size) {
        int myDist = shortestPath(board, getMyPosition(board, playerId), getTargetRow(playerId, size));
        int enemyDist = shortestPath(board, getMyPosition(board, 3 - playerId), getTargetRow(3 - playerId, size));

        return enemyDist - myDist;
    }

    private int getTargetRow(int playerId, int size) {
        return playerId == 1 ? size - 1 : 0;
    }

    private String getMyPosition(Board board, int playerId) {
        return playerId == 1
                ? board.getPositionOfPlayer1()
                : board.getPositionOfPlayer2();
    }
}
