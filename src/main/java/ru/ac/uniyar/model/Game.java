package ru.ac.uniyar.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.ac.uniyar.model.enums.GameSize;
import ru.ac.uniyar.model.players.Player;

import java.time.Instant;

//класс, описывающий конфигурацию игры
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Game {
    private GameSize gameSize;
    private Board board;
    private Player player1;
    private Player player2;
    private Instant gameTimeStart;
    private int amountOfMoves;
}
