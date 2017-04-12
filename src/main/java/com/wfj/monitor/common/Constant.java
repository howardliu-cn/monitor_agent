package com.wfj.monitor.common;

import javax.servlet.ServletContext;
import java.util.UUID;

/**
 * <br>created at 17-4-11
 *
 * @author liuxh
 * @version 1.0.0
 * @since 1.0.0
 */
public class Constant {
    public static final String THIS_TAG = UUID.randomUUID().toString();
    public static final String HEADER_SERVER_TAG = "Server-Tag";

    public static final String DEFAULT_ENV_PROPERTIES_FILE = "/env/monitor-env-setting-default.properties";
    public static final String DEFAULT_MONITOR_PROPERTIES_FILE = "/env/default-wfj-monitor.properties";
    public static final String CUSTOM_MONITOR_PROPERTIES_FILE = "/wfj-monitor.properties";

    public static final String SYSTEM_SETTING_MONITOR_IS_DEBUG = "system.setting.monitor.isDebug";
    public static final String SYSTEM_SETTING_EXCLUDE_PACKAGE = "system.setting.exclude.package";
    public static final String SYSTEM_SETTING_INCLUDE_PACKAGE = "system.setting.include.package";
    public static final String SYSTEM_SETTING_EXCLUDE_CLASS_LOADER = "system.setting.exclude.ClassLoader";

    public static Boolean IS_DEBUG = false;

    public static ServletContext servletContext = null;
}
