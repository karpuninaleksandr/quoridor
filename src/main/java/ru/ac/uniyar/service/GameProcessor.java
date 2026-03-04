package ru.ac.uniyar.service;

import org.springframework.stereotype.Service;
import ru.ac.uniyar.model.enums.GameSize;

import java.util.Arrays;
import java.util.List;

@Service
public class GameProcessor {
    public List<String> getAvailableBoardSizes() {
        return Arrays.stream(GameSize.values()).map(GameSize::getDescription).toList();
    }

    public void startNewGame(String sizeDescription) {
        GameSize gameSize = GameSize.findByDescription(sizeDescription);
        System.out.println(gameSize);
    }
}