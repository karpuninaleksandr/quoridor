package ru.ac.uniyar.model.algorithms;

import ru.ac.uniyar.model.Board;
import ru.ac.uniyar.model.Move;
import ru.ac.uniyar.model.enums.*;

import java.util.*;

public class MonteCarloAlgorithm implements Algorithm {
    private static final double C = 1.2;
    private static final Random random = new Random();

    @Override
    public ComputerAlgorithmType getType() {
        return ComputerAlgorithmType.MONTECARLO;
    }

    @Override
    public Move getMove(Board board, ComputerPlayerHardnessLevel hardnessLevel, int playerId, int wallsLeft) {
        int size = (int) Math.sqrt(board.getTiles().size());

        int rolloutDepth = switch (hardnessLevel) {
            case EASY -> 40 * (size / GameSize.NORMAL.getAmountOfTilesPerSide());
            case MEDIUM -> 80 * (size / GameSize.NORMAL.getAmountOfTilesPerSide());
            case HARD -> 120 * (size / GameSize.NORMAL.getAmountOfTilesPerSide());
        };

        Node root = new Node(board.copy(), null, null, playerId, wallsLeft, wallsLeft);

        while (System.currentTimeMillis() < System.currentTimeMillis() + getTimeLimit(hardnessLevel, size)) {
            Node node = select(root);
            Node expanded = expand(node);

            int winner = simulateSmart(expanded, rolloutDepth, playerId, size);

            backpropagate(expanded, winner, playerId);

            if (root.visits > 200 && bestWinRate(root) > 0.95) {
                break;
            }
        }

        return root.children.stream().max(Comparator.comparing(n -> n.visits)).orElseThrow().move;
    }

    private Node select(Node node) {
        while (!node.children.isEmpty()) {
            node = node.children.stream().max(Comparator.comparing(this::uct)).orElseThrow();
        }
        return node;
    }

    private Node expand(Node node) {
        if (!node.children.isEmpty()) return node;

        List<Move> moves = getMoves(node.board, node.player, node.getWalls());

        for (Move move : moves) {
            Board copy = node.board.copy();
            applyMove(copy, move);

            int w1 = node.walls1;
            int w2 = node.walls2;

            if (move.getMoveType() == MoveType.PLACE_WALL) {
                if (node.player == 1) w1--;
                else w2--;
            }

            node.children.add(new Node(copy, node, move, 3 - node.player, w1, w2));
        }

        return node.children.get(random.nextInt(node.children.size()));
    }

    private int simulateSmart(Node node, int maxSteps, int rootPlayer, int size) {
        Board board = node.board.copy();
        int currentPlayer = node.player;
        int w1 = node.walls1;
        int w2 = node.walls2;

        for (int step = 0; step < maxSteps; step++) {
            if (isWin(board, 1, size)) {
                return 1;
            }
            if (isWin(board, 2, size)) {
                return 2;
            }

            int walls = currentPlayer == 1 ? w1 : w2;
            List<Move> moves = getMoves(board, currentPlayer, walls);

            if (moves.isEmpty()) {
                return 3 - currentPlayer;
            }

            Move best = null;
            int bestScore = Integer.MIN_VALUE;
            for (int k = 0; k < 4 && k < moves.size(); ++k) {
                Move m = moves.get(random.nextInt(moves.size()));

                Board tmp = board.copy();
                applyMove(tmp, m);

                int score = evaluate(tmp, rootPlayer, size, w1, w2);

                if (score > bestScore) {
                    bestScore = score;
                    best = m;
                }
            }

            applyMove(board, best);

            if (best.getMoveType() == MoveType.PLACE_WALL) {
                if (currentPlayer == 1) w1--;
                else w2--;
            }

            currentPlayer = 3 - currentPlayer;
        }

        return evaluate(board, rootPlayer, size, w1, w2) > 0 ? rootPlayer : (3 - rootPlayer);
    }

    private void backpropagate(Node node, int winner, int playerId) {
        while (node != null) {
            node.visits++;
            if (winner == playerId) node.wins++;
            node = node.parent;
        }
    }

    private double uct(Node n) {
        if (n.visits == 0) {
            return Double.MAX_VALUE;
        }

        double exploitation = n.wins / n.visits;
        double exploration = C * Math.sqrt(Math.log(n.parent.visits) / n.visits);
        double heuristic = evaluate(n.board, n.parent.player,
                (int) Math.sqrt(n.board.getTiles().size()),
                n.walls1, n.walls2) * 0.01;

        return exploitation + exploration + heuristic;
    }

    private double bestWinRate(Node root) {
        return root.children.stream().mapToDouble(n -> n.visits == 0 ? 0 : n.wins / n.visits).max().orElse(0);
    }

    private boolean isWin(Board board, int playerId, int size) {
        String pos = getCurrentPosition(board, playerId);
        int row = pos.charAt(0) - '0';
        return (playerId == 1 && row == 0) || (playerId == 2 && row == size - 1);
    }

    static class Node {
        Board board;
        Node parent;
        List<Node> children = new ArrayList<>();
        Move move;

        int visits = 0;
        double wins = 0;

        int player;
        int walls1, walls2;

        Node(Board board, Node parent, Move move,
             int player, int w1, int w2) {
            this.board = board;
            this.parent = parent;
            this.move = move;
            this.player = player;
            this.walls1 = w1;
            this.walls2 = w2;
        }

        int getWalls() {
            return player == 1 ? walls1 : walls2;
        }
    }
}