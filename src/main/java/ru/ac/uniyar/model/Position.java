package ru.ac.uniyar.model;

//координаты клетки игрового поля
public record Position(int row, int col) {
    public Position move(int rowDelta, int colDelta) {
        return new Position(row + rowDelta, col + colDelta);
    }
}
