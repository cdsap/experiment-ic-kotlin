package com.awesomeapp.app.java;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

public class QueryBuilder5676 {

    public enum JoinType { INNER, LEFT, RIGHT, FULL, CROSS }
    public enum SortDirection { ASC, DESC }
    public enum AggregateFunction { COUNT, SUM, AVG, MIN, MAX }

    public static class Column {
        private final String table;
        private final String name;
        private final String alias;

        public Column(String table, String name, String alias) {
            this.table = table; this.name = name; this.alias = alias;
        }

        public String toSql() {
            String col = table != null ? table + "." + name : name;
            return alias != null ? col + " AS " + alias : col;
        }
    }

    public static class Condition {
        private final String expression;
        private final List<Object> params;

        public Condition(String expression, Object... params) {
            this.expression = expression;
            this.params = params != null ? Arrays.asList(params) : Collections.emptyList();
        }

        public String getExpression() { return expression; }
        public List<Object> getParams() { return params; }
    }

    private final List<Column> selectColumns = new ArrayList<>();
    private String fromTable;
    private String fromAlias;
    private final List<String> joins = new ArrayList<>();
    private final List<Condition> whereConditions = new ArrayList<>();
    private final List<String> groupByColumns = new ArrayList<>();
    private final List<Condition> havingConditions = new ArrayList<>();
    private final List<String> orderByColumns = new ArrayList<>();
    private Integer limit;
    private Integer offset;
    private boolean distinct = false;

    public QueryBuilder5676 select(String... columns) {
        for (String c : columns) selectColumns.add(new Column(null, c, null));
        return this;
    }

    public QueryBuilder5676 select(String table, String column, String alias) {
        selectColumns.add(new Column(table, column, alias));
        return this;
    }

    public QueryBuilder5676 selectAggregate(AggregateFunction fn, String column, String alias) {
        selectColumns.add(new Column(null, fn.name() + "(" + column + ")", alias));
        return this;
    }

    public QueryBuilder5676 distinct() { this.distinct = true; return this; }

    public QueryBuilder5676 from(String table) { this.fromTable = table; return this; }

    public QueryBuilder5676 from(String table, String alias) {
        this.fromTable = table; this.fromAlias = alias; return this;
    }

    public QueryBuilder5676 join(JoinType type, String table, String onCondition) {
        joins.add(type.name().replace('_', ' ') + " JOIN " + table + " ON " + onCondition);
        return this;
    }

    public QueryBuilder5676 where(String expression, Object... params) {
        whereConditions.add(new Condition(expression, params));
        return this;
    }

    public QueryBuilder5676 groupBy(String... columns) {
        groupByColumns.addAll(Arrays.asList(columns));
        return this;
    }

    public QueryBuilder5676 having(String expression, Object... params) {
        havingConditions.add(new Condition(expression, params));
        return this;
    }

    public QueryBuilder5676 orderBy(String column, SortDirection dir) {
        orderByColumns.add(column + " " + dir.name());
        return this;
    }

    public QueryBuilder5676 limit(int limit) { this.limit = limit; return this; }
    public QueryBuilder5676 offset(int offset) { this.offset = offset; return this; }

    public String build() {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT ");
        if (distinct) sb.append("DISTINCT ");
        if (selectColumns.isEmpty()) {
            sb.append("*");
        } else {
            StringJoiner sj = new StringJoiner(", ");
            selectColumns.forEach(c -> sj.add(c.toSql()));
            sb.append(sj);
        }
        sb.append(" FROM ").append(fromTable);
        if (fromAlias != null) sb.append(" ").append(fromAlias);
        joins.forEach(j -> sb.append(" ").append(j));
        if (!whereConditions.isEmpty()) {
            sb.append(" WHERE ");
            StringJoiner wj = new StringJoiner(" AND ");
            whereConditions.forEach(c -> wj.add(c.getExpression()));
            sb.append(wj);
        }
        if (!groupByColumns.isEmpty()) sb.append(" GROUP BY ").append(String.join(", ", groupByColumns));
        if (!havingConditions.isEmpty()) {
            sb.append(" HAVING ");
            StringJoiner hj = new StringJoiner(" AND ");
            havingConditions.forEach(c -> hj.add(c.getExpression()));
            sb.append(hj);
        }
        if (!orderByColumns.isEmpty()) sb.append(" ORDER BY ").append(String.join(", ", orderByColumns));
        if (limit != null) sb.append(" LIMIT ").append(limit);
        if (offset != null) sb.append(" OFFSET ").append(offset);
        return sb.toString();
    }

    public List<Object> getParameters() {
        List<Object> params = new ArrayList<>();
        whereConditions.forEach(c -> params.addAll(c.getParams()));
        havingConditions.forEach(c -> params.addAll(c.getParams()));
        return params;
    }
}
