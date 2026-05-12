package com.example.sorteador.strategy;

import java.util.Collections;
import java.util.List;

public class NoRepeatStrategy implements DrawStrategy {

    @Override
    public List<String> draw(List<String> names, int winnersCount) {
        Collections.shuffle(names);
        return names.stream().limit(winnersCount).toList();
    }
}