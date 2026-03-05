package ru.ac.uniyar.model.enums;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum ComputerPlayerHardnessLevel {
    EASY("Легкий"),
    MEDIUM("Средний"),
    HARD("Сложный");

    private final String description;

    ComputerPlayerHardnessLevel(String description) {
        this.description = description;
    }

    public static ComputerPlayerHardnessLevel findByDescription(String description) {
        return Arrays.stream(ComputerPlayerHardnessLevel.values()).filter(it -> it.description.equals(description)).findFirst().get();
    }
}
