package ru.ac.uniyar.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

//класс описывающий состояние игрового поля
@Getter
@Setter
@NoArgsConstructor
public class Board {
    private Map<String, BoardTile> tiles = new HashMap<>();
    private String positionOfPlayer1;
    private String positionOfPlayer2;

    public void placeWall(String startPosition, String endPosition) {
        int i1 = startPosition.charAt(0) - '0';
        int j1 = startPosition.charAt(1) - '0';

        int i2 = endPosition.charAt(0) - '0';
        int j2 = endPosition.charAt(1) - '0';

        if (i1 == i2 && Math.abs(j1 - j2) == 1) {
            int j = Math.min(j1, j2);

            BoardTile topLeft = tiles.get(i1 + "" + j);
            BoardTile topRight = tiles.get(i1 + "" + (j + 1));
            BoardTile bottomLeft = tiles.get((i1 + 1) + "" + j);
            BoardTile bottomRight = tiles.get((i1 + 1) + "" + (j + 1));

            topLeft.setBackwardsMovementAvailable(false);
            bottomLeft.setForwardMovementAvailable(false);

            topRight.setBackwardsMovementAvailable(false);
            bottomRight.setForwardMovementAvailable(false);

            return;
        }

        if (j1 == j2 && Math.abs(i1 - i2) == 1) {
            int i = Math.min(i1, i2);

            BoardTile topLeft = tiles.get(i + "" + j1);
            BoardTile bottomLeft = tiles.get((i + 1) + "" + j1);
            BoardTile topRight = tiles.get(i + "" + (j1 + 1));
            BoardTile bottomRight = tiles.get((i + 1) + "" + (j1 + 1));

            topLeft.setRightMovementAvailable(false);
            topRight.setLeftMovementAvailable(false);

            bottomLeft.setRightMovementAvailable(false);
            bottomRight.setLeftMovementAvailable(false);
        }
    }
}
