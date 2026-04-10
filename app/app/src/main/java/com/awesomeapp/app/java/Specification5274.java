package com.awesomeapp.app.java;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public abstract class Specification5274<T> {

    public abstract boolean isSatisfiedBy(T candidate);
    public abstract String describe();

    public Specification5274<T> and(Specification5274<T> other) {
        return new AndSpecification<>(this, other);
    }

    public Specification5274<T> or(Specification5274<T> other) {
        return new OrSpecification<>(this, other);
    }

    public Specification5274<T> not() {
        return new NotSpecification<>(this);
    }

    public List<T> filter(Collection<T> candidates) {
        return candidates.stream().filter(this::isSatisfiedBy).collect(Collectors.toList());
    }

    public long count(Collection<T> candidates) {
        return candidates.stream().filter(this::isSatisfiedBy).count();
    }

    public boolean anyMatch(Collection<T> candidates) {
        return candidates.stream().anyMatch(this::isSatisfiedBy);
    }

    public boolean allMatch(Collection<T> candidates) {
        return candidates.stream().allMatch(this::isSatisfiedBy);
    }

    public static <T> Specification5274<T> of(String description, Predicate<T> predicate) {
        return new PredicateSpecification<>(description, predicate);
    }

    public static <T, V extends Comparable<V>> Specification5274<T> greaterThan(String field, Function<T, V> extractor, V value) {
        return of(field + " > " + value, t -> extractor.apply(t).compareTo(value) > 0);
    }

    public static <T, V extends Comparable<V>> Specification5274<T> lessThan(String field, Function<T, V> extractor, V value) {
        return of(field + " < " + value, t -> extractor.apply(t).compareTo(value) < 0);
    }

    public static <T> Specification5274<T> equalTo(String field, Function<T, ?> extractor, Object value) {
        return of(field + " == " + value, t -> Objects.equals(extractor.apply(t), value));
    }

    public static <T> Specification5274<T> in(String field, Function<T, ?> extractor, Collection<?> values) {
        return of(field + " in " + values, t -> values.contains(extractor.apply(t)));
    }

    private static class PredicateSpecification<T> extends Specification5274<T> {
        private final String description;
        private final Predicate<T> predicate;

        PredicateSpecification(String desc, Predicate<T> pred) { this.description = desc; this.predicate = pred; }
        @Override public boolean isSatisfiedBy(T candidate) { return predicate.test(candidate); }
        @Override public String describe() { return description; }
    }

    private static class AndSpecification<T> extends Specification5274<T> {
        private final Specification5274<T> left, right;
        AndSpecification(Specification5274<T> left, Specification5274<T> right) { this.left = left; this.right = right; }
        @Override public boolean isSatisfiedBy(T candidate) { return left.isSatisfiedBy(candidate) && right.isSatisfiedBy(candidate); }
        @Override public String describe() { return "(" + left.describe() + " AND " + right.describe() + ")"; }
    }

    private static class OrSpecification<T> extends Specification5274<T> {
        private final Specification5274<T> left, right;
        OrSpecification(Specification5274<T> left, Specification5274<T> right) { this.left = left; this.right = right; }
        @Override public boolean isSatisfiedBy(T candidate) { return left.isSatisfiedBy(candidate) || right.isSatisfiedBy(candidate); }
        @Override public String describe() { return "(" + left.describe() + " OR " + right.describe() + ")"; }
    }

    private static class NotSpecification<T> extends Specification5274<T> {
        private final Specification5274<T> spec;
        NotSpecification(Specification5274<T> spec) { this.spec = spec; }
        @Override public boolean isSatisfiedBy(T candidate) { return !spec.isSatisfiedBy(candidate); }
        @Override public String describe() { return "NOT(" + spec.describe() + ")"; }
    }
}
