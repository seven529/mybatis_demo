/**
 * Copyright (c) 2022-2023, Mybatis-Flex (fuhai999@gmail.com).
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mybatisflex.core.dialect;

import com.mybatisflex.core.FlexConsts;
import com.mybatisflex.core.exception.FlexExceptions;
import com.mybatisflex.core.query.*;
import com.mybatisflex.core.row.Row;
import com.mybatisflex.core.table.TableInfo;
import com.mybatisflex.core.util.ArrayUtil;
import com.mybatisflex.core.util.CollectionUtil;
import com.mybatisflex.core.util.StringUtil;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

/**
 * 通用的方言设计，其他方言可以继承于当前 CommonsDialectImpl
 * 创建或获取方言请参考 {@link com.mybatisflex.core.dialect.DialectFactory}
 */
public class CommonsDialectImpl implements IDialect {

    protected KeywordWrap keywordWrap = KeywordWrap.BACKQUOTE;
    private LimitOffsetProcesser limitOffsetProcesser = LimitOffsetProcesser.MYSQL;

    public CommonsDialectImpl() {
    }

    public CommonsDialectImpl(LimitOffsetProcesser limitOffsetProcesser) {
        this.limitOffsetProcesser = limitOffsetProcesser;
    }

    public CommonsDialectImpl(KeywordWrap keywordWrap, LimitOffsetProcesser limitOffsetProcesser) {
        this.keywordWrap = keywordWrap;
        this.limitOffsetProcesser = limitOffsetProcesser;
    }

    @Override
    public String wrap(String keyword) {
        return keywordWrap.wrap(keyword);
    }

    @Override
    public String forInsertRow(String tableName, Row row) {
        StringBuilder fields = new StringBuilder();
        StringBuilder questions = new StringBuilder();

        Set<String> attrs = row.obtainModifyAttrs();
        int index = 0;
        for (String attr : attrs) {
            fields.append(wrap(attr));
            questions.append("?");
            if (index != attrs.size() - 1) {
                fields.append(", ");
                questions.append(", ");
            }
            index++;
        }

        String sql = "INSERT INTO " + wrap(tableName) +
                "(" + fields + ")  VALUES " +
                "(" + questions + ")";
        return sql;
    }


    @Override
    public String forInsertBatchWithFirstRowColumns(String tableName, List<Row> rows) {
        StringBuilder fields = new StringBuilder();
        StringBuilder questions = new StringBuilder();

        Row firstRow = rows.get(0);
        Set<String> attrs = firstRow.obtainModifyAttrs();
        int index = 0;
        for (String attr : attrs) {
            fields.append(wrap(attr));
            if (index != attrs.size() - 1) {
                fields.append(", ");
            }
            index++;
        }

        for (int i = 0; i < rows.size(); i++) {
            questions.append(buildQuestion(attrs.size(), true));
            if (i != rows.size() - 1) {
                questions.append(",");
            }
        }

        String sql = "INSERT INTO " + wrap(tableName) +
                "(" + fields + ")  VALUES " + questions;
        return sql;
    }


    @Override
    public String forDeleteById(String tableName, String[] primaryKeys) {
        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM ");
        sql.append(wrap(tableName));
        sql.append(" WHERE ");
        for (int i = 0; i < primaryKeys.length; i++) {
            if (i > 0) {
                sql.append(" AND ");
            }
            sql.append(wrap(primaryKeys[i])).append(" = ?");
        }
        return sql.toString();
    }


    @Override
    public String forDeleteBatchByIds(String tableName, String[] primaryKeys, Object[] ids) {
        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM ");
        sql.append(wrap(tableName));
        sql.append(" WHERE ");

        //多主键的场景
        if (primaryKeys.length > 1) {
            for (int i = 0; i < ids.length / primaryKeys.length; i++) {
                if (i > 0) {
                    sql.append(" OR ");
                }
                sql.append("(");
                for (int j = 0; j < primaryKeys.length; j++) {
                    if (j > 0) {
                        sql.append(" AND ");
                    }
                    sql.append(wrap(primaryKeys[j])).append(" = ?");
                }
                sql.append(")");
            }
        }
        // 单主键
        else {
            for (int i = 0; i < ids.length; i++) {
                if (i > 0) {
                    sql.append(" OR ");
                }
                sql.append(wrap(primaryKeys[0])).append(" = ?");
            }
        }
        return sql.toString();
    }

