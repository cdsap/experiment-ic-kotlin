package com.awesomeapp.app.java;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Graph5470<V> {

    public static class Edge<V> {
        private final V source;
        private final V target;
        private final double weight;

        public Edge(V source, V target, double weight) {
            this.source = source; this.target = target; this.weight = weight;
        }

        public V getSource() { return source; }
        public V getTarget() { return target; }
        public double getWeight() { return weight; }
    }

    private final Map<V, Set<Edge<V>>> adjacencyList = new HashMap<>();
    private final boolean directed;

    public Graph5470(boolean directed) { this.directed = directed; }

    public void addVertex(V vertex) {
        adjacencyList.putIfAbsent(vertex, new LinkedHashSet<>());
    }

    public void addEdge(V source, V target, double weight) {
        addVertex(source);
        addVertex(target);
        adjacencyList.get(source).add(new Edge<>(source, target, weight));
        if (!directed) {
            adjacencyList.get(target).add(new Edge<>(target, source, weight));
        }
    }

    public void addEdge(V source, V target) { addEdge(source, target, 1.0); }

    public Set<V> getVertices() { return Collections.unmodifiableSet(adjacencyList.keySet()); }

    public Set<Edge<V>> getEdges(V vertex) {
        return adjacencyList.getOrDefault(vertex, Collections.emptySet());
    }

    public List<V> getNeighbors(V vertex) {
        return getEdges(vertex).stream().map(Edge::getTarget).collect(Collectors.toList());
    }

    public void bfs(V start, Consumer<V> visitor) {
        Set<V> visited = new HashSet<>();
        Queue<V> queue = new ArrayDeque<>();
        visited.add(start);
        queue.offer(start);
        while (!queue.isEmpty()) {
            V current = queue.poll();
            visitor.accept(current);
            for (Edge<V> edge : getEdges(current)) {
                if (visited.add(edge.getTarget())) {
                    queue.offer(edge.getTarget());
                }
            }
        }
    }

    public void dfs(V start, Consumer<V> visitor) {
        Set<V> visited = new HashSet<>();
        dfsRecursive(start, visited, visitor);
    }

    private void dfsRecursive(V vertex, Set<V> visited, Consumer<V> visitor) {
        visited.add(vertex);
        visitor.accept(vertex);
        for (Edge<V> edge : getEdges(vertex)) {
            if (!visited.contains(edge.getTarget())) {
                dfsRecursive(edge.getTarget(), visited, visitor);
            }
        }
    }

    public Optional<List<V>> findPath(V from, V to) {
        Map<V, V> parentMap = new HashMap<>();
        Set<V> visited = new HashSet<>();
        Queue<V> queue = new ArrayDeque<>();
        visited.add(from);
        queue.offer(from);
        while (!queue.isEmpty()) {
            V current = queue.poll();
            if (current.equals(to)) {
                return Optional.of(reconstructPath(parentMap, from, to));
            }
            for (Edge<V> edge : getEdges(current)) {
                if (visited.add(edge.getTarget())) {
                    parentMap.put(edge.getTarget(), current);
                    queue.offer(edge.getTarget());
                }
            }
        }
        return Optional.empty();
    }

    private List<V> reconstructPath(Map<V, V> parentMap, V from, V to) {
        List<V> path = new ArrayList<>();
        V current = to;
        while (!current.equals(from)) { path.add(current); current = parentMap.get(current); }
        path.add(from);
        Collections.reverse(path);
        return path;
    }

    public boolean hasCycle() {
        Set<V> visited = new HashSet<>();
        Set<V> inStack = new HashSet<>();
        for (V vertex : adjacencyList.keySet()) {
            if (!visited.contains(vertex) && hasCycleDfs(vertex, visited, inStack)) return true;
        }
        return false;
    }

    private boolean hasCycleDfs(V vertex, Set<V> visited, Set<V> inStack) {
        visited.add(vertex);
        inStack.add(vertex);
        for (Edge<V> edge : getEdges(vertex)) {
            if (inStack.contains(edge.getTarget())) return true;
            if (!visited.contains(edge.getTarget()) && hasCycleDfs(edge.getTarget(), visited, inStack)) return true;
        }
        inStack.remove(vertex);
        return false;
    }

    public int getVertexCount() { return adjacencyList.size(); }
    public int getEdgeCount() {
        int count = adjacencyList.values().stream().mapToInt(Set::size).sum();
        return directed ? count : count / 2;
    }
    public int getDegree(V vertex) { return getEdges(vertex).size(); }
}
