package com.example.sorteador.strategy;

import java.util.List;

public interface DrawStrategy {
    List<String> draw(List<String> names, int winnersCount);

}
