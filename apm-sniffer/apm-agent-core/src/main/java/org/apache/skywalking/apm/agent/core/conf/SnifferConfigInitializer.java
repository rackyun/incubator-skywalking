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

package org.apache.skywalking.apm.agent.core.conf;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.apache.skywalking.apm.agent.core.boot.AgentPackageNotFoundException;
import org.apache.skywalking.apm.agent.core.boot.AgentPackagePath;
import org.apache.skywalking.apm.agent.core.boot.BizPackagePath;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.util.ConfigInitializer;
import org.apache.skywalking.apm.util.PropertyPlaceholderHelper;
import org.apache.skywalking.apm.util.StringUtil;

/**
 * The <code>SnifferConfigInitializer</code> initializes all configs in several way.
 *
 * @author wusheng
 */
public class SnifferConfigInitializer {
    private static final ILog logger = LogManager.getLogger(SnifferConfigInitializer.class);
    private static String SPECIFIED_CONFIG_PATH = "skywalking_config";
    private static String DEFAULT_CONFIG_FILE_NAME = "/config/agent.config";
    private static String ENV_KEY_PREFIX = "skywalking.";
    private static String BOOTSTRAP_FILE_NAME = "bootstrap.properties";
    private static String APPLICATION_NAME_KEY = "spring.application.name";
    private static boolean IS_INIT_COMPLETED = false;

    /**
     * If the specified agent config path is set, the agent will try to locate the specified agent config. If the
     * specified agent config path is not set , the agent will try to locate `agent.config`, which should be in the
     * /config dictionary of agent package.
     * <p>
     * Also try to override the config by system.env and system.properties. All the keys in these two places should
     * start with {@link #ENV_KEY_PREFIX}. e.g. in env `skywalking.agent.application_code=yourAppName` to override
     * `agent.application_code` in config file.
     * <p>
     * At the end, `agent.application_code` and `collector.servers` must be not blank.
     */
    public static void initialize(String agentOptions) throws ConfigNotFoundException, AgentPackageNotFoundException {
        InputStreamReader configFileStream;

        // Reads the business Jar package directory so that it can be written to the business directory when the log is initialized
        try {
            BizPackagePath.getPath();
        } catch (Exception e) {
            logger.warn(e, "Failed to read the biz file path, skywalking is going use default log path.");
        }

        try {
            configFileStream = loadConfig();
            Properties properties = new Properties();
            properties.load(configFileStream);
            for (String key : properties.stringPropertyNames()) {
                String value = (String)properties.get(key);
                //replace the key's value. properties.replace(key,value) in jdk8+
                properties.put(key, PropertyPlaceholderHelper.INSTANCE.replacePlaceholders(value, properties));
            }
            ConfigInitializer.initialize(properties, Config.class);
        } catch (Exception e) {
            logger.error(e, "Failed to read the config file, skywalking is going to run in default config.");
        }

        try {
            overrideConfigBySystemProp();
        } catch (Exception e) {
            logger.error(e, "Failed to read the system env.");
        }

        //todo
        String appName = readApplicationName();
        if (appName != null) {
            Config.Agent.SERVICE_NAME = appName;
        }

        if (!StringUtil.isEmpty(agentOptions)) {
            try {
                agentOptions = agentOptions.trim();
                logger.info("Agent options is {}.", agentOptions);

                overrideConfigByAgentOptions(agentOptions);
            } catch (Exception e) {
                logger.error(e, "Failed to parse the agent options, val is {}.", agentOptions);
            }
        }

        logger.info("Apm agent version is {}.", readVersion());

        if (StringUtil.isEmpty(Config.Agent.SERVICE_NAME)) {
            throw new ExceptionInInitializerError("`agent.service_code` is missing.");
        }

        String appEnv = System.getenv("APP_ENV");
        if (StringUtil.isEmpty(appEnv)) {
            appEnv = "dev";
        }
        String backendService = selectBackendService(appEnv);
        if (StringUtil.isEmpty(backendService)) {
            throw new ExceptionInInitializerError("`collector.direct_servers` and `collector.servers` cannot be empty at the same time.");
        }
        Config.Collector.BACKEND_SERVICE = backendService;

        IS_INIT_COMPLETED = true;
    }

    private static String selectBackendService(String appEnv) {

        switch (AppEnv.valueOfName(appEnv)) {
            case DEV:
                return Config.Collector.BACKEND_SERVICE_DEV;
            case PRE:
                return Config.Collector.BACKEND_SERVICE_PRE;
            case ONLINE:
                return Config.Collector.BACKEND_SERVICE_ONLINE;
        }
        return null;
    }

    private enum AppEnv {
        DEV("dev"), PRE("pre"), ONLINE("online");

        private String name;

        AppEnv(String name) {
            this.name = name;
        }

        static AppEnv valueOfName(String name) {
            for (AppEnv appEnv : AppEnv.values()) {
                if (appEnv.name.equals(name)) {
                    return appEnv;
                }
            }
            throw new ExceptionInInitializerError("APP_ENV " + name + " is illegal");
        }
    }

