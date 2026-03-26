package ru.ac.uniyar.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.ac.uniyar.model.enums.MoveType;

//класс, описывающий ход игрока
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Move {
    private MoveType moveType;
    private int playerId;
    private String startPosition;
    private String endPosition;
}
