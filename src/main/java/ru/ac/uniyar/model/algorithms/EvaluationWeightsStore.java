package ru.ac.uniyar.model.algorithms;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;

public final class EvaluationWeightsStore {
    private static final Path FILE = Path.of("data", "evaluation-weights.properties");
    private static final Path HISTORY_FILE = Path.of("data", "evaluation-weights-history.csv");
    private static EvaluationWeights current;

    private EvaluationWeightsStore() {}

    public static synchronized EvaluationWeights current() {
        if (current == null) {
            current = load();
        }
        return current;
    }

    public static synchronized EvaluationTrainingUpdate learnFromGame(List<EvaluationLearningSample> samples, int winnerId, String source) {
        EvaluationWeights beforeWeights = current();
        if (samples.isEmpty() || winnerId == 0) {
            return new EvaluationTrainingUpdate(false, winnerId, samples.size(), new int[7], beforeWeights, beforeWeights);
        }

        int[] gradient = new int[7];
        for (EvaluationLearningSample sample : samples) {
            int reward = sample.playerId() == winnerId ? 1 : -1;
            EvaluationFeatures delta = sample.delta();
            gradient[0] += reward * delta.pathAdvantage();
            gradient[1] += reward * delta.myMobility();
            gradient[2] += reward * delta.enemyMobilityPenalty();
            gradient[3] += reward * delta.wallAdvantage();
            gradient[4] += reward * delta.progressAdvantage();
            gradient[5] += reward * delta.myEndgame();
            gradient[6] += reward * delta.enemyEndgamePenalty();
        }

        if (isZero(gradient)) {
            return new EvaluationTrainingUpdate(false, winnerId, samples.size(), gradient, beforeWeights, beforeWeights);
        }

        current = beforeWeights.adjustedByGameGradient(gradient, 1, samples.size());
        save(current);
        appendHistory(source, winnerId, samples.size(), gradient, beforeWeights, current);
        return new EvaluationTrainingUpdate(true, winnerId, samples.size(), gradient, beforeWeights, current);
    }

    private static EvaluationWeights load() {
        if (!Files.exists(FILE)) {
            EvaluationWeights defaults = EvaluationWeights.defaults();
            save(defaults);
            return defaults;
        }

        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
            properties.load(reader);
            return new EvaluationWeights(
                    readInt(properties, "pathAdvantage", EvaluationWeights.defaults().pathAdvantage()),
                    readInt(properties, "myMobility", EvaluationWeights.defaults().myMobility()),
                    readInt(properties, "enemyMobility", EvaluationWeights.defaults().enemyMobility()),
                    readInt(properties, "wallAdvantage", EvaluationWeights.defaults().wallAdvantage()),
                    readInt(properties, "progressAdvantage", EvaluationWeights.defaults().progressAdvantage()),
                    readInt(properties, "myEndgame", EvaluationWeights.defaults().myEndgame()),
                    readInt(properties, "enemyEndgame", EvaluationWeights.defaults().enemyEndgame()),
                    readLong(properties, "samples", 0)
            );
        } catch (IOException | IllegalArgumentException e) {
            EvaluationWeights defaults = EvaluationWeights.defaults();
            save(defaults);
            return defaults;
        }
    }

    private static void save(EvaluationWeights weights) {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, serialize(weights), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // Если файл недоступен, алгоритмы продолжают работать с весами в памяти.
        }
    }

    private static String serialize(EvaluationWeights weights) {
        return """
                pathAdvantage=%d
                myMobility=%d
                enemyMobility=%d
                wallAdvantage=%d
                progressAdvantage=%d
                myEndgame=%d
                enemyEndgame=%d
                samples=%d
                """.formatted(
                weights.pathAdvantage(),
                weights.myMobility(),
                weights.enemyMobility(),
                weights.wallAdvantage(),
                weights.progressAdvantage(),
                weights.myEndgame(),
                weights.enemyEndgame(),
                weights.samples()
        );
    }

    private static void appendHistory(String source, int winnerId, int sampleCount, int[] gradient, EvaluationWeights before, EvaluationWeights after) {
        try {
            Files.createDirectories(HISTORY_FILE.getParent());
            if (!Files.exists(HISTORY_FILE)) {
                Files.writeString(HISTORY_FILE, historyHeader(), StandardCharsets.UTF_8);
            }
            Files.writeString(HISTORY_FILE, historyRow(source, winnerId, sampleCount, gradient, before, after),
                    StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
            // История полезна для диплома, но недоступность файла не должна ломать обучение.
        }
    }

    private static String historyHeader() {
        return "timestamp,source,winnerId,sampleCount,"
                + "gradientPath,gradientMyMobility,gradientEnemyMobility,gradientWall,gradientProgress,gradientMyEndgame,gradientEnemyEndgame,"
                + "beforePath,beforeMyMobility,beforeEnemyMobility,beforeWall,beforeProgress,beforeMyEndgame,beforeEnemyEndgame,beforeSamples,"
                + "afterPath,afterMyMobility,afterEnemyMobility,afterWall,afterProgress,afterMyEndgame,afterEnemyEndgame,afterSamples\n";
    }

    private static String historyRow(String source, int winnerId, int sampleCount, int[] gradient, EvaluationWeights before, EvaluationWeights after) {
        return String.join(",",
                LocalDateTime.now().toString(),
                escape(source),
                Integer.toString(winnerId),
                Integer.toString(sampleCount),
                Integer.toString(gradient[0]),
                Integer.toString(gradient[1]),
                Integer.toString(gradient[2]),
                Integer.toString(gradient[3]),
                Integer.toString(gradient[4]),
                Integer.toString(gradient[5]),
                Integer.toString(gradient[6]),
                Integer.toString(before.pathAdvantage()),
                Integer.toString(before.myMobility()),
                Integer.toString(before.enemyMobility()),
                Integer.toString(before.wallAdvantage()),
                Integer.toString(before.progressAdvantage()),
                Integer.toString(before.myEndgame()),
                Integer.toString(before.enemyEndgame()),
                Long.toString(before.samples()),
                Integer.toString(after.pathAdvantage()),
                Integer.toString(after.myMobility()),
                Integer.toString(after.enemyMobility()),
                Integer.toString(after.wallAdvantage()),
                Integer.toString(after.progressAdvantage()),
                Integer.toString(after.myEndgame()),
                Integer.toString(after.enemyEndgame()),
                Long.toString(after.samples())
        ) + "\n";
    }

    private static String escape(String value) {
        String safe = value == null ? "" : value.replace("\"", "\"\"");
        return "\"" + safe + "\"";
    }

    private static int readInt(Properties properties, String key, int fallback) {
        return Integer.parseInt(properties.getProperty(key, Integer.toString(fallback)));
    }

    private static long readLong(Properties properties, String key, long fallback) {
        return Long.parseLong(properties.getProperty(key, Long.toString(fallback)));
    }

    private static boolean isZero(int[] values) {
        for (int value : values) {
            if (value != 0) {
                return false;
            }
        }
        return true;
    }
}
