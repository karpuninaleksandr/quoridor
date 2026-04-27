package ru.ac.uniyar.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.*;

@Getter
@Setter
@NoArgsConstructor
public class Board {
    private Map<Position, BoardTile> tiles = new HashMap<>();
    private Position positionOfPlayer1;
    private Position positionOfPlayer2;

    public void placeWall(Position startPosition, Position endPosition) {
        int i1 = startPosition.row();
        int j1 = startPosition.col();

        int i2 = endPosition.row();
        int j2 = endPosition.col();

        if (i1 == i2 && Math.abs(j1 - j2) == 1) {
            placeHorizontalWall(i1, Math.min(j1, j2));
            return;
        }

        if (j1 == j2 && Math.abs(i1 - i2) == 1) {
            placeVerticalWall(Math.min(i1, i2), j1);
            return;
        }

        throw new RuntimeException("Invalid wall");
    }

    private void placeHorizontalWall(int row, int col) {
        BoardTile tl = getRequiredTile(new Position(row, col));
        BoardTile tr = getRequiredTile(new Position(row, col + 1));
        BoardTile bl = getRequiredTile(new Position(row + 1, col));
        BoardTile br = getRequiredTile(new Position(row + 1, col + 1));

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
    }

    private void placeVerticalWall(int row, int col) {
        BoardTile tl = getRequiredTile(new Position(row, col));
        BoardTile bl = getRequiredTile(new Position(row + 1, col));
        BoardTile tr = getRequiredTile(new Position(row, col + 1));
        BoardTile br = getRequiredTile(new Position(row + 1, col + 1));

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

    private BoardTile getRequiredTile(Position position) {
        BoardTile tile = tiles.get(position);
        if (tile == null) {
            throw new RuntimeException("Wall outside board");
        }
        return tile;
    }

    public List<Position> getAvailableMoves(Position pos) {
        List<Position> result = new ArrayList<>();
        Position opponent = pos.equals(positionOfPlayer1) ? positionOfPlayer2 : positionOfPlayer1;

        BoardTile tile = tiles.get(pos);
        if (tile == null) return result;

        handleMove(result, pos, opponent, -1, 0, tile.isForwardMovementAvailable());
        handleMove(result, pos, opponent, 1, 0, tile.isBackwardsMovementAvailable());
        handleMove(result, pos, opponent, 0, -1, tile.isLeftMovementAvailable());
        handleMove(result, pos, opponent, 0, 1, tile.isRightMovementAvailable());

        return result;
    }

    private void handleMove(List<Position> result, Position pos, Position opponent,
                            int rowDelta, int colDelta, boolean movementAvailable) {
        if (!movementAvailable) {
            return;
        }

        Position next = pos.move(rowDelta, colDelta);
        if (!tiles.containsKey(next)) {
            return;
        }

        if (!next.equals(opponent)) {
            result.add(next);
            return;
        }

        Position behind = next.move(rowDelta, colDelta);
        if (tiles.containsKey(behind) && !isBlocked(next, behind)) {
            result.add(behind);
            return;
        }

        addDiagonalJumps(result, next, rowDelta, colDelta);
    }

    private void addDiagonalJumps(List<Position> result, Position opponent, int rowDelta, int colDelta) {
        if (rowDelta != 0) {
            addDiagonalJump(result, opponent, 0, -1);
            addDiagonalJump(result, opponent, 0, 1);
        } else {
            addDiagonalJump(result, opponent, -1, 0);
            addDiagonalJump(result, opponent, 1, 0);
        }
    }

    private void addDiagonalJump(List<Position> result, Position from, int rowDelta, int colDelta) {
        Position next = from.move(rowDelta, colDelta);
        if (tiles.containsKey(next) && !isBlocked(from, next)) {
            result.add(next);
        }
    }

    private boolean isBlocked(Position from, Position to) {
        BoardTile tile = tiles.get(from);
        int i1 = from.row();
        int j1 = from.col();

        int i2 = to.row();
        int j2 = to.col();

        if (i1 == i2) {
            if (j2 > j1) return !tile.isRightMovementAvailable();
            else return !tile.isLeftMovementAvailable();
        }

        if (j1 == j2) {
            if (i2 > i1) return !tile.isBackwardsMovementAvailable();
            else return !tile.isForwardMovementAvailable();
        }

        return true;
    }

    public Board copy() {
        Board copy = new Board();
        Map<Position, BoardTile> newTiles = new HashMap<>();

        for (Map.Entry<Position, BoardTile> entry : tiles.entrySet()) {
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
