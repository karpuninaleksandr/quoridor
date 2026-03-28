package ru.ac.uniyar.model.players;

import ru.ac.uniyar.model.Board;
import ru.ac.uniyar.model.Move;

//реализация интерфейса Player для пользователя - методы будут ожидать его вводимых действий
public class HumanPlayer extends Player {
    @Override
    public Move getMove(Board board, int playerId) {
        //запрос хода от пользователя происходит с UI
        return null;
    }
}
