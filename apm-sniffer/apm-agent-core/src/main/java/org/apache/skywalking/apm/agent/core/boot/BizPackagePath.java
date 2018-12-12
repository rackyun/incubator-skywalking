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
