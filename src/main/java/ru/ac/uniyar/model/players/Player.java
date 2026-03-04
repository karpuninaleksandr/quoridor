package ru.ac.uniyar.model.players;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import ru.ac.uniyar.model.Board;
import ru.ac.uniyar.model.Move;

//базовый класс Player с описанием возможных действий игрока
@AllArgsConstructor
@NoArgsConstructor
public abstract class Player {
    private int amountOfWallsLeft;

    public boolean canPlaceWall() {
        return amountOfWallsLeft > 0;
    }

    public abstract Move getMove(Board board);
}
