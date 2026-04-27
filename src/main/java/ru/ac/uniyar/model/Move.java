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
    private String startPosition;
    private String endPosition;

    public static Move movePlayer(int playerId, String endPosition) {
        return new Move(
                MoveType.MOVE_PLAYER,
                playerId,
                null,
                endPosition
        );
    }

    public static Move placeWall(int playerId, String startPosition, String endPosition) {
        return new Move(
                MoveType.PLACE_WALL,
                playerId,
                startPosition,
                endPosition
        );
    }
}
