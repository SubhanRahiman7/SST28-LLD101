package com.example.snakeandladder;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Collections;

/**
 * Board stores the jump mapping:
 * - ladderStart -> ladderEnd
 * - snakeHead -> snakeTail
 */
public final class Board {
    private final int size;
    private final int lastCell;
    private final Map<Integer, Integer> jumpMap;

    Board(int size, Map<Integer, Integer> jumpMap) {
        if (size <= 0) {
            throw new IllegalArgumentException("size must be > 0");
        }
        if (jumpMap == null) {
            throw new IllegalArgumentException("jumpMap cannot be null");
        }
        this.size = size;
        this.lastCell = size * size;
        this.jumpMap = jumpMap;
    }

    public int getLastCell() {
        return lastCell;
    }

    Map<Integer, Integer> getJumpMap() {
        return Collections.unmodifiableMap(jumpMap);
    }

    public int resolveJumps(int position) {
        if (position < 0) {
            throw new IllegalArgumentException("position cannot be negative");
        }
        Set<Integer> visited = new HashSet<>();
        int current = position;

        // Chain-jumps: if destination is also a jump start, apply again.
        while (jumpMap.containsKey(current) && !visited.contains(current)) {
            visited.add(current);
            current = jumpMap.get(current);
        }

        if (visited.contains(current)) {
            // Should never happen because generator avoids cycles; runtime safeguard for correctness.
            throw new IllegalStateException("Jump cycle detected at position: " + current);
        }
        return current;
    }
}

