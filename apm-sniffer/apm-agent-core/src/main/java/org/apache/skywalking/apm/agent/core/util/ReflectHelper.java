/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.skywalking.apm.agent.core.util;

import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * @author yunhai.hu
 * at 2019/1/15
 */
public class ReflectHelper {

    private static final ILog logger = LogManager.getLogger(ReflectHelper.class);
    private Map<String, CacheItem<Method>> methodCache;
    private Class<?> clazz;

    public ReflectHelper(String clazzName) throws ClassNotFoundException {
        clazz = Class.forName(clazzName);
        methodCache = new HashMap<String, CacheItem<Method>>();
    }

    public Object invoke(String methodName, Object realObject, Object... args) throws InvocationTargetException,
            IllegalAccessException {
        Method method = getMethod(methodName);
        if (method == null) {
            logger.warn("method {} not found", methodName);
            return null;
        }
        return method.invoke(realObject, args);
    }

    public boolean isInstance(Object realObject) {
        return clazz.isInstance(realObject);
    }

    private Method getMethod(String methodName) {
        CacheItem<Method> methodCacheItem = methodCache.get(methodName);
        Method method = null;
        if (methodCacheItem == null) {
            try {
                method = clazz.getMethod(methodName);
            } catch (NoSuchMethodException e) {
                logger.error(e, "reflect {}  method {} error", clazz.getName(), methodName);
            }
            methodCache.put(methodName, new CacheItem<Method>(method));
        } else {
            method = methodCacheItem.getItem();
        }
        return method;
    }

    static class CacheItem<T> {
        private T item;

        public CacheItem(T item) {
            this.item = item;
        }

        public T getItem() {
            return item;
        }
    }
}
