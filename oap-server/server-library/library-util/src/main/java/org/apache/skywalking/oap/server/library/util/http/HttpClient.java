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

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * @author yunhai.hu
 * at 2019/4/22
 */
public class HttpClient {

    private static final Logger logger = LoggerFactory.getLogger(HttpClient.class);

    public static String get(String url, List<NameValuePair> params) throws IOException {
        HttpGet httpget = new HttpGet(url);
        if (params != null) {
            String paramStr = EntityUtils.toString(new UrlEncodedFormEntity(params));
            try {
                httpget.setURI(new URI(httpget.getURI().toString() + "?" + paramStr));
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("url " + url + " is illegal");
            }
        }

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
        HttpPost httpPost = new HttpPost(url);
        if (params != null) {
            String paramStr = EntityUtils.toString(new UrlEncodedFormEntity(params));
            try {
                httpPost.setURI(new URI(httpPost.getURI().toString() + "?" + paramStr));
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("url " + url + " is illegal");
            }
        }
        httpPost.setEntity(new StringEntity(content, ContentType.APPLICATION_JSON));
        return execute(url, httpPost);
    }

}
