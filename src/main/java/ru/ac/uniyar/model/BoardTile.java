package ru.ac.uniyar.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

//класс описывающий клетку игрового поля Board
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BoardTile {
    private boolean leftMovementAvailable;
    private boolean forwardMovementAvailable;
    private boolean rightMovementAvailable;
    private boolean backwardsMovementAvailable;
}
