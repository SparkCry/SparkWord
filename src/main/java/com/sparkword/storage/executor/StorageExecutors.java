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
package com.sparkword.storage.executor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class StorageExecutors {

    public final ExecutorService writer;
    public final ExecutorService reader;

    private static final ThreadLocal<Boolean> IS_WRITER_THREAD = ThreadLocal.withInitial(() -> false);

    public StorageExecutors() {
        this.writer = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "SparkWord-DB-Writer");
            t.setDaemon(false);

            return t;
        });

        this.writer.submit(() -> IS_WRITER_THREAD.set(true));

        this.reader = Executors.newVirtualThreadPerTaskExecutor();
    }

    public boolean isWriterThread() {
        return IS_WRITER_THREAD.get();
    }

    public void shutdown() {
        reader.shutdownNow();
        writer.shutdown();
        try {
            if (!writer.awaitTermination(5, TimeUnit.SECONDS)) {
                writer.shutdownNow();
            }
        } catch (InterruptedException e) {
            writer.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
