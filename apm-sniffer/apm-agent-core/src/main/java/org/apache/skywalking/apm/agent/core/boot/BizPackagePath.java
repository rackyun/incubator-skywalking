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

package org.apache.skywalking.apm.agent.core.boot;

import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

public class BizPackagePath {
    private static final ILog logger = LogManager.getLogger(BizPackagePath.class);

    private static File BIZ_PACKAGE_PATH;

    public static File getPath() throws AgentPackageNotFoundException {
        if (BIZ_PACKAGE_PATH == null) {
            BIZ_PACKAGE_PATH = findPath();
        }
        return BIZ_PACKAGE_PATH;
    }

    public static boolean isPathFound() {
        return BIZ_PACKAGE_PATH != null;
    }

    private static File findPath() throws AgentPackageNotFoundException {
        URL resource = ((URLClassLoader) AgentPackagePath.class.getClassLoader()).getURLs()[0];
        if (resource != null) {
            String urlString = resource.toString();

            logger.debug("The biz class location is {}.", urlString);

            urlString = urlString.substring(urlString.indexOf("file:"));

            File agentJarFile = null;
            try {
                agentJarFile = new File(new URL(urlString).getFile());
            } catch (MalformedURLException e) {
                logger.error(e, "Can not locate agent jar file by url:" + urlString);
            }

            if (agentJarFile.exists()) {
                return agentJarFile.getParentFile();
            }
        }

        logger.error("Can not locate agent jar file.");
        throw new AgentPackageNotFoundException("Can not locate agent jar file.");
    }
}