    @Override
    public String forDeleteByQuery(QueryWrapper queryWrapper) {
        return buildDeleteSql(queryWrapper);
    }

    @Override
    public String forUpdateById(String tableName, Row row) {
        StringBuilder sql = new StringBuilder();

        Set<String> modifyAttrs = row.obtainModifyAttrs();
        String[] primaryKeys = row.obtainsPrimaryKeyStrings();

        sql.append("UPDATE ").append(wrap(tableName)).append(" SET ");
        int index = 0;
        for (Map.Entry<String, Object> e : row.entrySet()) {
            String colName = e.getKey();
            if (modifyAttrs.contains(colName) && !ArrayUtil.contains(primaryKeys, colName)) {
                if (index > 0) {
                    sql.append(", ");
                }
                sql.append(wrap(colName)).append(" = ? ");
                index++;
            }
        }
        sql.append(" WHERE ");
        for (int i = 0; i < primaryKeys.length; i++) {
            if (i > 0) {
                sql.append(" AND ");
            }
            sql.append(wrap(primaryKeys[i])).append(" = ?");
        }

        return sql.toString();
    }

    @Override
    public String forUpdateByQuery(String tableName, Row row, QueryWrapper queryWrapper) {
        StringBuilder sql = new StringBuilder();

        Set<String> modifyAttrs = row.obtainModifyAttrs();

        sql.append("UPDATE ").append(wrap(tableName)).append(" SET ");
        int index = 0;
        for (String modifyAttr : modifyAttrs) {
            if (index > 0) {
                sql.append(", ");
            }
            sql.append(wrap(modifyAttr)).append(" = ? ");
            index++;
        }

        String whereConditionSql = buildWhereConditionSql(queryWrapper);
        if (StringUtil.isNotBlank(whereConditionSql)) {
            sql.append(" WHERE ").append(whereConditionSql);
        }

        return sql.toString();
    }

    @Override
    public String forUpdateBatchById(String tableName, List<Row> rows) {
        if (rows.size() == 1) {
            return forUpdateById(tableName, rows.get(0));
        }
        StringBuilder sql = new StringBuilder();
        for (Row row : rows) {
            sql.append(forUpdateById(tableName, row)).append("; ");
        }
        return sql.toString();
    }


    @Override
    public String forSelectOneById(String tableName, String[] primaryKeys, Object[] primaryValues) {
        StringBuilder sql = new StringBuilder("SELECT * FROM ");
        sql.append(wrap(tableName)).append(" WHERE ");
        for (int i = 0; i < primaryKeys.length; i++) {
            if (i > 0) {
                sql.append(" AND ");
            }
            sql.append(wrap(primaryKeys[i])).append(" = ?");
        }
        return sql.toString();
    }

    @Override
    public String forSelectListByQuery(QueryWrapper queryWrapper) {
        return buildSelectSql(queryWrapper);
    }


    @Override
    public String forSelectCountByQuery(QueryWrapper queryWrapper) {
        return buildSelectCountSql(queryWrapper);
    }


