package com.awesomeapp.post.extra

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicLong

class MiddlewareChain288<Req : Any, Res : Any> {

    data class Context<Req, Res>(
        val request: Req,
        var response: Res? = null,
        val attributes: MutableMap<String, Any> = mutableMapOf(),
        val startTime: Long = System.nanoTime(),
        var aborted: Boolean = false,
        var abortReason: String? = null
    ) {
        inline fun <reified T> getAttribute(key: String): T? = attributes[key] as? T
        fun setAttribute(key: String, value: Any) { attributes[key] = value }
        fun abort(reason: String) { aborted = true; abortReason = reason }
        val elapsedMs: Long get() = (System.nanoTime() - startTime) / 1_000_000
    }

    interface Middleware<Req, Res> {
        val name: String
        val order: Int get() = 0
        suspend fun handle(context: Context<Req, Res>, next: suspend (Context<Req, Res>) -> Unit)
    }

    data class MiddlewareStats(
        val name: String,
        val invocations: AtomicLong = AtomicLong(0),
        val totalTimeNs: AtomicLong = AtomicLong(0),
        val errors: AtomicLong = AtomicLong(0)
    ) {
        val avgTimeMs: Double get() {
            val count = invocations.get()
            return if (count == 0L) 0.0 else totalTimeNs.get().toDouble() / count / 1_000_000
        }
    }

    data class ChainResult<Req, Res>(
        val context: Context<Req, Res>,
        val success: Boolean,
        val error: Throwable? = null,
        val durationMs: Long
    )

    private val middlewares = mutableListOf<Middleware<Req, Res>>()
    private val stats = mutableMapOf<String, MiddlewareStats>()
    private val mutex = Mutex()
    private val totalProcessed = AtomicLong(0)
    private val totalErrors = AtomicLong(0)
    private var errorHandler: suspend (Throwable, Context<Req, Res>) -> Unit = { _, _ -> }

    fun use(middleware: Middleware<Req, Res>): MiddlewareChain288<Req, Res> {
        middlewares.add(middleware)
        middlewares.sortBy { it.order }
        stats[middleware.name] = MiddlewareStats(middleware.name)
        return this
    }

    fun onError(handler: suspend (Throwable, Context<Req, Res>) -> Unit): MiddlewareChain288<Req, Res> {
        this.errorHandler = handler
        return this
    }

    suspend fun execute(request: Req): ChainResult<Req, Res> {
        val context = Context<Req, Res>(request)
        val startTime = System.nanoTime()
        totalProcessed.incrementAndGet()

        try {
            executeMiddleware(0, context)
            val duration = (System.nanoTime() - startTime) / 1_000_000
            return ChainResult(context, !context.aborted, durationMs = duration)
        } catch (e: Throwable) {
            totalErrors.incrementAndGet()
            errorHandler(e, context)
            val duration = (System.nanoTime() - startTime) / 1_000_000
            return ChainResult(context, false, e, duration)
        }
    }

    private suspend fun executeMiddleware(index: Int, context: Context<Req, Res>) {
        if (index >= middlewares.size || context.aborted) return
        val middleware = middlewares[index]
        val mwStats = stats[middleware.name]
        val start = System.nanoTime()

        try {
            middleware.handle(context) { ctx -> executeMiddleware(index + 1, ctx) }
            mwStats?.invocations?.incrementAndGet()
            mwStats?.totalTimeNs?.addAndGet(System.nanoTime() - start)
        } catch (e: Throwable) {
            mwStats?.errors?.incrementAndGet()
            throw e
        }
    }

    suspend fun executeBatch(requests: List<Req>): List<ChainResult<Req, Res>> =
        requests.map { execute(it) }

    fun getMiddlewareStats(): Map<String, MiddlewareStats> = stats.toMap()
    fun getTotalProcessed(): Long = totalProcessed.get()
    fun getTotalErrors(): Long = totalErrors.get()
    fun getMiddlewareCount(): Int = middlewares.size
    fun getMiddlewareNames(): List<String> = middlewares.map { it.name }
}
