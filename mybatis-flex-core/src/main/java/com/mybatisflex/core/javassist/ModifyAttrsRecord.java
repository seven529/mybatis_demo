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
package com.mybatisflex.core.javassist;

import org.apache.ibatis.javassist.util.proxy.ProxyObject;

import java.io.Serializable;
import java.util.Set;

public interface ModifyAttrsRecord extends Serializable {

    /**
     * 注意：
     * 对于 entity 来说，这里存放的只是 属性的名称，而非字段
     * 对于 row 来说，存放的则是 字段 名称
     */
    default Set<String> getModifyAttrs(){
        ModifyAttrsRecordHandler handler = (ModifyAttrsRecordHandler) ((ProxyObject) this).getHandler();
        return handler.getModifyAttrs();
    }

    default void addModifyAttr(String attr) {
        getModifyAttrs().add(attr);
    }

    default void removeModifyAttr(String attr) {
        getModifyAttrs().remove(attr);
    }

    default Set<String> obtainModifyAttrs() {
        return getModifyAttrs();
    }

    default void clearModifyFlag() {
        getModifyAttrs().clear();
    }


}
