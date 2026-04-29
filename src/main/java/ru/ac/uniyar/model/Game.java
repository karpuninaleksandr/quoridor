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
    private int currentPlayer;
    private Instant gameTimeStart;
    private int amountOfMoves;
    private boolean finished;

    public void applyMove(Move move) {
        switch (move.getMoveType()) {
            case PLACE_WALL -> {
                Player player = move.getPlayerId() == 1 ? player1 : player2;
                if (!player.canPlaceWall()) {
                    throw new IllegalStateException("Player has no walls left");
                }
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
        checkIfFinished();
    }

    private void checkIfFinished() {
        int player1Row = board.getPositionOfPlayer1().row();
        int player2Row = board.getPositionOfPlayer2().row();

        if (player1Row == 0 || player2Row == gameSize.getAmountOfTilesPerSide() - 1) {
            finished = true;
        }
    }

    public boolean isWonBy(int playerId) {
        int targetRow = playerId == 1 ? 0 : gameSize.getAmountOfTilesPerSide() - 1;
        Position position = playerId == 1 ? board.getPositionOfPlayer1() : board.getPositionOfPlayer2();
        return position.row() == targetRow;
    }
}
