package ru.ac.uniyar.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import ru.ac.uniyar.model.enums.MoveType;

//класс, описывающий ход игрока
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class Move {
    private MoveType moveType;
    private int playerId;
    private Position startPosition;
    private Position endPosition;

    public static Move movePlayer(int playerId, Position endPosition) {
        return new Move(
                MoveType.MOVE_PLAYER,
                playerId,
                null,
                endPosition
        );
    }

    public static Move placeWall(int playerId, Position startPosition, Position endPosition) {
        return new Move(
                MoveType.PLACE_WALL,
                playerId,
                startPosition,
                endPosition
        );
    }
}
