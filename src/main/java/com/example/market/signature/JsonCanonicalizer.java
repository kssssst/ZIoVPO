package com.example.market.signature;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Реализация RFC 8785 JSON Canonicalization Scheme (JCS).
 * Преобразует JSON в каноническую форму без лишних пробелов,
 * с сортировкой ключей объектов и строгими правилами экранирования.
 */
public class JsonCanonicalizer {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
            .configure(SerializationFeature.INDENT_OUTPUT, false);

    public static byte[] canonicalize(Object object) throws IOException {
        JsonNode node = MAPPER.valueToTree(object);
        String canonicalString = canonicalizeNode(node);
        return canonicalString.getBytes(StandardCharsets.UTF_8);
    }

    private static String canonicalizeNode(JsonNode node) throws IOException {
        if (node == null || node.isNull()) {
            return "null";
        }
        if (node.isBoolean()) {
            return node.asBoolean() ? "true" : "false";
        }
        if (node.isNumber()) {
            // Числа представляем как в ECMAScript (без лишних нулей)
            if (node.isInt()) return Integer.toString(node.asInt());
            if (node.isLong()) return Long.toString(node.asLong());
            if (node.isDouble()) {
                double d = node.asDouble();
                if (Double.isNaN(d) || Double.isInfinite(d)) {
                    throw new IllegalArgumentException("NaN/Infinity not allowed in JCS");
                }
                String s = Double.toString(d);
                // Убираем .0 для целых чисел (как в JS)
                if (s.endsWith(".0")) s = s.substring(0, s.length() - 2);
                return s;
            }
            return node.asText();
        }
        if (node.isTextual()) {
            return quote(node.asText());
        }
        if (node.isArray()) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < node.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(canonicalizeNode(node.get(i)));
            }
            sb.append("]");
            return sb.toString();
        }
        if (node.isObject()) {
            // Сортируем ключи по правилам JCS (сравнение UTF-16 code units)
            List<String> sortedKeys = new ArrayList<>();
            node.fieldNames().forEachRemaining(sortedKeys::add);
            sortedKeys.sort((a, b) -> {
                char[] ca = a.toCharArray();
                char[] cb = b.toCharArray();
                int min = Math.min(ca.length, cb.length);
                for (int i = 0; i < min; i++) {
                    if (ca[i] != cb[i]) return Character.compare(ca[i], cb[i]);
                }
                return Integer.compare(ca.length, cb.length);
            });

            StringBuilder sb = new StringBuilder("{");
            for (int i = 0; i < sortedKeys.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(quote(sortedKeys.get(i)));
                sb.append(":");
                sb.append(canonicalizeNode(node.get(sortedKeys.get(i))));
            }
            sb.append("}");
            return sb.toString();
        }
        throw new IllegalArgumentException("Unsupported JSON node: " + node);
    }

    private static String quote(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append("\"");
        return sb.toString();
    }
}