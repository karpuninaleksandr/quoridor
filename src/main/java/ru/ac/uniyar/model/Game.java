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
    private boolean finished;

    public void applyMove(Move move) {
        switch (move.getMoveType()) {
            case PLACE_WALL -> {
                board.placeWall(move.getStartPosition(), move.getEndPosition());
                if (move.getPlayerId() == 1) {
                    player1.setAmountOfWallsLeft(player1.getAmountOfWallsLeft() - 1);
                } else {
                    player2.setAmountOfWallsLeft(player2.getAmountOfWallsLeft() - 1);
                }
            }
            case MOVE_PLAYER -> {
                if (move.getPlayerId() == 1) {
                    board.setPositionOfPlayer1(move.getEndPosition());
                } else {
                    board.setPositionOfPlayer2(move.getEndPosition());
                }
            }
        }
        ++amountOfMoves;
        //todo check if game is finished
    }
}
