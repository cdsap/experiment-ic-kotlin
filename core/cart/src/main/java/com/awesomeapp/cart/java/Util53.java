package com.awesomeapp.cart.java;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Utility class with static helper methods.
 */
public final class Util53 {
    private static final Pattern ALPHANUMERIC = Pattern.compile("[^a-zA-Z0-9]");
    private static final int MAX_RETRIES = 3;
    private static final double TOLERANCE = 1e-4;

    private Util53() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String sanitize(String input) {
        if (input == null) return "";
        return ALPHANUMERIC.matcher(input.trim()).replaceAll("");
    }

    public static List<Integer> fibonacci(int n) {
        if (n <= 0) return new ArrayList<>();
        List<Integer> seq = new ArrayList<>();
        seq.add(0);
        if (n == 1) return seq;
        seq.add(1);
        for (int i = 2; i < n; i++) {
            seq.add(seq.get(i - 1) + seq.get(i - 2));
        }
        return seq;
    }

    public static boolean isPrime(int n) {
        if (n < 2) return false;
        if (n == 2) return true;
        if (n % 2 == 0) return false;
        for (int i = 3; i * i <= n; i += 2) {
            if (n % i == 0) return false;
        }
        return true;
    }

    public static List<Integer> primesBelowN(int limit) {
        return IntStream.range(2, limit)
            .filter(Util53::isPrime)
            .boxed()
            .collect(Collectors.toList());
    }

    public static double average(int[] values) {
        if (values == null || values.length == 0) return 0.0;
        return Arrays.stream(values).average().orElse(0.0);
    }

    public static boolean approxEqual(double a, double b) {
        return Math.abs(a - b) < TOLERANCE;
    }

    public static <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }

    public static String repeat(String s, int times) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++) {
            sb.append(s);
        }
        return sb.toString();
    }
}
