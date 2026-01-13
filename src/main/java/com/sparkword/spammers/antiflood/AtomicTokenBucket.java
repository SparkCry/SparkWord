/*
 * This file is part of SparkWord - https://github.com/SparkCry/SparkWord
 * Copyright (C) 2026 SparkCry and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.sparkword.spammers.antiflood;

import java.util.concurrent.atomic.AtomicReference;

public class AtomicTokenBucket {
    private final long capacity;
    private final double refillRatePerNano;
    private final AtomicReference<BucketState> state;

    public AtomicTokenBucket(long capacity, double refillRatePerSecond) {
        this.capacity = capacity;

        this.refillRatePerNano = refillRatePerSecond / 1_000_000_000.0;
        this.state = new AtomicReference<>(new BucketState(capacity, System.nanoTime()));
    }

    public boolean tryConsume() {
        BucketState current;
        BucketState next;
        do {
            current = state.get();
            long now = System.nanoTime();

            long timeElapsed = Math.max(0, now - current.lastRefillTimeNano());
            double tokensToAdd = timeElapsed * refillRatePerNano;

            double newTokens = Math.min(capacity, current.tokens() + tokensToAdd);

            if (newTokens < 1.0) return false;

            next = new BucketState(newTokens - 1.0, now);
        } while (!state.compareAndSet(current, next));

        return true;
    }
}
