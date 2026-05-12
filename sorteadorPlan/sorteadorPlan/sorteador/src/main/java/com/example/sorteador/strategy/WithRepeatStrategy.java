package com.example.sorteador.strategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WithRepeatStrategy implements DrawStrategy {

    private final Random random = new Random();

    @Override
    public List<String> draw(List<String> names, int winnersCount) {
        List<String> winners = new ArrayList<>();

        for (int i = 0; i < winnersCount; i++) {
            winners.add(names.get(random.nextInt(names.size())));
        }

        return winners;
    }
}