    ////////////build query sql///////
    @Override
    public String buildSelectSql(QueryWrapper queryWrapper) {
        List<QueryTable> queryTables = CPI.getQueryTables(queryWrapper);
        List<QueryTable> joinTables = CPI.getJoinTables(queryWrapper);
        List<QueryTable> allTables = CollectionUtil.merge(queryTables, joinTables);

        List<QueryColumn> selectColumns = CPI.getSelectColumns(queryWrapper);

        StringBuilder sqlBuilder = new StringBuilder("SELECT ");
        if (selectColumns == null || selectColumns.isEmpty()) {
            sqlBuilder.append("*");
        } else {
            int index = 0;

            for (QueryColumn selectColumn : selectColumns) {
                String selectColumnSql = CPI.toSelectSql(selectColumn, allTables, this);
                sqlBuilder.append(selectColumnSql);
                if (index != selectColumns.size() - 1) {
                    sqlBuilder.append(", ");
                }
                index++;
            }
        }
        sqlBuilder.append(" FROM ").append(StringUtil.join(", ", queryTables, queryTable -> queryTable.toSql(this)));

        buildJoinSql(sqlBuilder, queryWrapper, allTables);
        buildWhereSql(sqlBuilder, queryWrapper, allTables);
        buildGroupBySql(sqlBuilder, queryWrapper, allTables);
        buildHavingSql(sqlBuilder, queryWrapper, allTables);
        buildOrderBySql(sqlBuilder, queryWrapper, allTables);

        Integer limitRows = CPI.getLimitRows(queryWrapper);
        Integer limitOffset = CPI.getLimitOffset(queryWrapper);
        if (limitRows != null || limitOffset != null) {
            sqlBuilder = buildLimitOffsetSql(sqlBuilder, queryWrapper, limitRows, limitOffset);
        }

        return sqlBuilder.toString();
    }


    @Override
    public String buildSelectCountSql(QueryWrapper queryWrapper) {
        List<QueryTable> queryTables = CPI.getQueryTables(queryWrapper);
        List<QueryTable> joinTables = CPI.getJoinTables(queryWrapper);
        List<QueryTable> allTables = CollectionUtil.merge(queryTables, joinTables);

        //ignore selectColumns
        StringBuilder sqlBuilder = new StringBuilder("SELECT COUNT(*) FROM ");
        sqlBuilder.append(StringUtil.join(", ", queryTables, queryTable -> queryTable.toSql(this)));


        buildJoinSql(sqlBuilder, queryWrapper, allTables);
        buildWhereSql(sqlBuilder, queryWrapper, allTables);
        buildGroupBySql(sqlBuilder, queryWrapper, allTables);
        buildHavingSql(sqlBuilder, queryWrapper, allTables);

        // ignore orderBy and limit
        // buildOrderBySql(sqlBuilder, queryWrapper);
        // buildLimitSql(sqlBuilder, queryWrapper);

        return sqlBuilder.toString();
    }

    @Override
    public String buildDeleteSql(QueryWrapper queryWrapper) {
        List<QueryTable> queryTables = CPI.getQueryTables(queryWrapper);
        List<QueryTable> joinTables = CPI.getJoinTables(queryWrapper);
        List<QueryTable> allTables = CollectionUtil.merge(queryTables, joinTables);

        //ignore selectColumns
        StringBuilder sqlBuilder = new StringBuilder("DELETE FROM ");
        sqlBuilder.append(StringUtil.join(", ", queryTables, queryTable -> queryTable.toSql(this)));

        buildJoinSql(sqlBuilder, queryWrapper, allTables);
        buildWhereSql(sqlBuilder, queryWrapper, allTables);
        buildGroupBySql(sqlBuilder, queryWrapper, allTables);
        buildHavingSql(sqlBuilder, queryWrapper, allTables);

        //ignore orderBy and limit
        //buildOrderBySql(sqlBuilder, queryWrapper);
        //buildLimitSql(sqlBuilder, queryWrapper);

        return sqlBuilder.toString();
    }

    @Override
    public String buildWhereConditionSql(QueryWrapper queryWrapper) {
        QueryCondition whereQueryCondition = CPI.getWhereQueryCondition(queryWrapper);
        return whereQueryCondition != null ? whereQueryCondition.toSql(CPI.getQueryTables(queryWrapper), this) : "";
    }

    @Override
    public String forInsertEntity(TableInfo tableInfo, Object entity) {
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(wrap(tableInfo.getTableName()));

        String[] insertColumns = tableInfo.obtainInsertColumns();
        Map<String, String> onInsertColumns = tableInfo.getOnInsertColumns();

        StringJoiner sqlFields = new StringJoiner(", ");
        StringJoiner sqlValues = new StringJoiner(", ");

        for (String insertColumn : insertColumns) {
            sqlFields.add(wrap(insertColumn));
            if (onInsertColumns != null && onInsertColumns.containsKey(insertColumn)) {
                sqlValues.add(onInsertColumns.get(insertColumn));
            } else {
                sqlValues.add("?");
            }
        }

        return sql.append("(").append(sqlFields).append(")")
                .append(" VALUES ")
                .append("(").append(sqlValues).append(")").toString();
    }

