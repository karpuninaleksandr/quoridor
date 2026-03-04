package ru.ac.uniyar.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

//класс описывающий клетку игрового поля Board
@Getter
@Setter
@AllArgsConstructor
public class BoardTile {
    private String tileCode;
    private boolean leftMovementAvailable;
    private boolean forwardMovementAvailable;
    private boolean rightMovementAvailable;
    private boolean backwardsMovementAvailable;
}
