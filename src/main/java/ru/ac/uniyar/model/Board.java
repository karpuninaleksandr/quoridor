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
}
