package ru.ac.uniyar.model.enums;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum GameSize {
    SMALL(7, 8, "Малое поле 7 на 7"),
    NORMAL(9, 10, "Среднее поле 9 на 9"),
    LARGE(11, 12, "Большое поле 11 на 11");

    private final int amountOfTilesPerSide;
    private final int amountOfWalls;
    private final String description;

    GameSize(int amountOfTilesPerSide, int amountOfWalls, String description) {
        this.amountOfWalls = amountOfWalls;
        this.amountOfTilesPerSide = amountOfTilesPerSide;
        this.description = description;
    }

    public static GameSize findByDescription(String description) {
        return Arrays.stream(GameSize.values()).filter(it -> it.description.equals(description)).findFirst().get();
    }
}
