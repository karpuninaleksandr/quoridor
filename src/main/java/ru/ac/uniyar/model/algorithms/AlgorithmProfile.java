package ru.ac.uniyar.model.algorithms;

public final class AlgorithmProfile {
    private AlgorithmProfile() {
    }

    public static String describe(String algorithm) {
        if (algorithm == null) {
            return "Выберите алгоритм, чтобы увидеть краткое описание его стратегии.";
        }
        if (algorithm.contains("Случайный")) {
            return "Случайный: базовый алгоритм для сравнения. На высокой сложности выбирает случайно из лучших ходов по оценке.";
        }
        if (algorithm.contains("MiniMax")) {
            return "MiniMax: перебирает дерево ходов до заданной глубины без alpha-beta отсечений.";
        }
        if (algorithm.contains("AlphaBeta")) {
            return "AlphaBeta: MiniMax с отсечениями, сортировкой ходов и transposition table.";
        }
        if (algorithm.contains("MonteCarlo")) {
            return "MonteCarlo: строит статистику по симуляциям и выбирает ход по UCT/rollout оценке.";
        }
        return "Алгоритм ИИ для выбора хода в текущей позиции.";
    }
}
