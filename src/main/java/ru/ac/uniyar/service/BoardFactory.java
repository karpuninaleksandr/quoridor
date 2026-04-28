package ru.ac.uniyar.service;

import org.springframework.stereotype.Service;
import ru.ac.uniyar.model.Board;
import ru.ac.uniyar.model.BoardTile;
import ru.ac.uniyar.model.Position;
import ru.ac.uniyar.model.enums.GameSize;

@Service
public class BoardFactory {
    public Board create(GameSize gameSize) {
        Board board = new Board();
        int size = gameSize.getAmountOfTilesPerSide();

        for (int i = 0; i < size; ++i) {
            for (int j = 0; j < size; ++j) {
                board.getTiles().put(new Position(i, j), new BoardTile(
                        j != 0,
                        i != 0,
                        j != size - 1,
                        i != size - 1
                ));
            }
        }

        int mid = (size - 1) / 2;
        board.setPositionOfPlayer1(new Position(size - 1, mid));
        board.setPositionOfPlayer2(new Position(0, mid));

        return board;
    }
}