    @Override
    public String forInsertEntityBatch(TableInfo tableInfo, List<Object> entities) {
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(wrap(tableInfo.getTableName()));
        String[] insertColumns = tableInfo.obtainInsertColumns();
        String[] warpedInsertColumns = new String[insertColumns.length];
        for (int i = 0; i < insertColumns.length; i++) {
            warpedInsertColumns[i] = wrap(insertColumns[i]);
        }
        sql.append("(").append(StringUtil.join(", ", warpedInsertColumns)).append(")");
        sql.append(" VALUES ");

        Map<String, String> onInsertColumns = tableInfo.getOnInsertColumns();
        for (int i = 0; i < entities.size(); i++) {
            StringJoiner stringJoiner = new StringJoiner(", ", "(", ")");
            for (String insertColumn : warpedInsertColumns) {
                if (onInsertColumns != null && onInsertColumns.containsKey(insertColumn)) {
                    //直接读取 onInsert 配置的值，而不用 "?" 代替
                    stringJoiner.add(onInsertColumns.get(insertColumn));
                } else {
                    stringJoiner.add("?");
                }
            }
            sql.append(stringJoiner);
            if (i != entities.size() - 1) {
                sql.append(", ");
            }
        }

        return sql.toString();
    }

    @Override
    public String forDeleteEntityById(TableInfo tableInfo) {
        String logicDeleteColumn = tableInfo.getLogicDeleteColumn();

        //正常删除
        if (StringUtil.isBlank(logicDeleteColumn)) {
            return forDeleteById(tableInfo.getTableName(), tableInfo.getPrimaryKeys());
        }

        //逻辑删除
        StringBuilder sql = new StringBuilder();
        String[] primaryKeys = tableInfo.getPrimaryKeys();

        sql.append("UPDATE ").append(wrap(tableInfo.getTableName())).append(" SET ");
        sql.append(wrap(logicDeleteColumn)).append(" = ").append(FlexConsts.DEL_STATUS_DELETED);
        sql.append(" WHERE ");
        for (int i = 0; i < primaryKeys.length; i++) {
            if (i > 0) {
                sql.append(" AND ");
            }
            sql.append(wrap(primaryKeys[i])).append(" = ?");
        }

        sql.append(" AND ").append(wrap(logicDeleteColumn)).append(" = ").append(FlexConsts.DEL_STATUS_NORMAL);

        return sql.toString();
    }

    @Override
    public String forDeleteEntityBatchByIds(TableInfo tableInfo, Object[] primaryValues) {
        String logicDeleteColumn = tableInfo.getLogicDeleteColumn();

        //正常删除
        if (StringUtil.isBlank(logicDeleteColumn)) {
            return forDeleteBatchByIds(tableInfo.getTableName(), tableInfo.getPrimaryKeys(), primaryValues);
        }

        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ");
        sql.append(wrap(tableInfo.getTableName()));
        sql.append(" SET ").append(wrap(logicDeleteColumn)).append(" = ").append(FlexConsts.DEL_STATUS_DELETED);
        sql.append(" WHERE ");
        sql.append("(");

        String[] primaryKeys = tableInfo.getPrimaryKeys();

        //多主键的场景
        if (primaryKeys.length > 1) {
            for (int i = 0; i < primaryValues.length / primaryKeys.length; i++) {
                if (i > 0) {
                    sql.append(" OR ");
                }
                sql.append("(");
                for (int j = 0; j < primaryKeys.length; j++) {
                    if (j > 0) {
                        sql.append(" AND ");
                    }
                    sql.append(wrap(primaryKeys[j])).append(" = ?");
                }
                sql.append(")");
            }
        }
        // 单主键
        else {
            for (int i = 0; i < primaryValues.length; i++) {
                if (i > 0) {
                    sql.append(" OR ");
                }
                sql.append(wrap(primaryKeys[0])).append(" = ?");
            }
        }

        if (StringUtil.isNotBlank(logicDeleteColumn)) {
            sql.append(") AND ").append(wrap(logicDeleteColumn)).append(" = ").append(FlexConsts.DEL_STATUS_NORMAL);
        }
        return sql.toString();
    }

