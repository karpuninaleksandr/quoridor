package ru.ac.uniyar.model.algorithms;

import ru.ac.uniyar.model.Board;
import ru.ac.uniyar.model.Move;
import ru.ac.uniyar.model.enums.ComputerPlayerHardnessLevel;

//интерфейс для взаимодействия с любым из алгоритмов для ComputerPlayer
public interface Algorithm {
    Move getMove(Board board, ComputerPlayerHardnessLevel hardnessLevel);
}
