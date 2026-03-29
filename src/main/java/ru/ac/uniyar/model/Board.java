package ru.ac.uniyar.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.*;

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

            BoardTile tl = tiles.get(i1 + "" + j);
            BoardTile tr = tiles.get(i1 + "" + (j + 1));
            BoardTile bl = tiles.get((i1 + 1) + "" + j);
            BoardTile br = tiles.get((i1 + 1) + "" + (j + 1));

            if (!tl.isBackwardsMovementAvailable() || !tr.isBackwardsMovementAvailable()) {
                throw new RuntimeException("Wall overlap");
            }

            if (!tl.isRightMovementAvailable() && !bl.isRightMovementAvailable()) {
                throw new RuntimeException("Wall crossing");
            }

            tl.setBackwardsMovementAvailable(false);
            bl.setForwardMovementAvailable(false);

            tr.setBackwardsMovementAvailable(false);
            br.setForwardMovementAvailable(false);
            return;
        }

        if (j1 == j2 && Math.abs(i1 - i2) == 1) {
            int i = Math.min(i1, i2);

            BoardTile tl = tiles.get(i + "" + j1);
            BoardTile bl = tiles.get((i + 1) + "" + j1);
            BoardTile tr = tiles.get(i + "" + (j1 + 1));
            BoardTile br = tiles.get((i + 1) + "" + (j1 + 1));

            if (!tl.isRightMovementAvailable() || !bl.isRightMovementAvailable()) {
                throw new RuntimeException("Wall overlap");
            }

            if (!tl.isBackwardsMovementAvailable() && !tr.isBackwardsMovementAvailable()) {
                throw new RuntimeException("Wall crossing");
            }

            tl.setRightMovementAvailable(false);
            tr.setLeftMovementAvailable(false);

            bl.setRightMovementAvailable(false);
            br.setLeftMovementAvailable(false);
        }
    }

    public List<String> getAvailableMoves(String pos) {
        List<String> result = new ArrayList<>();

        int i = pos.charAt(0) - '0';
        int j = pos.charAt(1) - '0';

        BoardTile tile = tiles.get(pos);
        if (tile == null) return result;

        if (tile.isLeftMovementAvailable()) {
            String next = i + "" + (j - 1);
            if (tiles.containsKey(next)) result.add(next);
        }

        if (tile.isRightMovementAvailable()) {
            String next = i + "" + (j + 1);
            if (tiles.containsKey(next)) result.add(next);
        }

        if (tile.isForwardMovementAvailable()) {
            String next = (i - 1) + "" + j;
            if (tiles.containsKey(next)) result.add(next);
        }

        if (tile.isBackwardsMovementAvailable()) {
            String next = (i + 1) + "" + j;
            if (tiles.containsKey(next)) result.add(next);
        }

        return result;
    }

    public Board copy() {
        Board copy = new Board();

        Map<String, BoardTile> newTiles = new HashMap<>();

        for (Map.Entry<String, BoardTile> entry : tiles.entrySet()) {
            BoardTile t = entry.getValue();

            newTiles.put(entry.getKey(), new BoardTile(
                    t.isLeftMovementAvailable(),
                    t.isForwardMovementAvailable(),
                    t.isRightMovementAvailable(),
                    t.isBackwardsMovementAvailable()
            ));
        }

        copy.setTiles(newTiles);
        copy.setPositionOfPlayer1(positionOfPlayer1);
        copy.setPositionOfPlayer2(positionOfPlayer2);

        return copy;
    }
}