    @Override
    public String forDeleteEntityBatchByQuery(TableInfo tableInfo, QueryWrapper queryWrapper) {

        String logicDeleteColumn = tableInfo.getLogicDeleteColumn();

        //正常删除
        if (StringUtil.isBlank(logicDeleteColumn)) {
            return forDeleteByQuery(queryWrapper);
        }


        //逻辑删除
        List<QueryTable> queryTables = CPI.getQueryTables(queryWrapper);
        List<QueryTable> joinTables = CPI.getJoinTables(queryWrapper);
        List<QueryTable> allTables = CollectionUtil.merge(queryTables, joinTables);

        //ignore selectColumns
        StringBuilder sqlBuilder = new StringBuilder("UPDATE ");
        sqlBuilder.append(wrap(tableInfo.getTableName()));
        sqlBuilder.append(" SET ").append(wrap(logicDeleteColumn)).append(" = ").append(FlexConsts.DEL_STATUS_DELETED);


        buildJoinSql(sqlBuilder, queryWrapper, allTables);
        buildWhereSql(sqlBuilder, queryWrapper, allTables);
        buildGroupBySql(sqlBuilder, queryWrapper, allTables);
        buildHavingSql(sqlBuilder, queryWrapper, allTables);

        //ignore orderBy and limit
        //buildOrderBySql(sqlBuilder, queryWrapper);
        //buildLimitSql(sqlBuilder, queryWrapper);

        return sqlBuilder.toString();
    }


    @Override
    public String forUpdateEntity(TableInfo tableInfo, Object entity, boolean ignoreNulls) {
        StringBuilder sql = new StringBuilder();

        Set<String> modifyAttrs = tableInfo.obtainUpdateColumns(entity, ignoreNulls, false);
        String[] primaryKeys = tableInfo.getPrimaryKeys();

        sql.append("UPDATE ").append(wrap(tableInfo.getTableName())).append(" SET ");

        StringJoiner stringJoiner = new StringJoiner(", ");

        for (String modifyAttr : modifyAttrs) {
            stringJoiner.add(wrap(modifyAttr) + " = ?");
        }

        Map<String, String> onUpdateColumns = tableInfo.getOnUpdateColumns();
        if (onUpdateColumns != null && !onUpdateColumns.isEmpty()) {
            onUpdateColumns.forEach((column, value) -> stringJoiner.add(wrap(column) + " = " + value));
        }

        //乐观锁字段
        String versionColumn = tableInfo.getVersionColumn();
        if (StringUtil.isNotBlank(versionColumn)) {
            stringJoiner.add(wrap(versionColumn) + " = " + wrap(versionColumn) + " + 1 ");
        }

        sql.append(stringJoiner);

        sql.append(" WHERE ");
        for (int i = 0; i < primaryKeys.length; i++) {
            if (i > 0) {
                sql.append(" AND ");
            }
            sql.append(wrap(primaryKeys[i])).append(" = ?");
        }

        //逻辑删除条件，已删除的数据不能被修改
        String logicDeleteColumn = tableInfo.getLogicDeleteColumn();
        if (StringUtil.isNotBlank(logicDeleteColumn)) {
            sql.append(" AND ").append(wrap(logicDeleteColumn)).append(" = ").append(FlexConsts.DEL_STATUS_NORMAL);
        }

        //乐观锁条件
        if (StringUtil.isNotBlank(versionColumn)) {
            Object versionValue = tableInfo.buildColumnSqlArg(entity, versionColumn);
            if (versionValue == null) {
                throw FlexExceptions.wrap("The version value of entity[%s] must not be null.", entity);
            }
            sql.append(" AND ").append(wrap(versionColumn)).append(" = ").append(versionValue);
        }


        return sql.toString();
    }

