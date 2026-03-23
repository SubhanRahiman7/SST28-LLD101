package com.example.snakeandladder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public final class App {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        int n = readPositiveInt(scanner, "Enter n (board size): ");
        int x = readPositiveInt(scanner, "Enter x (number of players): ");
        DifficultyLevel difficulty = readDifficulty(scanner, "Enter difficulty_level (easy/hard): ");

        if (x <= 0) {
            throw new IllegalArgumentException("x must be >= 1");
        }
        if (n < 3) {
            throw new IllegalArgumentException("n must be >= 3 so we can place n snakes and n ladders with distinct start cells.");
        }
        if (x == 1) {
            System.out.println("Note: With 1 player, game ends after that player wins.");
        }

        BoardGenerator generator = new BoardGenerator();
        Board board = generator.generate(n, difficulty);

        List<Player> players = new ArrayList<>();
        for (int i = 1; i <= x; i++) {
            players.add(new Player("P" + i));
        }

        Dice dice = new RandomDice();
        Game game = new Game(board, players, dice);
        game.play();
    }

    private static int readPositiveInt(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine();
            try {
                int value = Integer.parseInt(line.trim());
                if (value > 0) {
                    return value;
                }
                System.out.println("Value must be > 0.");
            } catch (NumberFormatException ex) {
                System.out.println("Invalid integer. Try again.");
            }
        }
    }

    private static DifficultyLevel readDifficulty(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String raw = scanner.nextLine();
            if (raw == null) {
                continue;
            }
            String val = raw.trim().toLowerCase(Locale.ROOT);
            if ("easy".equals(val)) {
                return DifficultyLevel.EASY;
            }
            if ("hard".equals(val)) {
                return DifficultyLevel.HARD;
            }
            System.out.println("Invalid difficulty. Enter either 'easy' or 'hard'.");
        }
    }
}

