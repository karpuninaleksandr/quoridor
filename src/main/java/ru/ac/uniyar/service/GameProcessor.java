package ru.ac.uniyar.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.ac.uniyar.model.Game;
import ru.ac.uniyar.model.enums.ComputerAlgorithmType;
import ru.ac.uniyar.model.enums.ComputerPlayerHardnessLevel;
import ru.ac.uniyar.model.enums.GameSize;
import ru.ac.uniyar.model.players.ComputerPlayerFabric;
import ru.ac.uniyar.model.players.HumanPlayer;
import ru.ac.uniyar.model.players.Player;

import java.util.Arrays;
import java.util.List;

@Service
public class GameProcessor {
    @Autowired
    private ComputerPlayerFabric computerPlayerFabric;
    private Game game;

    public List<String> getAvailableBoardSizes() {
        return Arrays.stream(GameSize.values()).map(GameSize::getDescription).toList();
    }

    public List<String> getAvailablePlayerTypes() {
        return Arrays.stream(ComputerAlgorithmType.values()).map(ComputerAlgorithmType::getDescription).toList();
    }

    public List<String> getAvailableHardnessLevels() {
        return Arrays.stream(ComputerPlayerHardnessLevel.values()).map(ComputerPlayerHardnessLevel::getDescription).toList();
    }

    public void startNewGame(String sizeDescription, String typeOfPlayer1, String typeOfPlayer2, String gameHardness) {
        GameSize gameSize = GameSize.findByDescription(sizeDescription);
        Player player1 = typeOfPlayer1.equals("Игрок") ? new HumanPlayer() : computerPlayerFabric.getComputerPlayer(typeOfPlayer1, gameHardness);
        Player player2 = computerPlayerFabric.getComputerPlayer(typeOfPlayer2, gameHardness);

        //todo init board and game

        System.out.println(gameSize);
    }
}