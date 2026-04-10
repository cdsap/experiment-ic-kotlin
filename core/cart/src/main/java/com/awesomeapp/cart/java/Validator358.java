package com.awesomeapp.cart.java;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Validation framework class 358 with rule-based validation.
 */
public class Validator358<T> {
    private final List<ValidationRule<T>> rules = new ArrayList<>();
    private final Map<String, List<String>> errors = new HashMap<>();
    private boolean stopOnFirstError = false;

    public static class ValidationRule<T> {
        private final String fieldName;
        private final String message;
        private final Predicate<T> condition;

        public ValidationRule(String fieldName, String message, Predicate<T> condition) {
            this.fieldName = fieldName;
            this.message = message;
            this.condition = condition;
        }

        public String getFieldName() { return fieldName; }
        public String getMessage() { return message; }
        public boolean test(T value) { return condition.test(value); }
    }

    public Validator358<T> addRule(String field, String message, Predicate<T> condition) {
        rules.add(new ValidationRule<>(field, message, condition));
        return this;
    }

    public Validator358<T> stopOnFirst(boolean stop) {
        this.stopOnFirstError = stop;
        return this;
    }

    public boolean validate(T target) {
        errors.clear();
        for (ValidationRule<T> rule : rules) {
            if (!rule.test(target)) {
                errors.computeIfAbsent(rule.getFieldName(), k -> new ArrayList<>())
                    .add(rule.getMessage());
                if (stopOnFirstError) break;
            }
        }
        return errors.isEmpty();
    }

    public Map<String, List<String>> getErrors() {
        return new HashMap<>(errors);
    }

    public List<String> getErrorsForField(String field) {
        return errors.getOrDefault(field, new ArrayList<>());
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public int errorCount() {
        return errors.values().stream().mapToInt(List::size).sum();
    }

    public static Predicate<String> matchesPattern(String regex) {
        Pattern p = Pattern.compile(regex);
        return s -> s != null && p.matcher(s).matches();
    }

    public static Predicate<String> minLength(int len) {
        return s -> s != null && s.length() >= len;
    }

    public static Predicate<String> maxLength(int len) {
        return s -> s != null && s.length() <= len;
    }

    public static <N extends Comparable<N>> Predicate<N> between(N min, N max) {
        return n -> n != null && n.compareTo(min) >= 0 && n.compareTo(max) <= 0;
    }
}