    @Override
    public String forUpdateEntityByQuery(TableInfo tableInfo, Object entity, boolean ignoreNulls, QueryWrapper queryWrapper) {
        StringBuilder sql = new StringBuilder();

        Set<String> modifyAttrs = tableInfo.obtainUpdateColumns(entity, ignoreNulls, true);

        sql.append("UPDATE ").append(wrap(tableInfo.getTableName())).append(" SET ");

        StringJoiner stringJoiner = new StringJoiner(", ");

        for (String modifyAttr : modifyAttrs) {
            stringJoiner.add(wrap(modifyAttr) + " = ?");
        }

        Map<String, String> onUpdateColumns = tableInfo.getOnUpdateColumns();
        if (onUpdateColumns != null && !onUpdateColumns.isEmpty()) {
            onUpdateColumns.forEach((column, value) -> stringJoiner.add(wrap(column) + " = " + value));
        }

        //乐观锁字段
        String versionColumn = tableInfo.getVersionColumn();
        if (StringUtil.isNotBlank(versionColumn)) {
            stringJoiner.add(wrap(versionColumn) + " = " + wrap(versionColumn) + " + 1 ");
        }

        sql.append(stringJoiner);


        sql.append(" WHERE ");

        //乐观锁条件
        if (StringUtil.isNotBlank(versionColumn)) {
            Object versionValue = tableInfo.buildColumnSqlArg(entity, versionColumn);
            if (versionValue == null) {
                throw FlexExceptions.wrap("The version value of entity[%s] must not be null.", entity);
            }
            queryWrapper.and(new StringQueryCondition(wrap(versionColumn) + " = " + versionValue));
        }

        //逻辑删除条件，已删除的数据不能被修改
        String logicDeleteColumn = tableInfo.getLogicDeleteColumn();
        if (StringUtil.isNotBlank(logicDeleteColumn)) {
            queryWrapper.and(new StringQueryCondition(wrap(logicDeleteColumn) + " = " + FlexConsts.DEL_STATUS_NORMAL));
        }

        sql.append(buildWhereConditionSql(queryWrapper));

        return sql.toString();
    }

    @Override
    public String forSelectOneEntityById(TableInfo tableInfo) {
        StringBuilder sql = new StringBuilder("SELECT * FROM ");
        sql.append(wrap(tableInfo.getTableName()));
        sql.append(" WHERE ");
        String[] pKeys = tableInfo.getPrimaryKeys();
        for (int i = 0; i < pKeys.length; i++) {
            if (i > 0) {
                sql.append(" AND ");
            }
            sql.append(wrap(pKeys[i])).append(" = ?");
        }

        //逻辑删除的情况下，需要添加逻辑删除的条件
        String logicDeleteColumn = tableInfo.getLogicDeleteColumn();
        if (StringUtil.isNotBlank(logicDeleteColumn)) {
            sql.append(" AND ").append(wrap(logicDeleteColumn)).append(" = ").append(FlexConsts.DEL_STATUS_NORMAL);
        }

        return sql.toString();
    }


    @Override
    public String forSelectEntityListByIds(TableInfo tableInfo, Object[] primaryValues) {
        StringBuilder sql = new StringBuilder("SELECT * FROM ");
        sql.append(wrap(tableInfo.getTableName()));
        sql.append(" WHERE ");
        String[] primaryKeys = tableInfo.getPrimaryKeys();

        String logicDeleteColumn = tableInfo.getLogicDeleteColumn();
        if (StringUtil.isNotBlank(logicDeleteColumn)) {
            sql.append(wrap(logicDeleteColumn)).append(" = ").append(FlexConsts.DEL_STATUS_NORMAL);
            sql.append(" AND (");
        }

        //多主键的场景
        if (primaryKeys.length > 1) {
            for (int i = 0; i < primaryValues.length / primaryKeys.length; i++) {
                if (i > 0) {
                    sql.append(" OR ");
                }
                sql.append("(");
                for (int j = 0; j < primaryKeys.length; j++) {
                    if (j > 0) {
                        sql.append(" AND ");
                    }
                    sql.append(wrap(primaryKeys[j])).append(" = ?");
                }
                sql.append(")");
            }
        }
        // 单主键
        else {
            for (int i = 0; i < primaryValues.length; i++) {
                if (i > 0) {
                    sql.append(" OR ");
                }
                sql.append(wrap(primaryKeys[0])).append(" = ?");
            }
        }

        if (StringUtil.isNotBlank(logicDeleteColumn)) {
            sql.append(wrap(logicDeleteColumn)).append(" )");
        }

        return sql.toString();
    }


