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
package com.sparkword.util;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class BenchmarkReporter {

    private static final Map<String, Map<String, MetricEntry>> metricsStore = new ConcurrentHashMap<>();

    private static final List<String> alertsStore = new CopyOnWriteArrayList<>();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            String outputPath = System.getProperty("metrics.out");
            if (outputPath != null && !outputPath.isEmpty()) {
                exportToJson(outputPath);
            }
        }));
    }

    public static void log(String category, String key, Object value, String unit) {
        System.out.printf("[METRIC] %s | %s | %s | %s%n", category, key, value, unit);

        metricsStore.computeIfAbsent(category, k -> new ConcurrentHashMap<>())
            .put(key, new MetricEntry(value, unit));
    }

    public static void alert(String category, String message) {
        System.out.println(String.format("[ALERT] %s | %s", category, message));
        alertsStore.add("[" + category + "] " + message);
    }

    private static void exportToJson(String path) {
        try (FileWriter writer = new FileWriter(path)) {
            StringBuilder json = new StringBuilder();
            json.append("{");

            json.append("\"metrics\": {");
            int catIndex = 0;
            for (Map.Entry<String, Map<String, MetricEntry>> catEntry : metricsStore.entrySet()) {
                if (catIndex++ > 0) json.append(",");
                json.append("\"").append(escape(catEntry.getKey())).append("\": {");
                int metricIndex = 0;
                for (Map.Entry<String, MetricEntry> metric : catEntry.getValue().entrySet()) {
                    if (metricIndex++ > 0) json.append(",");

                    json.append("\"").append(escape(metric.getKey())).append("\": {");
                    Object val = metric.getValue().value;

                    if (val instanceof Number) json.append("\"value\": ").append(val);
                    else json.append("\"value\": \"").append(escape(val.toString())).append("\"");

                    json.append(", \"unit\": \"").append(escape(metric.getValue().unit)).append("\"");
                    json.append("}");
                }
                json.append("}");
            }
            json.append("},");

            json.append("\"alerts\": [");

            for (int i = 0; i < alertsStore.size(); i++) {
                if (i > 0) json.append(",");
                json.append("\"").append(escape(alertsStore.get(i))).append("\"");
            }
            json.append("]}");

            writer.write(json.toString());
            System.out.println("[BenchmarkReporter] Metrics exported to: " + path);
        } catch (IOException e) {
            System.err.println("[BenchmarkReporter] Error writing JSON: " + e.getMessage());
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"").replace("\n", " ").replace("\r", " ");
    }

    private static class MetricEntry {
        Object value;
        String unit;

        MetricEntry(Object value, String unit) {
            this.value = value;
            this.unit = unit;
        }
    }
}
