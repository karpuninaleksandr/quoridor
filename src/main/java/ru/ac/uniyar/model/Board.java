package ru.ac.uniyar.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

//класс описывающий состояние игрового поля
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Board {
    private Map<String, BoardTile> tiles;
}
