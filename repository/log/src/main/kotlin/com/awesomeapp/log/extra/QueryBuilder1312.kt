package com.awesomeapp.log.extra

class QueryBuilder1312 {

    sealed class Expression {
        data class Field(val name: String) : Expression()
        data class Literal(val value: Any?) : Expression()
        data class Function(val name: String, val args: List<Expression>) : Expression()
        data class BinaryOp(val left: Expression, val op: String, val right: Expression) : Expression()
        data class UnaryOp(val op: String, val operand: Expression) : Expression()
        data class SubQuery(val query: QueryBuilder1312) : Expression()
    }

    sealed class Condition {
        data class Comparison(val field: String, val op: CompOp, val value: Any?) : Condition()
        data class Between(val field: String, val low: Any, val high: Any) : Condition()
        data class In(val field: String, val values: List<Any>) : Condition()
        data class IsNull(val field: String) : Condition()
        data class IsNotNull(val field: String) : Condition()
        data class Like(val field: String, val pattern: String) : Condition()
        data class And(val conditions: List<Condition>) : Condition()
        data class Or(val conditions: List<Condition>) : Condition()
        data class Not(val condition: Condition) : Condition()
        data class Exists(val subQuery: QueryBuilder1312) : Condition()
        data class Raw(val sql: String) : Condition()
    }

    enum class CompOp(val symbol: String) {
        EQ("="), NE("!="), GT(">"), GE(">="), LT("<"), LE("<=")
    }

    enum class JoinType { INNER, LEFT, RIGHT, FULL, CROSS }
    enum class OrderDir { ASC, DESC }

    data class Join(val type: JoinType, val table: String, val alias: String?, val on: Condition)
    data class OrderBy(val field: String, val direction: OrderDir)
    data class GroupBy(val fields: List<String>, val having: Condition? = null)

    private var table: String = ""
    private var tableAlias: String? = null
    private val selectFields = mutableListOf<Pair<Expression, String?>>()
    private val conditions = mutableListOf<Condition>()
    private val joins = mutableListOf<Join>()
    private val orderBys = mutableListOf<OrderBy>()
    private var groupBy: GroupBy? = null
    private var limitValue: Int? = null
    private var offsetValue: Int? = null
    private var distinct = false
    private val parameters = mutableListOf<Any?>()

    fun from(table: String, alias: String? = null): QueryBuilder1312 {
        this.table = table; this.tableAlias = alias; return this
    }

    fun select(vararg fields: String): QueryBuilder1312 {
        fields.forEach { selectFields.add(Expression.Field(it) to null) }; return this
    }

    fun selectAs(field: String, alias: String): QueryBuilder1312 {
        selectFields.add(Expression.Field(field) to alias); return this
    }

    fun selectExpr(expr: Expression, alias: String? = null): QueryBuilder1312 {
        selectFields.add(expr to alias); return this
    }

    fun distinct(): QueryBuilder1312 { distinct = true; return this }

    fun where(condition: Condition): QueryBuilder1312 { conditions.add(condition); return this }
    fun whereEq(field: String, value: Any?): QueryBuilder1312 { conditions.add(Condition.Comparison(field, CompOp.EQ, value)); return this }
    fun whereGt(field: String, value: Any): QueryBuilder1312 { conditions.add(Condition.Comparison(field, CompOp.GT, value)); return this }
    fun whereLt(field: String, value: Any): QueryBuilder1312 { conditions.add(Condition.Comparison(field, CompOp.LT, value)); return this }
    fun whereBetween(field: String, low: Any, high: Any): QueryBuilder1312 { conditions.add(Condition.Between(field, low, high)); return this }
    fun whereIn(field: String, values: List<Any>): QueryBuilder1312 { conditions.add(Condition.In(field, values)); return this }
    fun whereNull(field: String): QueryBuilder1312 { conditions.add(Condition.IsNull(field)); return this }
    fun whereNotNull(field: String): QueryBuilder1312 { conditions.add(Condition.IsNotNull(field)); return this }
    fun whereLike(field: String, pattern: String): QueryBuilder1312 { conditions.add(Condition.Like(field, pattern)); return this }
    fun whereNot(condition: Condition): QueryBuilder1312 { conditions.add(Condition.Not(condition)); return this }
    fun whereExists(subQuery: QueryBuilder1312): QueryBuilder1312 { conditions.add(Condition.Exists(subQuery)); return this }

