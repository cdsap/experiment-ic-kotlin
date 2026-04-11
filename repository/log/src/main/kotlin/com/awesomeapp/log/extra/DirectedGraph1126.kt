package com.awesomeapp.log.extra

import java.util.LinkedList
import java.util.PriorityQueue

class DirectedGraph1126<V : Any> {

    data class Edge<V>(val from: V, val to: V, val weight: Double = 1.0, val label: String? = null)

    data class PathResult<V>(val path: List<V>, val totalWeight: Double, val edgeCount: Int) {
        val isEmpty: Boolean get() = path.isEmpty()
    }

    data class GraphStats(
        val vertexCount: Int, val edgeCount: Int,
        val avgDegree: Double, val maxInDegree: Int, val maxOutDegree: Int,
        val isDAG: Boolean, val componentCount: Int
    )

    private val adjacency = mutableMapOf<V, MutableList<Edge<V>>>()
    private val reverseAdj = mutableMapOf<V, MutableList<Edge<V>>>()
    private val vertices = mutableSetOf<V>()

    fun addVertex(vertex: V) { vertices.add(vertex); adjacency.getOrPut(vertex) { mutableListOf() } }

    fun addEdge(from: V, to: V, weight: Double = 1.0, label: String? = null) {
        addVertex(from); addVertex(to)
        val edge = Edge(from, to, weight, label)
        adjacency.getOrPut(from) { mutableListOf() }.add(edge)
        reverseAdj.getOrPut(to) { mutableListOf() }.add(edge)
    }

    fun removeVertex(vertex: V) {
        vertices.remove(vertex)
        adjacency.remove(vertex)
        reverseAdj.remove(vertex)
        adjacency.values.forEach { edges -> edges.removeIf { it.to == vertex } }
        reverseAdj.values.forEach { edges -> edges.removeIf { it.from == vertex } }
    }

    fun removeEdge(from: V, to: V) {
        adjacency[from]?.removeIf { it.to == to }
        reverseAdj[to]?.removeIf { it.from == from }
    }

    fun neighbors(vertex: V): List<V> = adjacency[vertex]?.map { it.to } ?: emptyList()
    fun incomingEdges(vertex: V): List<Edge<V>> = reverseAdj[vertex] ?: emptyList()
    fun outgoingEdges(vertex: V): List<Edge<V>> = adjacency[vertex] ?: emptyList()
    fun outDegree(vertex: V): Int = adjacency[vertex]?.size ?: 0
    fun inDegree(vertex: V): Int = reverseAdj[vertex]?.size ?: 0

    fun bfs(start: V): List<V> {
        val visited = mutableSetOf<V>()
        val queue = LinkedList<V>()
        val result = mutableListOf<V>()
        queue.add(start); visited.add(start)
        while (queue.isNotEmpty()) {
            val current = queue.poll()
            result.add(current)
            for (neighbor in neighbors(current)) {
                if (visited.add(neighbor)) queue.add(neighbor)
            }
        }
        return result
    }

    fun dfs(start: V): List<V> {
        val visited = mutableSetOf<V>()
        val result = mutableListOf<V>()
        fun visit(v: V) {
            if (!visited.add(v)) return
            result.add(v)
            neighbors(v).forEach { visit(it) }
        }
        visit(start)
        return result
    }

    fun shortestPath(start: V, end: V): PathResult<V> {
        val dist = mutableMapOf<V, Double>().withDefault { Double.MAX_VALUE }
        val prev = mutableMapOf<V, V>()
        val pq = PriorityQueue<Pair<V, Double>>(compareBy { it.second })

        dist[start] = 0.0
        pq.add(start to 0.0)

        while (pq.isNotEmpty()) {
            val (current, d) = pq.poll()
            if (d > dist.getValue(current)) continue
            if (current == end) break

            for (edge in outgoingEdges(current)) {
                val newDist = d + edge.weight
                if (newDist < dist.getValue(edge.to)) {
                    dist[edge.to] = newDist
                    prev[edge.to] = current
                    pq.add(edge.to to newDist)
                }
            }
        }

        if (!prev.containsKey(end) && start != end) return PathResult(emptyList(), 0.0, 0)

        val path = mutableListOf(end)
        var current = end
        while (current != start) {
            current = prev[current] ?: return PathResult(emptyList(), 0.0, 0)
            path.add(0, current)
        }
        return PathResult(path, dist.getValue(end), path.size - 1)
    }

    fun topologicalSort(): List<V>? {
        val inDegrees = mutableMapOf<V, Int>()
        vertices.forEach { inDegrees[it] = inDegree(it) }
        val queue = LinkedList(vertices.filter { inDegrees[it] == 0 })
        val result = mutableListOf<V>()

        while (queue.isNotEmpty()) {
            val v = queue.poll()
            result.add(v)
            for (neighbor in neighbors(v)) {
                inDegrees[neighbor] = (inDegrees[neighbor] ?: 0) - 1
                if (inDegrees[neighbor] == 0) queue.add(neighbor)
            }
        }
        return if (result.size == vertices.size) result else null
    }

    fun hasCycle(): Boolean = topologicalSort() == null

    fun connectedComponents(): List<Set<V>> {
        val visited = mutableSetOf<V>()
        val components = mutableListOf<Set<V>>()
        for (v in vertices) {
            if (v !in visited) {
                val component = mutableSetOf<V>()
                val queue = LinkedList<V>()
                queue.add(v)
                while (queue.isNotEmpty()) {
                    val current = queue.poll()
                    if (component.add(current)) {
                        neighbors(current).forEach { if (it !in component) queue.add(it) }
                        incomingEdges(current).map { it.from }.forEach { if (it !in component) queue.add(it) }
                    }
                }
                visited.addAll(component)
                components.add(component)
            }
        }
        return components
    }

    fun getStats(): GraphStats {
        val edgeCount = adjacency.values.sumOf { it.size }
        return GraphStats(
            vertexCount = vertices.size, edgeCount = edgeCount,
            avgDegree = if (vertices.isEmpty()) 0.0 else edgeCount.toDouble() / vertices.size,
            maxInDegree = vertices.maxOfOrNull { inDegree(it) } ?: 0,
            maxOutDegree = vertices.maxOfOrNull { outDegree(it) } ?: 0,
            isDAG = !hasCycle(),
            componentCount = connectedComponents().size
        )
    }

    fun getVertices(): Set<V> = vertices.toSet()
    fun getAllEdges(): List<Edge<V>> = adjacency.values.flatten()
    fun hasVertex(vertex: V): Boolean = vertex in vertices
    fun hasEdge(from: V, to: V): Boolean = adjacency[from]?.any { it.to == to } == true
}
