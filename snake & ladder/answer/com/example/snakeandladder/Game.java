package com.example.snakeandladder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Game {
    private final Board board;
    private final List<Player> players;
    private final Dice dice;
    private final int lastCell;
    private final int targetActivePlayers;

    public Game(Board board, List<Player> players, Dice dice) {
        if (board == null) {
            throw new IllegalArgumentException("board cannot be null");
        }
        if (players == null || players.isEmpty()) {
            throw new IllegalArgumentException("players cannot be null/empty");
        }
        if (dice == null) {
            throw new IllegalArgumentException("dice cannot be null");
        }
        this.board = board;
        this.players = players;
        this.dice = dice;
        this.lastCell = board.getLastCell();
        // "Continue till there are at least 2 players still playing to win"
        // => keep playing while active (not yet at last cell) players >= 2.
        this.targetActivePlayers = Math.min(2, players.size());
    }

    public void play() {
        // winners keeps insertion order for nicer output.
        Map<Player, Integer> winners = new LinkedHashMap<>();

        int turn = 0;
        int maxTurns = 100_000; // safeguard to avoid accidental infinite loops

        System.out.println("Snakes & Ladders Board size: " + lastCell);
        printJumps();

        while ((players.size() - winners.size()) >= targetActivePlayers && turn < maxTurns) {
            for (Player player : players) {
                if (winners.containsKey(player)) {
                    continue; // already won
                }
                if (player.getPosition() == lastCell) {
                    winners.put(player, turn);
                    if ((players.size() - winners.size()) < targetActivePlayers) {
                        break;
                    }
                    continue;
                }

                int roll = dice.roll();
                int current = player.getPosition();
                int proposed = current + roll;

                System.out.println(player.getName() + " rolled " + roll + " (pos=" + current + ")");

                if (proposed > lastCell) {
                    // "If a piece is supposed to move outside position 100, it does not move."
                    System.out.println("  Move blocked: would go past " + lastCell + ". Staying at " + current);
                    continue;
                }

                // Move then resolve possible snake/ladder chain.
                player.setPosition(proposed);
                int resolved = board.resolveJumps(proposed);
                player.setPosition(resolved);

                if (resolved != proposed) {
                    System.out.println("  Jump! Landed on " + proposed + ", moved to " + resolved);
                }

                if (resolved == lastCell) {
                    winners.put(player, turn);
                    System.out.println("  " + player.getName() + " reached the last cell and won!");
                    if ((players.size() - winners.size()) < targetActivePlayers) {
                        break;
                    }
                }
            }
            turn++;
        }

        System.out.println();
        int activePlayers = players.size() - winners.size();
        System.out.println("Game Over. Active (not yet won) players: " + activePlayers);
        System.out.println("Winners:");

        List<Player> winnerList = new ArrayList<>(winners.keySet());
        for (int i = 0; i < winnerList.size(); i++) {
            Player p = winnerList.get(i);
            System.out.println((i + 1) + ". " + p.getName());
        }
    }

    private void printJumps() {
        System.out.println("Jump mapping (start -> end):");
        for (Map.Entry<Integer, Integer> e : board.getJumpMap().entrySet()) {
            int start = e.getKey();
            int end = e.getValue();
            if (end > start) {
                System.out.println("  Ladder: " + start + " -> " + end);
            } else {
                System.out.println("  Snake:  " + start + " -> " + end);
            }
        }
        System.out.println();
    }
}

