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
package com.mybatisflex.core.keygen;

import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.core.FlexConsts;
import com.mybatisflex.core.FlexGlobalConfig;
import com.mybatisflex.core.exception.FlexExceptions;
import com.mybatisflex.core.table.IdInfo;
import com.mybatisflex.core.table.TableInfo;
import com.mybatisflex.core.util.StringUtil;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.reflection.invoker.Invoker;
import org.apache.ibatis.session.Configuration;

import java.sql.Statement;
import java.util.Map;

/**
 * 通过 java 编码的方式生成主键
 * 当主键类型配置为 KeyType#Generator 时，使用此生成器生成
 * {@link KeyType#Generator}
 */
public class CustomKeyGenerator implements KeyGenerator {

    protected Configuration configuration;
    protected IKeyGenerator keyGenerator;
    protected TableInfo tableInfo;
    protected IdInfo idInfo;


    public CustomKeyGenerator(Configuration configuration, TableInfo tableInfo, IdInfo idInfo) {
        this.configuration = configuration;
        FlexGlobalConfig.KeyConfig globalKeyConfig = FlexGlobalConfig.getConfig(configuration).getKeyConfig();
        String keyValue = MybatisKeyGeneratorUtil.getKeyValue(idInfo, globalKeyConfig);
        this.keyGenerator = KeyGeneratorFactory.getKeyGenerator(keyValue);
        this.tableInfo = tableInfo;
        this.idInfo = idInfo;

        ensuresKeyGeneratorNotNull();
    }

    private void ensuresKeyGeneratorNotNull() {
        if (keyGenerator == null) {
            throw FlexExceptions.wrap("The name of \"%s\" key generator not exist.\n" +
                            "please check annotation @Id(value=\"%s\") at field: %s#%s"
                    , idInfo.getValue(), idInfo.getValue(), tableInfo.getEntityClass().getName(), idInfo.getProperty());
        }
    }


    @Override
    public void processBefore(Executor executor, MappedStatement ms, Statement stmt, Object parameter) {
        Object entity = ((Map) parameter).get(FlexConsts.ENTITY);
        Object generateId = keyGenerator.generate(entity, idInfo.getColumn());
        try {
            Invoker setInvoker = tableInfo.getReflector().getSetInvoker(idInfo.getProperty());
            Object id = convert(generateId, setInvoker.getType());
            setInvoker.invoke(entity, new Object[]{id});
        } catch (Exception e) {
            throw FlexExceptions.wrap(e);
        }
    }


    public Object convert(Object value, Class<?> targetClass) {
        if (value == null || (value.getClass() == String.class && StringUtil.isBlank((String) value)
                && targetClass != String.class)) {
            return null;
        }
        if (value.getClass().isAssignableFrom(targetClass)) {
            return value;
        }
        if (targetClass == String.class) {
            return value.toString();
        } else if (targetClass == Long.class || targetClass == long.class) {
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            return Long.parseLong(value.toString());
        } else if (targetClass == java.math.BigInteger.class) {
            return new java.math.BigInteger(value.toString());
        } else if (targetClass == java.math.BigDecimal.class) {
            return new java.math.BigDecimal(value.toString());
        } else if (targetClass == Integer.class || targetClass == int.class) {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return Integer.parseInt(value.toString());
        } else if (targetClass == Double.class || targetClass == double.class) {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            return Double.parseDouble(value.toString());
        } else if (targetClass == Float.class || targetClass == float.class) {
            if (value instanceof Number) {
                return ((Number) value).floatValue();
            }
            return Float.parseFloat(value.toString());
        } else if (targetClass == Short.class || targetClass == short.class) {
            if (value instanceof Number) {
                return ((Number) value).shortValue();
            }
            return Short.parseShort(value.toString());
        }

        throw FlexExceptions.wrap("\"%s\" can not be parsed for primary key.", value);
    }


    @Override
    public void processAfter(Executor executor, MappedStatement ms, Statement stmt, Object parameter) {
        //do nothing
    }
}
