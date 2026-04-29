package ru.ac.uniyar.model.algorithms;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

public final class EvaluationWeightsStore {
    private static final Path FILE = Path.of("data", "evaluation-weights.properties");
    private static EvaluationWeights current;

    private EvaluationWeightsStore() {
    }

    public static synchronized EvaluationWeights current() {
        if (current == null) {
            current = load();
        }
        return current;
    }

    public static synchronized void learnFromGame(List<EvaluationLearningSample> samples, int winnerId) {
        if (samples.isEmpty() || winnerId == 0) {
            return;
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
            return;
        }

        current = current().adjustedByGameGradient(gradient, 1, samples.size());
        save(current);
    }

    private static EvaluationWeights load() {
        if (!Files.exists(FILE)) {
            EvaluationWeights defaults = EvaluationWeights.defaults();
            save(defaults);
            return defaults;
        }

        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(FILE)) {
            properties.load(input);
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
            Files.writeString(FILE, serialize(weights));
        } catch (IOException ignored) {
            // Если файл недоступен, алгоритмы продолжают работать с весами в памяти.
        }
    }

    private static String serialize(EvaluationWeights weights) {
        return """
                # Learned evaluation weights for Quoridor AI.
                # Файл можно редактировать вручную; при следующем сохранении комментарии будут сохранены.
                #
                # pathAdvantage - вес разницы кратчайших путей: путь соперника минус путь ИИ.
                # Чем больше значение, тем сильнее ИИ стремится сокращать свой путь и удлинять путь соперника.
                pathAdvantage=%d
                
                # myMobility - вес количества доступных ходов фишкой для ИИ.
                # Увеличивает ценность позиций, где у ИИ больше вариантов движения.
                myMobility=%d
                
                # enemyMobility - штраф за количество доступных ходов фишкой у соперника.
                # Чем больше значение, тем сильнее ИИ старается ограничивать мобильность противника.
                enemyMobility=%d
                
                # wallAdvantage - вес преимущества по оставшимся стенам: стены ИИ минус стены соперника.
                # Помогает не тратить стены без необходимости и ценить запас стен в эндшпиле.
                wallAdvantage=%d
                
                # progressAdvantage - вес продвижения к целевой линии относительно соперника.
                # Нужен, чтобы алгоритм не зацикливался на равных по длине пути позициях.
                progressAdvantage=%d
                
                # myEndgame - бонус за близость ИИ к победной линии.
                # Работает особенно сильно, когда до цели осталось 1-3 хода.
                myEndgame=%d
                
                # enemyEndgame - штраф за близость соперника к победной линии.
                # Чем больше значение, тем охотнее ИИ срочно блокирует финиш соперника.
                enemyEndgame=%d
                
                # samples - сколько ходов ИИ участвовало в принятых обновлениях после завершения партий.
                # Веса меняются не после отдельного хода, а после партии, на основании победителя.
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
