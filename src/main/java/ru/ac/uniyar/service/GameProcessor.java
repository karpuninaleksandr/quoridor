package ru.ac.uniyar.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.ac.uniyar.model.Game;
import ru.ac.uniyar.model.enums.GameSize;
import ru.ac.uniyar.model.players.ComputerPlayerFabric;
import ru.ac.uniyar.model.players.HumanPlayer;
import ru.ac.uniyar.model.players.Player;

@Service
public class GameProcessor {
    @Autowired
    private ComputerPlayerFabric computerPlayerFabric;
    private Game game;

    public void startNewGame(String sizeDescription, String typeOfPlayer1, String typeOfPlayer2, String gameHardness) {
        GameSize gameSize = GameSize.findByDescription(sizeDescription);
        Player player1 = typeOfPlayer1.equals("Игрок") ? new HumanPlayer() : computerPlayerFabric.getComputerPlayer(typeOfPlayer1, gameHardness);
        Player player2 = computerPlayerFabric.getComputerPlayer(typeOfPlayer2, gameHardness);

        //todo init board and game

        System.out.println(gameSize);
    }
}