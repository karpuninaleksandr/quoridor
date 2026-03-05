package ru.ac.uniyar.model.players;

import lombok.Setter;
import ru.ac.uniyar.model.Board;
import ru.ac.uniyar.model.Move;
import ru.ac.uniyar.model.algorithms.Algorithm;
import ru.ac.uniyar.model.enums.ComputerPlayerHardnessLevel;

//реализация интерфейса Player для компьютера - методы будут ожидать выполнения его алгоритма
@Setter
public class ComputerPlayer extends Player {
    private Algorithm algorithm;
    private ComputerPlayerHardnessLevel hardnessLevel;

    @Override
    public Move getMove(Board board) {
        return algorithm.getMove(board, hardnessLevel);
    }
}
