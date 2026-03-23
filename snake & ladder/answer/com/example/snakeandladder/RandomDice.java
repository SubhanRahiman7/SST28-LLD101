package com.example.snakeandladder;

import java.util.concurrent.ThreadLocalRandom;

public final class RandomDice implements Dice {
    @Override
    public int roll() {
        return ThreadLocalRandom.current().nextInt(1, 7); // 1..6 inclusive
    }
}

