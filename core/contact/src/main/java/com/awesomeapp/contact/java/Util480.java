package com.awesomeapp.contact.java;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class Util480 {
    public static String formatId(String prefix, int number) {
        return String.format("%s-%06d", prefix, number);
    }

    public static <T> List<T> safeSubList(List<T> list, int from, int to) {
        if (list == null || list.isEmpty()) return new ArrayList<>();
        int end = Math.min(to, list.size());
        int start = Math.max(0, Math.min(from, end));
        return new ArrayList<>(list.subList(start, end));
    }

    public static boolean isNullOrEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    public static List<String> filterNonEmpty(List<String> items) {
        if (items == null) return new ArrayList<>();
        return items.stream().filter(s -> !isNullOrEmpty(s)).collect(Collectors.toList());
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
