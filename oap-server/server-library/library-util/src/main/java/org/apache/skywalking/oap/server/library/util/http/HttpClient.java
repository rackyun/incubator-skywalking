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

package org.apache.skywalking.oap.server.library.util.http;

import org.apache.http.*;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

/**
 * @author yunhai.hu
 * at 2019/4/22
 */
public class HttpClient {

    private static final Logger logger = LoggerFactory.getLogger(HttpClient.class);

    public static String get(String url, List<NameValuePair> params) throws IOException {
        HttpGet httpget = new HttpGet(url);
        fillParams(url, params, httpget);
        return execute(url, httpget);
    }

    private static String execute(String url, HttpUriRequest httpRequest) {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        try {
            try (CloseableHttpResponse response = httpClient.execute(httpRequest)) {
                StatusLine statusLine = response.getStatusLine();
                if (HttpStatus.SC_OK != statusLine.getStatusCode()) {
                    logger.error("request url {} occurred error, reason {} code {}",
                            url, statusLine.getReasonPhrase(), statusLine.getStatusCode());
                    logger.info("error content {}", EntityUtils.toString(response.getEntity()));
                    return null;
                }
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    return EntityUtils.toString(entity);
                }
            }
        } catch (Exception e) {
            logger.warn("bad url=" + url, e);
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                logger.warn("bad url=" + url, e);
            }
        }
        return null;
    }

    public static String jsonPost(String url, List<NameValuePair> params, String content) throws IOException {
        HttpPost httpPost = generateHttpPost(url, params, content);
        return execute(url, httpPost);
    }

    private static HttpPost generateHttpPost(String url, List<NameValuePair> params, String content) throws IOException {
        HttpPost httpPost = new HttpPost(url);
        fillParams(url, params, httpPost);
        StringEntity contentEntity = new StringEntity(content, ContentType.APPLICATION_JSON);
        contentEntity.setChunked(false);
        httpPost.setEntity(contentEntity);
        return httpPost;
    }

    private static void fillParams(String url, List<NameValuePair> params, HttpRequestBase httpRequestBase) throws IOException {
        if (params != null) {
            String paramStr = EntityUtils.toString(new UrlEncodedFormEntity(params));
            try {
                httpRequestBase.setURI(new URI(httpRequestBase.getURI().toString() + "?" + paramStr));
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("url " + url + " is illegal");
            }
        }
    }

    private static CloseableHttpAsyncClient ASYNC_CLIENT = HttpAsyncClients.createDefault();
    static {
        ASYNC_CLIENT.start();
    }

    private static Future<HttpResponse> asyncJsonPost(String url, List<NameValuePair> params, String content,
                                                      FutureCallback<HttpResponse> callback) throws IOException {
        logger.debug("async http request, url={} content={}", url, content);
        HttpPost httpPost = generateHttpPost(url, params, content);
        return ASYNC_CLIENT.execute(httpPost, callback);
    }

    public static List<String> multiJsonPost(String url, List<NameValuePair> params, List<String> contents) {
        CountDownLatch latch = new CountDownLatch(contents.size());
        List<String> result = new ArrayList<>();
        try {
            for (String content : contents) {
                asyncJsonPost(url, params, content, new FutureCallback<HttpResponse>() {
                    @Override
                    public void completed(HttpResponse response) {
                        StatusLine statusLine = response.getStatusLine();
                        if (HttpStatus.SC_OK != statusLine.getStatusCode()) {
                            logger.error("request url {} occurred error, reason {} code {}",
                                    url, statusLine.getReasonPhrase(), statusLine.getStatusCode());
                            return;
                        }
                        HttpEntity entity = response.getEntity();
                        if (entity != null) {
                            try {
                                result.add(EntityUtils.toString(entity));
                            } catch (IOException e) {
                                logger.error("read response occur error", e);
                            }
                        }
                        latch.countDown();
                    }

                    @Override
                    public void failed(Exception ex) {
                        logger.error("request " + url + " occur error", ex);
                        latch.countDown();
                    }

                    @Override
                    public void cancelled() {
                        latch.countDown();
                    }
                });
            }
            latch.await();
        } catch (Exception e) {
            logger.error("wait async http request occur error", e);
        }
        return result;
    }
}
