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
package com.mybatisflex.core.provider;

import com.mybatisflex.core.FlexConsts;
import com.mybatisflex.core.exception.FlexExceptions;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.row.Row;
import com.mybatisflex.core.table.TableInfo;
import com.mybatisflex.core.table.TableInfos;
import com.mybatisflex.core.util.StringUtil;
import org.apache.ibatis.builder.annotation.ProviderContext;

import java.util.Collection;
import java.util.List;
import java.util.Map;

class ProviderUtil {

    private static final Object[] NULL_ARGS = new Object[0];

    public static String getSqlString(Map params) {
        return (String) params.get(FlexConsts.SQL);
    }

    public static void setSqlArgs(Map params, Object[] args) {
        params.put(FlexConsts.SQL_ARGS, args);
    }

    public static String getTableName(Map params) {
        return params.get(FlexConsts.TABLE_NAME).toString().trim();
    }

    public static String[] getPrimaryKeys(Map params) {
        String primaryKey = (String) params.get(FlexConsts.PRIMARY_KEY);
        if (StringUtil.isBlank(primaryKey)) {
            throw FlexExceptions.wrap("primaryKey can not be null or blank.");
        }
        String[] primaryKeys = primaryKey.split(",");
        for (int i = 0; i < primaryKeys.length; i++) {
            primaryKeys[i] = primaryKeys[i].trim();
        }
        return primaryKeys;
    }

    public static Object[] getPrimaryValues(Map params) {
        Object primaryValue = params.get(FlexConsts.PRIMARY_VALUE);
        if (primaryValue == null) {
            return NULL_ARGS;
        }
        if (primaryValue.getClass().isArray()) {
            return (Object[]) primaryValue;
        } else if (primaryValue instanceof Collection) {
            return ((Collection<?>) primaryValue).toArray();
        } else {
            return new Object[]{primaryValue};
        }
    }

    public static QueryWrapper getQueryWrapper(Map params) {
        return (QueryWrapper) params.get(FlexConsts.QUERY);
    }

    public static Row getRow(Map params) {
        return (Row) params.get(FlexConsts.ROW);
    }

    public static List<Row> getRows(Map params) {
        return (List<Row>) params.get(FlexConsts.ROWS);
    }

    public static TableInfo getTableInfo(ProviderContext context){
        return TableInfos.ofMapperClass(context.getMapperType());
    }

    public static Object getEntity(Map params) {
        return params.get(FlexConsts.ENTITY);
    }


    public static List<Object> getEntities(Map params) {
        return (List<Object>) params.get(FlexConsts.ENTITIES);
    }

    public static boolean isIgnoreNulls(Map params){
        return params.containsKey(FlexConsts.IGNORE_NULLS) && (boolean) params.get(FlexConsts.IGNORE_NULLS);
    }


}