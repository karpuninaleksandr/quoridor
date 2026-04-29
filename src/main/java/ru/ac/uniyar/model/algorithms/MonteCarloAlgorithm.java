package ru.ac.uniyar.model.algorithms;

import ru.ac.uniyar.model.Board;
import ru.ac.uniyar.model.Move;
import ru.ac.uniyar.model.Position;
import ru.ac.uniyar.model.enums.*;

import java.util.*;

public class MonteCarloAlgorithm implements Algorithm {
    /**
     * коэффициент баланса exploration/exploitation (UCT)
     * больше -> больше случайности, меньше -> больше жадности
     */
    private static final double C = 1.2;
    private static final Random random = new Random();
    private AlgorithmReport lastReport;
    private List<Position> recentPositions = List.of();
    private int rootPlayerId;

    @Override
    public ComputerAlgorithmType getType() {
        return ComputerAlgorithmType.MONTECARLO;
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
     * идем по узлам дерева:
     * добавляем детей узлу
     * играем случайную партию
     * обновляем статистику входов в узел и побед из него
     */
    @Override
    public Move getMove(Board board, ComputerPlayerHardnessLevel hardnessLevel, int playerId, int wallsLeft1, int wallsLeft2) {
        long startedAt = System.currentTimeMillis();
        int size = (int) Math.sqrt(board.getTiles().size());
        long timeLimit = getTimeLimit(hardnessLevel, size);
        long endTime = System.currentTimeMillis() + timeLimit;
        int rolloutDepth = getRolloutDepth(hardnessLevel, size);
        long iterationBudget = getIterationBudget(hardnessLevel, size);

        long iterations = 0;
        rootPlayerId = playerId;
        int currentWalls = playerId == 1 ? wallsLeft1 : wallsLeft2;
        Move tacticalMove = findEndgameMove(board, playerId, currentWalls);
        if (tacticalMove != null) {
            lastReport = new AlgorithmReport(
                    getType().getDescription(),
                    tacticalMove,
                    scoreMove(board, tacticalMove, playerId, wallsLeft1, wallsLeft2),
                    0,
                    0,
                    1,
                    0,
                    0,
                    System.currentTimeMillis() - startedAt,
                    "MCTS применил эндшпильное правило до запуска симуляций"
            );
            return tacticalMove;
        }

        Node root = new Node(board.copy(), null, null, playerId, wallsLeft1, wallsLeft2);
        while (System.currentTimeMillis() < endTime && iterations < iterationBudget) {
            Node node = select(root);
            Node expanded = expand(node);
            int winner = simulate(expanded, rolloutDepth, playerId, size);
            backpropagate(expanded, winner, playerId);
            iterations++;
            if (root.visits > 300 && bestWinRate(root) > 0.97) {
                break;
            }
        }

        Node best = root.children.stream()
                .max(Comparator.comparingDouble(this::robustChildScore))
                .orElseThrow();
        lastReport = new AlgorithmReport(
                getType().getDescription(),
                best.move,
                evaluate(best.board, playerId, size, best.walls1, best.walls2),
                rolloutDepth,
                iterations,
                root.children.size(),
                0,
                0,
                System.currentTimeMillis() - startedAt,
                "MCTS: выбор по UCT, rollout с epsilon-greedy эвристикой, немедленными победами и тактическими стенами"
                        + "; лимит времени: " + timeLimit + " мс"
                        + ", бюджет итераций: " + iterationBudget
        );
        return best.move;
    }

    private int getRolloutDepth(ComputerPlayerHardnessLevel level, int size) {
        return switch (level) {
            case EASY -> size * 3;
            case MEDIUM -> size * 5;
            case HARD -> size * 8;
        };
    }

    private long getIterationBudget(ComputerPlayerHardnessLevel level, int size) {
        boolean largeBoard = size > GameSize.NORMAL.getAmountOfTilesPerSide();
        return switch (level) {
            case EASY -> largeBoard ? 250L : 350L;
            case MEDIUM -> largeBoard ? 800L : 1100L;
            case HARD -> largeBoard ? 1700L : 2400L;
        };
    }

    @Override
    public long getTimeLimit(ComputerPlayerHardnessLevel level, int size) {
        boolean largeBoard = size > GameSize.NORMAL.getAmountOfTilesPerSide();
        return switch (level) {
            case EASY -> largeBoard ? 800L : 650L;
            case MEDIUM -> largeBoard ? 2500L : 1900L;
            case HARD -> largeBoard ? 5200L : 4200L;
        };
    }

    /**
     * идем вниз дерева по пути наибольшего UCT
     */
    private Node select(Node node) {
        while (!node.children.isEmpty() && node.untriedMoves.isEmpty()) {
            node = node.children.stream().max(Comparator.comparing(this::uct)).orElseThrow();
        }
        return node;
    }

    /**
     * создаем всевозможные ходы из текущего узла дерева
     */
    private Node expand(Node node) {
        if (node.untriedMoves.isEmpty() && !node.children.isEmpty()) {
            return node;
        }

        if (node.untriedMoves.isEmpty()) {
            node.untriedMoves.addAll(getMoves(node.board, node.player, node.getWalls()));
            node.untriedMoves.sort((a, b) -> Integer.compare(
                    scoreMove(node.board, b, node.player, node.walls1, node.walls2),
                    scoreMove(node.board, a, node.player, node.walls1, node.walls2)
            ));
        }

        if (node.untriedMoves.isEmpty()) {
            return node;
        }

        Move move = node.untriedMoves.remove(0);
        Board copy = node.board.copy();
        applyMove(copy, move);

        int wallsLeft1 = node.walls1;
        int wallsLeft2 = node.walls2;

        if (move.getMoveType() == MoveType.PLACE_WALL) {
            if (node.player == 1) {
                --wallsLeft1;
            } else {
                --wallsLeft2;
            }
        }
        Node child = new Node(copy, node, move, 3 - node.player, wallsLeft1, wallsLeft2);
        node.children.add(child);
        return child;
    }

    /**
     * играем случайную партию из узла дерева
     */
    private int simulate(Node node, int maxSteps, int rootPlayer, int size) {
        Board board = node.board.copy();
        int currentPlayer = node.player;
        int wallsLeft1 = node.walls1;
        int wallsLeft2 = node.walls2;

        for (int step = 0; step < maxSteps; step++) {
            if (isWin(board, 1, size)) {
                return 1;
            }
            if (isWin(board, 2, size)) {
                return 2;
            }

            int walls = currentPlayer == 1 ? wallsLeft1 : wallsLeft2;
            List<Move> moves = getMoves(board, currentPlayer, walls);
            if (moves.isEmpty()) {
                return 3 - currentPlayer;
            }

            Move best = pickRolloutMove(board, moves, currentPlayer, rootPlayer, size, wallsLeft1, wallsLeft2);

            applyMove(board, best);

            if (best.getMoveType() == MoveType.PLACE_WALL) {
                if (currentPlayer == 1) {
                    --wallsLeft1;
                } else {
                    --wallsLeft2;
                }
            }
            currentPlayer = 3 - currentPlayer;
        }

        return evaluate(board, rootPlayer, size, wallsLeft1, wallsLeft2) > 0 ? rootPlayer : (3 - rootPlayer);
    }

    /**
     * поднимаемся вверх по дереву и обновляем количество входов и побед
     */
    private void backpropagate(Node node, int winner, int playerId) {
        while (node != null) {
            ++node.visits;
            if (winner == playerId) {
                ++node.wins;
            }
            node = node.parent;
        }
    }

    /**
     * UCT - оценка узла
     * exploitation — доля побед
     * exploration — исследованность узла
     * heuristic — оценка через evaluate
     */
    private double uct(Node n) {
        if (n.visits == 0) {
            return Double.MAX_VALUE;
        }

        double exploitation = n.wins / n.visits;
        double exploration = C * Math.sqrt(Math.log(n.parent.visits) / n.visits);
        double heuristic = normalizedEvaluate(n.board, n.parent.player, n.walls1, n.walls2);

        return exploitation + exploration + heuristic;
    }

    private Move pickRolloutMove(Board board, List<Move> moves, int currentPlayer, int rootPlayer,
                                 int size, int wallsLeft1, int wallsLeft2) {
        for (Move move : moves) {
            if (move.getMoveType() == MoveType.MOVE_PLAYER
                    && move.getEndPosition().row() == getTargetRow(currentPlayer, size)) {
                return move;
            }
        }

        int walls = currentPlayer == 1 ? wallsLeft1 : wallsLeft2;
        if (walls > 0 && calculateShortestPath(board, getCurrentPosition(board, 3 - currentPlayer),
                getTargetRow(3 - currentPlayer, size)) <= 2) {
            Move tacticalWall = moves.stream()
                    .filter(move -> move.getMoveType() == MoveType.PLACE_WALL)
                    .max(Comparator.comparingInt(move -> wallImpact(board, move, currentPlayer, size)))
                    .orElse(null);
            if (tacticalWall != null && wallImpact(board, tacticalWall, currentPlayer, size) > 0) {
                return tacticalWall;
            }
        }

        if (random.nextDouble() < 0.22) {
            return moves.get(random.nextInt(moves.size()));
        }

        int sampleSize = Math.min(18, moves.size());
        List<Move> sampled = new ArrayList<>(sampleSize);
        Set<Integer> usedIndexes = new HashSet<>();
        while (sampled.size() < sampleSize) {
            int index = random.nextInt(moves.size());
            if (usedIndexes.add(index)) {
                sampled.add(moves.get(index));
            }
        }

        sampled.sort((a, b) -> Integer.compare(
                scoreRolloutMove(board, b, currentPlayer, rootPlayer, size, wallsLeft1, wallsLeft2),
                scoreRolloutMove(board, a, currentPlayer, rootPlayer, size, wallsLeft1, wallsLeft2)
        ));
        int top = Math.max(1, Math.min(4, sampled.size()));
        return sampled.get(random.nextInt(top));
    }

    private int scoreRolloutMove(Board board, Move move, int currentPlayer, int rootPlayer,
                                 int size, int wallsLeft1, int wallsLeft2) {
        Board tmp = board.copy();
        applyMove(tmp, move);
        int newWallsLeft1 = wallsLeft1;
        int newWallsLeft2 = wallsLeft2;
        if (move.getMoveType() == MoveType.PLACE_WALL) {
            if (currentPlayer == 1) {
                --newWallsLeft1;
            } else {
                --newWallsLeft2;
            }
        }

        int score = evaluate(tmp, rootPlayer, size, newWallsLeft1, newWallsLeft2);
        if (currentPlayer != rootPlayer) {
            score = -score;
        }
        if (currentPlayer == rootPlayer) {
            score += movementPreference(move, board, currentPlayer, size, recentPositions);
        }
        return score;
    }

    private int scoreMove(Board board, Move move, int playerId, int wallsLeft1, int wallsLeft2) {
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
        int preference = playerId == rootPlayerId
                ? movementPreference(move, board, playerId, size, recentPositions)
                : 0;
        return evaluate(copy, playerId, size, newWallsLeft1, newWallsLeft2) + preference;
    }

    private double normalizedEvaluate(Board board, int playerId, int wallsLeft1, int wallsLeft2) {
        int size = (int) Math.sqrt(board.getTiles().size());
        return Math.tanh(evaluate(board, playerId, size, wallsLeft1, wallsLeft2) / 500.0) * 0.15;
    }

    private double robustChildScore(Node node) {
        if (node.visits == 0) {
            return 0;
        }
        return node.visits + node.wins / node.visits;
    }

    /**
     * выбор лучшего ребенка через статистику побед
     */
    private double bestWinRate(Node root) {
        return root.children.stream().mapToDouble(n -> n.visits == 0 ? 0 : n.wins / n.visits).max().orElse(0);
    }

    /**
     * проверяем, достиг ли игрок нужной строки поля
     */
    public boolean isWin(Board board, int playerId, int size) {
        int row = getCurrentPosition(board, playerId).row();
        return (playerId == 1 && row == 0) || (playerId == 2 && row == size - 1);
    }

    /**
     * узел дерева
     */
    static class Node {
        Board board;
        Node parent;
        List<Node> children = new ArrayList<>();
        List<Move> untriedMoves = new ArrayList<>();
        Move move;

        int visits = 0;
        double wins = 0;

        int player;
        int walls1, walls2;

        Node(Board board, Node parent, Move move, int player, int w1, int w2) {
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