    protected void buildJoinSql(StringBuilder sqlBuilder, QueryWrapper queryWrapper, List<QueryTable> queryTables) {
        List<Join> joins = CPI.getJoins(queryWrapper);
        if (joins != null && !joins.isEmpty()) {
            for (Join join : joins) {
                if (!join.checkEffective()) {
                    continue;
                }
                sqlBuilder.append(join.toSql(queryTables, this));
            }
        }
    }


    protected void buildWhereSql(StringBuilder sqlBuilder, QueryWrapper queryWrapper, List<QueryTable> queryTables) {
        QueryCondition whereQueryCondition = CPI.getWhereQueryCondition(queryWrapper);
        if (whereQueryCondition != null) {
            String whereSql = whereQueryCondition.toSql(queryTables, this);
            if (StringUtil.isNotBlank(whereSql)) {
                sqlBuilder.append(" WHERE ").append(whereSql);
            }
        }
    }


    protected void buildGroupBySql(StringBuilder sqlBuilder, QueryWrapper queryWrapper, List<QueryTable> queryTables) {
        List<QueryColumn> groupByColumns = CPI.getGroupByColumns(queryWrapper);
        if (groupByColumns != null && !groupByColumns.isEmpty()) {
            sqlBuilder.append(" GROUP BY ");
            int index = 0;
            for (QueryColumn groupByColumn : groupByColumns) {
                String groupBy = CPI.toConditionSql(groupByColumn, queryTables, this);
                sqlBuilder.append(groupBy);
                if (index != groupByColumns.size() - 1) {
                    sqlBuilder.append(", ");
                }
                index++;
            }
        }
    }


    protected void buildHavingSql(StringBuilder sqlBuilder, QueryWrapper queryWrapper, List<QueryTable> queryTables) {
        QueryCondition havingQueryCondition = CPI.getHavingQueryCondition(queryWrapper);
        if (havingQueryCondition != null) {
            String havingSql = havingQueryCondition.toSql(queryTables, this);
            if (StringUtil.isNotBlank(havingSql)) {
                sqlBuilder.append(" HAVING ").append(havingSql);
            }
        }
    }


    protected void buildOrderBySql(StringBuilder sqlBuilder, QueryWrapper queryWrapper, List<QueryTable> queryTables) {
        List<QueryOrderBy> orderBys = CPI.getOrderBys(queryWrapper);
        if (orderBys != null && !orderBys.isEmpty()) {
            sqlBuilder.append(" ORDER BY ");
            int index = 0;
            for (QueryOrderBy orderBy : orderBys) {
                sqlBuilder.append(orderBy.toSql(queryTables, this));
                if (index != orderBys.size() - 1) {
                    sqlBuilder.append(", ");
                }
                index++;
            }
        }
    }


    /**
     * 构建 limit 和 offset 的参数
     */
    protected StringBuilder buildLimitOffsetSql(StringBuilder sqlBuilder, QueryWrapper queryWrapper, Integer limitRows, Integer limitOffset) {
        return limitOffsetProcesser.process(sqlBuilder, queryWrapper, limitRows, limitOffset);
    }


    protected String buildQuestion(int count, boolean withBrackets) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append("?");
            if (i != count - 1) {
                sb.append(", ");
            }
        }
        return withBrackets ? "(" + sb + ")" : sb.toString();
    }


}
