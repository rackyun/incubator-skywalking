/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.skywalking.apm.plugin.spring.mvc.commons.interceptor;

import java.lang.reflect.Method;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.XIdCarrierItem;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.util.OperationNameUtil;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.apache.skywalking.apm.plugin.spring.mvc.commons.EnhanceRequireObjectCache;
import org.apache.skywalking.apm.util.StringUtil;

import static org.apache.skywalking.apm.plugin.spring.mvc.commons.Constants.*;

/**
 * the abstract method inteceptor
 */
public abstract class AbstractMethodInterceptor implements InstanceMethodsAroundInterceptor {
    public abstract String getRequestURL(Method method);

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
        MethodInterceptResult result) throws Throwable {

        Boolean forwardRequestFlag = (Boolean)ContextManager.getRuntimeContext().get(FORWARD_REQUEST_FLAG);
        /**
         * Spring MVC plugin do nothing if current request is forward request.
         * Ref: https://github.com/apache/incubator-skywalking/pull/1325
         */
        if (forwardRequestFlag != null && forwardRequestFlag) {
            return;
        }

        EnhanceRequireObjectCache pathMappingCache = (EnhanceRequireObjectCache)objInst.getSkyWalkingDynamicField();
        String requestURL = pathMappingCache.findPathMapping(method);
        if (requestURL == null) {
            requestURL = getRequestURL(method);
            pathMappingCache.addPathMapping(method, requestURL);
            requestURL = pathMappingCache.findPathMapping(method);
        }

        HttpServletRequest request = (HttpServletRequest)ContextManager.getRuntimeContext().get(REQUEST_KEY_IN_RUNTIME_CONTEXT);
        if (request != null) {
            requestURL = OperationNameUtil.normalizeUrl(requestURL);
            ContextCarrier contextCarrier = new ContextCarrier();
            CarrierItem next = contextCarrier.items();
            while (next.hasNext()) {
                next = next.next();
                next.setHeadValue(request.getHeader(next.getHeadKey()));
            }

            setXIdCarrier(contextCarrier, request, requestURL);

            AbstractSpan span = ContextManager.createEntrySpan(requestURL, contextCarrier);
            Tags.URL.set(span, request.getRequestURL().toString());
            Tags.HTTP.METHOD.set(span, request.getMethod());
            String remoteAddr = StringUtil.isEmpty(request.getHeader(X_FORWARD_HEADER)) ?
                    (request.getRemoteAddr() + ":" + request.getRemotePort()) : (request.getHeader(X_FORWARD_HEADER));
            Tags.SENDER_HOST.set(span, remoteAddr);
            span.setComponent(ComponentsDefine.SPRING_MVC_ANNOTATION);
            SpanLayer.asHttp(span);
        }
    }

    private void setXIdCarrier(ContextCarrier contextCarrier, HttpServletRequest request, String requestURL) {
        String requestId = request.getHeader(XIdCarrierItem.HEADER_NAME);
        if (!contextCarrier.isValid() && !StringUtil.isEmpty(requestId)) {
            XIdCarrierItem.XIdCarrierValue carrierValue =
                    new XIdCarrierItem.XIdCarrierValue(requestURL,
                            request.getRemoteHost(), request.getHeader(XIdCarrierItem.DEVICE_NAME));
            XIdCarrierItem carrierItem = new XIdCarrierItem(contextCarrier, null, carrierValue);
            carrierItem.setHeadValue(requestId);
        }
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
        Object ret) throws Throwable {
        Boolean forwardRequestFlag = (Boolean)ContextManager.getRuntimeContext().get(FORWARD_REQUEST_FLAG);
        /**
         * Spring MVC plugin do nothing if current request is forward request.
         * Ref: https://github.com/apache/incubator-skywalking/pull/1325
         */
        if (forwardRequestFlag != null && forwardRequestFlag) {
            return ret;
        }

        HttpServletResponse response = (HttpServletResponse)ContextManager.getRuntimeContext().get(RESPONSE_KEY_IN_RUNTIME_CONTEXT);
        try {
            if (response != null) {
                AbstractSpan span = ContextManager.activeSpan();
                if (response.getStatus() >= 400) {
                    span.errorOccurred();
                    Tags.STATUS_CODE.set(span, Integer.toString(response.getStatus()));
                }
                ContextManager.stopSpan();
            }
        } finally {
            ContextManager.getRuntimeContext().remove(REQUEST_KEY_IN_RUNTIME_CONTEXT);
            ContextManager.getRuntimeContext().remove(RESPONSE_KEY_IN_RUNTIME_CONTEXT);
        }

        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
        Class<?>[] argumentsTypes, Throwable t) {
        ContextManager.activeSpan().errorOccurred().log(t);
    }
}
