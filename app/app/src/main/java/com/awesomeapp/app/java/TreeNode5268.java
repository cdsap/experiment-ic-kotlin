package com.awesomeapp.app.java;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class TreeNode5268<T> {

    public enum TraversalOrder { PRE_ORDER, POST_ORDER, BREADTH_FIRST }

    private T data;
    private TreeNode5268<T> parent;
    private final List<TreeNode5268<T>> children = new ArrayList<>();
    private final Map<String, Object> metadata = new java.util.HashMap<>();

    public TreeNode5268(T data) { this.data = data; }

    public TreeNode5268<T> addChild(T childData) {
        TreeNode5268<T> child = new TreeNode5268<>(childData);
        child.parent = this;
        children.add(child);
        return child;
    }

    public void removeChild(TreeNode5268<T> child) {
        children.remove(child);
        child.parent = null;
    }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    public Optional<TreeNode5268<T>> getParent() { return Optional.ofNullable(parent); }
    public List<TreeNode5268<T>> getChildren() { return Collections.unmodifiableList(children); }
    public boolean isLeaf() { return children.isEmpty(); }
    public boolean isRoot() { return parent == null; }

    public int getDepth() {
        int depth = 0;
        TreeNode5268<T> current = this;
        while (current.parent != null) { depth++; current = current.parent; }
        return depth;
    }

    public int getHeight() {
        if (isLeaf()) return 0;
        return 1 + children.stream().mapToInt(TreeNode5268::getHeight).max().orElse(0);
    }

    public int size() {
        return 1 + children.stream().mapToInt(TreeNode5268::size).sum();
    }

    public void traverse(TraversalOrder order, Consumer<T> visitor) {
        switch (order) {
            case PRE_ORDER: preOrder(visitor); break;
            case POST_ORDER: postOrder(visitor); break;
            case BREADTH_FIRST: breadthFirst(visitor); break;
        }
    }

    private void preOrder(Consumer<T> visitor) {
        visitor.accept(data);
        children.forEach(c -> c.preOrder(visitor));
    }

    private void postOrder(Consumer<T> visitor) {
        children.forEach(c -> c.postOrder(visitor));
        visitor.accept(data);
    }

    private void breadthFirst(Consumer<T> visitor) {
        Queue<TreeNode5268<T>> queue = new ArrayDeque<>();
        queue.offer(this);
        while (!queue.isEmpty()) {
            TreeNode5268<T> node = queue.poll();
            visitor.accept(node.data);
            queue.addAll(node.children);
        }
    }

    public Optional<TreeNode5268<T>> find(Predicate<T> predicate) {
        if (predicate.test(data)) return Optional.of(this);
        for (TreeNode5268<T> child : children) {
            Optional<TreeNode5268<T>> found = child.find(predicate);
            if (found.isPresent()) return found;
        }
        return Optional.empty();
    }

    public <R> TreeNode5268<R> map(Function<T, R> mapper) {
        TreeNode5268<R> mapped = new TreeNode5268<>(mapper.apply(data));
        for (TreeNode5268<T> child : children) {
            TreeNode5268<R> mappedChild = child.map(mapper);
            mappedChild.parent = mapped;
            mapped.children.add(mappedChild);
        }
        return mapped;
    }

    public List<T> toList(TraversalOrder order) {
        List<T> result = new ArrayList<>();
        traverse(order, result::add);
        return result;
    }

    public List<T> getPath() {
        List<T> path = new ArrayList<>();
        TreeNode5268<T> current = this;
        while (current != null) { path.add(current.data); current = current.parent; }
        Collections.reverse(path);
        return path;
    }
}
