package ru.ac.uniyar.model.algorithms;

import ru.ac.uniyar.model.Board;
import ru.ac.uniyar.model.Move;
import ru.ac.uniyar.model.enums.ComputerAlgorithmType;
import ru.ac.uniyar.model.enums.ComputerPlayerHardnessLevel;
import ru.ac.uniyar.model.enums.GameSize;
import ru.ac.uniyar.model.enums.MoveType;

import java.util.*;

public class MonteCarloAlgorithm implements Algorithm {
    private static final double C = 1.4;
    private static final Random random = new Random();

    @Override
    public ComputerAlgorithmType getType() {
        return ComputerAlgorithmType.MONTECARLO;
    }

    @Override
    public Move getMove(Board board, ComputerPlayerHardnessLevel hardnessLevel, int playerId, int wallsLeft) {
        int size = (int) Math.sqrt(board.getTiles().size());

        long timeLimit = switch (hardnessLevel) {
            case EASY -> 1000L * (size / GameSize.NORMAL.getAmountOfTilesPerSide());
            case MEDIUM -> 2000L * (size / GameSize.NORMAL.getAmountOfTilesPerSide());
            case HARD -> 3000L * (size / GameSize.NORMAL.getAmountOfTilesPerSide());
        };

        int maxSteps = switch (hardnessLevel) {
            case EASY -> 1000 * (size / GameSize.NORMAL.getAmountOfTilesPerSide());
            case MEDIUM -> 2000 * (size / GameSize.NORMAL.getAmountOfTilesPerSide());
            case HARD -> 3000 * (size / GameSize.NORMAL.getAmountOfTilesPerSide());
        };

        Node root = new Node(board.copy(), null, null, playerId, wallsLeft, wallsLeft);

        long endTime = System.currentTimeMillis() + timeLimit;

        while (System.currentTimeMillis() < endTime) {
            Node node = select(root);
            Node expanded = expand(node);
            int winner = simulate(expanded, maxSteps);
            backpropagate(expanded, winner, playerId);
        }

        return root.children.stream()
                .max(Comparator.comparing(n -> n.visits))
                .get()
                .move;
    }

    private Node select(Node node) {
        while (!node.children.isEmpty()) {
            node = node.children.stream()
                    .max(Comparator.comparing(this::uct))
                    .get();
        }
        return node;
    }

    private Node expand(Node node) {
        List<Move> moves = getMoves(node.board, node.player, node.getWalls());

        if (moves.isEmpty()) return node;

        Move move = moves.get(random.nextInt(moves.size()));

        Board copy = node.board.copy();
        applyMove(copy, move);

        int w1 = node.walls1;
        int w2 = node.walls2;

        if (move.getMoveType() == MoveType.PLACE_WALL) {
            if (node.player == 1) w1--;
            else w2--;
        }

        Node child = new Node(copy, node, move, 3 - node.player, w1, w2);
        node.children.add(child);

        return child;
    }

    private int simulate(Node node, int maxSteps) {
        Board board = node.board.copy();
        int currentPlayer = node.player;
        int w1 = node.walls1;
        int w2 = node.walls2;

        int size = (int) Math.sqrt(board.getTiles().size());

        for (int i = 0; i < maxSteps; i++) {
            if (isWin(board, 1, size)) return 1;
            if (isWin(board, 2, size)) return 2;

            int walls = currentPlayer == 1 ? w1 : w2;
            List<Move> moves = getMoves(board, currentPlayer, walls);

            if (moves.isEmpty()) return 3 - currentPlayer;

            Move move = moves.get(random.nextInt(moves.size()));
            applyMove(board, move);

            if (move.getMoveType() == MoveType.PLACE_WALL) {
                if (currentPlayer == 1) w1--;
                else w2--;
            }

            currentPlayer = 3 - currentPlayer;
        }

        return evaluate(board, 1, size, w1, w2) > 0 ? 1 : 2;
    }

    private void backpropagate(Node node, int winner, int playerId) {
        while (node != null) {
            node.visits++;
            if (winner == playerId) node.wins++;
            node = node.parent;
        }
    }

    private double uct(Node n) {
        if (n.visits == 0) return Double.MAX_VALUE;
        return (n.wins / n.visits) + C * Math.sqrt(Math.log(n.parent.visits) / n.visits);
    }

    private boolean isWin(Board board, int playerId, int size) {
        String pos = getCurrentPosition(board, playerId);
        int row = pos.charAt(0) - '0';
        return (playerId == 1 && row == size - 1) ||
                (playerId == 2 && row == 0);
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