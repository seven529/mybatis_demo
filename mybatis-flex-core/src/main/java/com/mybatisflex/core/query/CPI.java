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
package com.mybatisflex.core.query;

import com.mybatisflex.core.dialect.IDialect;
import com.mybatisflex.core.util.CollectionUtil;

import java.util.List;

/**
 * Cross Package Invoke
 * 夸包调用工具类，这么设计的原因，是需要保证 QueryWrapper 方法对于用户的纯净性
 * 而 framework 又可以通过 CPI 来调用 QueryWrapper 的其他方法
 */

public class CPI {


    public static Object[] getValueArray(QueryWrapper queryWrapper) {
        return queryWrapper.getValueArray();
    }


    public static List<QueryTable> getQueryTables(QueryWrapper queryWrapper) {
        return queryWrapper.getQueryTables();
    }

    public static void setQueryTable(QueryWrapper queryWrapper, List<QueryTable> queryTables) {
        queryWrapper.setQueryTables(queryTables);
    }

    public static String getDatasource(QueryWrapper queryWrapper) {
        return queryWrapper.getDatasource();
    }

    public static void setDatasource(QueryWrapper queryWrapper, String datasource) {
        queryWrapper.setDatasource(datasource);
    }

    public static List<QueryColumn> getSelectColumns(QueryWrapper queryWrapper) {
        return queryWrapper.getSelectColumns();
    }

    public static void setSelectColumns(QueryWrapper queryWrapper, List<QueryColumn> selectColumns) {
        queryWrapper.setSelectColumns(selectColumns);
    }

    public static List<Join> getJoins(QueryWrapper queryWrapper) {
        return queryWrapper.getJoins();
    }

    public static void setJoins(QueryWrapper queryWrapper, List<Join> joins) {
        queryWrapper.setJoins(joins);
    }


    public static List<QueryTable> getJoinTables(QueryWrapper queryWrapper) {
        return queryWrapper.getJoinTables();
    }

    public static void setJoinTables(QueryWrapper queryWrapper,List<QueryTable> joinTables) {
        queryWrapper.setJoinTables(joinTables);
    }


    public static QueryCondition getWhereQueryCondition(QueryWrapper queryWrapper) {
        return queryWrapper.getWhereQueryCondition();
    }

    public static List<QueryColumn> getGroupByColumns(QueryWrapper queryWrapper) {
        return queryWrapper.getGroupByColumns();
    }

    public static void setGroupByColumns(QueryWrapper queryWrapper, List<QueryColumn> groupByColumns) {
        queryWrapper.setGroupByColumns(groupByColumns);
    }

    public static QueryCondition getHavingQueryCondition(QueryWrapper queryWrapper) {
        return queryWrapper.getHavingQueryCondition();
    }

    public static void setHavingQueryCondition(QueryWrapper queryWrapper, QueryCondition havingQueryCondition) {
        queryWrapper.setHavingQueryCondition(havingQueryCondition);
    }

    public static List<QueryOrderBy> getOrderBys(QueryWrapper queryWrapper) {
        return queryWrapper.getOrderBys();
    }

    public static void setOrderBys(QueryWrapper queryWrapper, List<QueryOrderBy> orderBys) {
        queryWrapper.setOrderBys(orderBys);
    }

    public static Integer getLimitOffset(QueryWrapper queryWrapper) {
        return queryWrapper.getLimitOffset();
    }

    public static void setLimitOffset(QueryWrapper queryWrapper, Integer limitOffset) {
        queryWrapper.setLimitOffset(limitOffset);
    }

    public static Integer getLimitRows(QueryWrapper queryWrapper) {
        return queryWrapper.getLimitRows();
    }

    public static void setLimitRows(QueryWrapper queryWrapper, Integer limitRows) {
        queryWrapper.setLimitRows(limitRows);
    }


    public static String toConditionSql(QueryColumn queryColumn,List<QueryTable> queryTables, IDialect dialect) {
        return queryColumn.toConditionSql(queryTables,dialect);
    }

    public static String toSelectSql(QueryColumn queryColumn,List<QueryTable> queryTables, IDialect dialect) {
        return queryColumn.toSelectSql(queryTables,dialect);
    }

    public static void setFromIfNecessary(QueryWrapper queryWrapper,String tableName){
        if (CollectionUtil.isEmpty(queryWrapper.getQueryTables())){
            queryWrapper.from(tableName);
        }
    }
}