    private static void overrideConfigByAgentOptions(String agentOptions) throws IllegalAccessException {
        Properties properties = new Properties();
        for (List<String> terms : parseAgentOptions(agentOptions)) {
            if (terms.size() != 2) {
                throw new IllegalArgumentException("[" + terms + "] is not a key-value pair.");
            }
            properties.put(terms.get(0), terms.get(1));
        }
        if (!properties.isEmpty()) {
            ConfigInitializer.initialize(properties, Config.class);
        }
    }

    private static List<List<String>> parseAgentOptions(String agentOptions) {
        List<List<String>> options = new ArrayList<List<String>>();
        List<String> terms = new ArrayList<String>();
        boolean isInQuotes = false;
        StringBuilder currentTerm = new StringBuilder();
        for (char c : agentOptions.toCharArray()) {
            if (c == '\'' || c == '"') {
                isInQuotes = !isInQuotes;
            } else if (c == '=' && !isInQuotes) {   // key-value pair uses '=' as separator
                terms.add(currentTerm.toString());
                currentTerm = new StringBuilder();
            } else if (c == ',' && !isInQuotes) {   // multiple options use ',' as separator
                terms.add(currentTerm.toString());
                currentTerm = new StringBuilder();

                options.add(terms);
                terms = new ArrayList<String>();
            } else {
                currentTerm.append(c);
            }
        }
        // add the last term and option without separator
        terms.add(currentTerm.toString());
        options.add(terms);
        return options;
    }

    public static boolean isInitCompleted() {
        return IS_INIT_COMPLETED;
    }

    /**
     * Override the config by system properties. The env key must start with `skywalking`, the reuslt should be as same
     * as in `agent.config`
     * <p>
     * such as: Env key of `agent.application_code` shoule be `skywalking.agent.application_code`
     *
     * @return the config file {@link InputStream}, or null if not needEnhance.
     */
    private static void overrideConfigBySystemProp() throws IllegalAccessException {
        Properties properties = new Properties();
        Properties systemProperties = System.getProperties();
        Iterator<Map.Entry<Object, Object>> entryIterator = systemProperties.entrySet().iterator();
        while (entryIterator.hasNext()) {
            Map.Entry<Object, Object> prop = entryIterator.next();
            if (prop.getKey().toString().startsWith(ENV_KEY_PREFIX)) {
                String realKey = prop.getKey().toString().substring(ENV_KEY_PREFIX.length());
                properties.put(realKey, prop.getValue());
            }
        }

        if (!properties.isEmpty()) {
            ConfigInitializer.initialize(properties, Config.class);
        }
    }

    /**
     * Load the specified config file or default config file
     *
     * @return the config file {@link InputStream}, or null if not needEnhance.
     */
    private static InputStreamReader loadConfig() throws AgentPackageNotFoundException, ConfigNotFoundException, ConfigReadFailedException {

        String specifiedConfigPath = System.getProperties().getProperty(SPECIFIED_CONFIG_PATH);
        File configFile = StringUtil.isEmpty(specifiedConfigPath) ? new File(AgentPackagePath.getPath(), DEFAULT_CONFIG_FILE_NAME) : new File(specifiedConfigPath);

        if (configFile.exists() && configFile.isFile()) {
            try {
                logger.info("Config file found in {}.", configFile);

                return new InputStreamReader(new FileInputStream(configFile), "UTF-8");
            } catch (FileNotFoundException e) {
                throw new ConfigNotFoundException("Fail to load agent.config", e);
            } catch (UnsupportedEncodingException e) {
                throw new ConfigReadFailedException("Fail to load agent.config", e);
            }
        }
        throw new ConfigNotFoundException("Fail to load agent config file.");
    }

    /**
     * The application name of the service is read from the Jar package of the business
     *
     * @return
     */
    private static String readApplicationName() {
        String appName = null;
        try {
            Properties prop = new Properties();
            URL bizJarUrl =
                    ((URLClassLoader) ConfigInitializer.class.getClassLoader()).getURLs()[0];
            JarFile bizJar = new JarFile(bizJarUrl.getFile());

            // File bootstrap.properties may exist in:
            // BOOT-INF/classes/bootstrap.properties
            // WEB-INF/classes/bootstrap.properties
            // bootstrap.properties
            ZipEntry bootfile = null;
            Enumeration<JarEntry> entries = bizJar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(BOOTSTRAP_FILE_NAME)) {
                    bootfile = bizJar.getEntry(entry.getName());
                    break;
                }
            }

            InputStream input = bizJar.getInputStream(bootfile);
            prop.load(input);
            appName = prop.getProperty(APPLICATION_NAME_KEY);
        } catch (Exception e) {
            logger.error("Load bootstrap.properties error", e);
        }
        return appName;
    }

    private static String readVersion() {
        try {
            Properties prop = new Properties();
            prop.load(SnifferConfigInitializer.class.getResourceAsStream(
                        "/META-INF/maven/org.apache.skywalking/apm-agent-core/pom.properties"));
            return prop.getProperty("version");
        } catch (IOException e) {
            logger.error("read pom.properties failed", e);
        }
        return "";
    }
}