    fun join(table: String, on: Condition, type: JoinType = JoinType.INNER, alias: String? = null): QueryBuilder1312 {
        joins.add(Join(type, table, alias, on)); return this
    }

    fun leftJoin(table: String, on: Condition, alias: String? = null) = join(table, on, JoinType.LEFT, alias)
    fun rightJoin(table: String, on: Condition, alias: String? = null) = join(table, on, JoinType.RIGHT, alias)

    fun orderBy(field: String, direction: OrderDir = OrderDir.ASC): QueryBuilder1312 {
        orderBys.add(OrderBy(field, direction)); return this
    }

    fun groupBy(vararg fields: String, having: Condition? = null): QueryBuilder1312 {
        groupBy = GroupBy(fields.toList(), having); return this
    }

    fun limit(limit: Int): QueryBuilder1312 { limitValue = limit; return this }
    fun offset(offset: Int): QueryBuilder1312 { offsetValue = offset; return this }

    fun build(): String {
        val sb = StringBuilder()
        sb.append("SELECT ")
        if (distinct) sb.append("DISTINCT ")
        if (selectFields.isEmpty()) sb.append("*") else {
            sb.append(selectFields.joinToString(", ") { (expr, alias) ->
                val exprStr = renderExpression(expr)
                if (alias != null) "$exprStr AS $alias" else exprStr
            })
        }
        sb.append(" FROM $table")
        tableAlias?.let { sb.append(" AS $it") }

        joins.forEach { join ->
            sb.append(" ${join.type.name} JOIN ${join.table}")
            join.alias?.let { sb.append(" AS $it") }
            sb.append(" ON ${renderCondition(join.on)}")
        }

        if (conditions.isNotEmpty()) {
            sb.append(" WHERE ")
            sb.append(conditions.joinToString(" AND ") { renderCondition(it) })
        }

        groupBy?.let { gb ->
            sb.append(" GROUP BY ${gb.fields.joinToString(", ")}")
            gb.having?.let { sb.append(" HAVING ${renderCondition(it)}") }
        }

        if (orderBys.isNotEmpty()) {
            sb.append(" ORDER BY ")
            sb.append(orderBys.joinToString(", ") { "${it.field} ${it.direction.name}" })
        }

        limitValue?.let { sb.append(" LIMIT $it") }
        offsetValue?.let { sb.append(" OFFSET $it") }

        return sb.toString()
    }

    private fun renderExpression(expr: Expression): String = when (expr) {
        is Expression.Field -> expr.name
        is Expression.Literal -> renderLiteral(expr.value)
        is Expression.Function -> "${expr.name}(${expr.args.joinToString(", ") { renderExpression(it) }})"
        is Expression.BinaryOp -> "(${renderExpression(expr.left)} ${expr.op} ${renderExpression(expr.right)})"
        is Expression.UnaryOp -> "${expr.op}(${renderExpression(expr.operand)})"
        is Expression.SubQuery -> "(${expr.query.build()})"
    }

    private fun renderCondition(cond: Condition): String = when (cond) {
        is Condition.Comparison -> "${cond.field} ${cond.op.symbol} ${renderLiteral(cond.value)}"
        is Condition.Between -> "${cond.field} BETWEEN ${renderLiteral(cond.low)} AND ${renderLiteral(cond.high)}"
        is Condition.In -> "${cond.field} IN (${cond.values.joinToString(", ") { renderLiteral(it) }})"
        is Condition.IsNull -> "${cond.field} IS NULL"
        is Condition.IsNotNull -> "${cond.field} IS NOT NULL"
        is Condition.Like -> "${cond.field} LIKE '${cond.pattern}'"
        is Condition.And -> cond.conditions.joinToString(" AND ") { "(${renderCondition(it)})" }
        is Condition.Or -> cond.conditions.joinToString(" OR ") { "(${renderCondition(it)})" }
        is Condition.Not -> "NOT (${renderCondition(cond.condition)})"
        is Condition.Exists -> "EXISTS (${cond.subQuery.build()})"
        is Condition.Raw -> cond.sql
    }

    private fun renderLiteral(value: Any?): String = when (value) {
        null -> "NULL"
        is String -> "'$value'"
        is Number -> value.toString()
        is Boolean -> if (value) "TRUE" else "FALSE"
        else -> "'$value'"
    }

    fun getParameters(): List<Any?> = parameters.toList()
}
