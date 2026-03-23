package com.example.snakeandladder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Generates a random set of n snakes and n ladders placed on an n*n board.
 * Generation ensures the jump mapping is acyclic.
 */
public final class BoardGenerator {
    private static final int MAX_ATTEMPTS_PER_EDGE = 10_000;

    private final Random random = new Random();

    public Board generate(int n, DifficultyLevel difficulty) {
        if (n <= 1) {
            throw new IllegalArgumentException("n must be >= 2 to place snakes and ladders.");
        }
        int size = n;
        int lastCell = size * size;
        if (lastCell < 4) {
            throw new IllegalArgumentException("Board too small for snakes and ladders.");
        }

        // We require unique start positions for ladders+snakes to keep the model simple.
        // If your instructor doesn't require this, we can relax it later.
        int maxDistinctStarts = lastCell - 1; // positions 1..lastCell-1
        if (maxDistinctStarts < (2 * n)) {
            throw new IllegalArgumentException("Board too small for " + n + " snakes and " + n + " ladders (distinct start positions required).");
        }

        int pivot = Math.max(2, lastCell / 3);
        int easyMaxSnakeLen = pivot - 1;
        int hardMinSnakeLen = pivot;
        int easyMinLadderLen = pivot;
        int hardMaxLadderLen = pivot - 1;

        Map<Integer, Integer> jumpMap = new HashMap<>();
        Set<Integer> usedStartPositions = new HashSet<>();

        // Place ladders first, then snakes (either order works with cycle checks).
        for (int i = 0; i < n; i++) {
            placeLadder(lastCell, difficulty, pivot, easyMinLadderLen, hardMaxLadderLen, jumpMap, usedStartPositions);
        }
        for (int i = 0; i < n; i++) {
            placeSnake(lastCell, difficulty, pivot, easyMaxSnakeLen, hardMinSnakeLen, jumpMap, usedStartPositions);
        }

        if (!isAcyclic(jumpMap, lastCell)) {
            throw new IllegalStateException("Generated board unexpectedly has a cycle.");
        }

        return new Board(size, jumpMap);
    }

    private void placeLadder(
            int lastCell,
            DifficultyLevel difficulty,
            int pivot,
            int easyMinLadderLen,
            int hardMaxLadderLen,
            Map<Integer, Integer> jumpMap,
            Set<Integer> usedStartPositions
    ) {
        for (int attempt = 0; attempt < MAX_ATTEMPTS_PER_EDGE; attempt++) {
            int start = randomInt(1, lastCell - 1); // ladder start cannot be lastCell
            if (usedStartPositions.contains(start)) {
                continue;
            }

            int minLen;
            int maxLen;
            if (difficulty == DifficultyLevel.EASY) {
                minLen = easyMinLadderLen;
                maxLen = lastCell - start;
            } else {
                minLen = 1;
                maxLen = Math.min(hardMaxLadderLen, lastCell - start);
            }

            if (minLen > maxLen) {
                continue;
            }

            int len = randomInt(minLen, maxLen);
            int end = start + len;

            // Ladder start is always smaller; end is larger by definition.
            if (end <= start) {
                continue;
            }

            jumpMap.put(start, end);
            usedStartPositions.add(start);

            if (isAcyclic(jumpMap, lastCell)) {
                return;
            }

            // revert and retry
            jumpMap.remove(start);
            usedStartPositions.remove(start);
        }

        throw new IllegalStateException("Failed to place ladder after many attempts. Try a different difficulty or n.");
    }

    private void placeSnake(
            int lastCell,
            DifficultyLevel difficulty,
            int pivot,
            int easyMaxSnakeLen,
            int hardMinSnakeLen,
            Map<Integer, Integer> jumpMap,
            Set<Integer> usedStartPositions
    ) {
        for (int attempt = 0; attempt < MAX_ATTEMPTS_PER_EDGE; attempt++) {
            int head = randomInt(2, lastCell - 1); // snake head cannot be lastCell to keep win possible
            if (usedStartPositions.contains(head)) {
                continue;
            }

            int minLen;
            int maxLen;
            if (difficulty == DifficultyLevel.EASY) {
                minLen = 1;
                maxLen = Math.min(easyMaxSnakeLen, head - 1);
            } else {
                minLen = Math.max(hardMinSnakeLen, 1);
                maxLen = head - 1;
            }

            if (minLen > maxLen) {
                continue;
            }

            int len = randomInt(minLen, maxLen);
            int tail = head - len;

            if (tail >= head) {
                continue;
            }

            jumpMap.put(head, tail);
            usedStartPositions.add(head);

            if (isAcyclic(jumpMap, lastCell)) {
                return;
            }

            // revert and retry
            jumpMap.remove(head);
            usedStartPositions.remove(head);
        }

        throw new IllegalStateException("Failed to place snake after many attempts. Try a different difficulty or n.");
    }

    private boolean isAcyclic(Map<Integer, Integer> jumpMap, int lastCell) {
        // Directed graph cycle detection: edge u -> jumpMap[u] if present.
        // Node set is 1..lastCell (0 is outside board and has no outgoing edges).
        int[] color = new int[lastCell + 1]; // 0=unvisited, 1=visiting, 2=done

        for (int node = 1; node <= lastCell; node++) {
            if (color[node] == 0 && hasCycleDfs(node, jumpMap, color)) {
                return false;
            }
        }
        return true;
    }

    private boolean hasCycleDfs(int node, Map<Integer, Integer> jumpMap, int[] color) {
        Integer next = jumpMap.get(node);
        if (next == null) {
            color[node] = 2;
            return false;
        }

        if (color[node] == 1) {
            return true;
        }
        if (color[node] == 2) {
            return false;
        }

        color[node] = 1;
        boolean cycle = hasCycleDfs(next, jumpMap, color);
        color[node] = 2;
        return cycle;
    }

    private int randomInt(int inclusiveMin, int inclusiveMax) {
        if (inclusiveMin > inclusiveMax) {
            throw new IllegalArgumentException("min > max in randomInt");
        }
        return inclusiveMin + random.nextInt(inclusiveMax - inclusiveMin + 1);
    }
}

