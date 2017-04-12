package com.wfj.monitor.transform;

import com.wfj.monitor.conf.SystemPropertyConfig;

import java.util.HashSet;
import java.util.Set;

import static com.wfj.monitor.common.Constant.SYSTEM_SETTING_EXCLUDE_PACKAGE;
import static com.wfj.monitor.common.Constant.SYSTEM_SETTING_INCLUDE_PACKAGE;
import static com.wfj.monitor.common.Constant.SYSTEM_SETTING_EXCLUDE_CLASS_LOADER;

/**
 * <br>created at 17-3-22
 *
 * @author liuxh
 * @since 1.0.0
 */
public class ApmFilter {
    private static Set<String> excludePackage = new HashSet<>();
    private static Set<String> includePackage = new HashSet<>();
    private static Set<String> excludeClassLoader = new HashSet<>();

    static {
        addExcludePackage("java/");
        addExcludePackage("sun/");
        addExcludePackage("com/sun/");
        addExcludePackage("javassist");
        String excludePackageStr = SystemPropertyConfig.getContextProperty(SYSTEM_SETTING_EXCLUDE_PACKAGE);
        if (excludePackageStr != null && !excludePackageStr.isEmpty()) {
            for (String p : excludePackageStr.split(",")) {
                addExcludePackage(p.trim());
            }
        }

        String includePackageStr = SystemPropertyConfig.getContextProperty(SYSTEM_SETTING_INCLUDE_PACKAGE);
        if (includePackageStr != null && !includePackageStr.isEmpty()) {
            for (String p : includePackageStr.split(",")) {
                addIncludePackage(p.trim());
            }
        }

        String excludeClassLoaderStr = SystemPropertyConfig.getContextProperty(SYSTEM_SETTING_EXCLUDE_CLASS_LOADER);
        if (excludeClassLoaderStr != null && !excludeClassLoaderStr.isEmpty()) {
            for (String p : excludeClassLoaderStr.split(",")) {
                addExcludeClassLoader(p.trim());
            }
        }
    }

    public static void addExcludePackage(String p) {
        if (p == null || p.isEmpty()) {
            return;
        }
        excludePackage.add(p.replaceAll("\\.", "/"));
    }

    public static void addIncludePackage(String p) {
        if (p == null || p.isEmpty()) {
            return;
        }
        includePackage.add(p.replaceAll("\\.", "/"));
    }

    public static void addExcludeClass(String c) {
        if (c == null || c.isEmpty()) {
            return;
        }
        excludePackage.add(c.replaceAll("\\.", "/"));
    }

    public static void addExcludeClassLoader(String l) {
        if (l == null || l.isEmpty()) {
            return;
        }
        excludeClassLoader.add(l);
    }

    public static boolean isNotNeedInject(String className) {
        if (className == null || className.isEmpty()) {
            return true;
        }
        String _className = className.replaceAll("\\.", "/");
        for (String exclude : excludePackage) {
            if (_className.startsWith(exclude)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isNeedInject(String className) {
        if (className == null || className.isEmpty()) {
            return true;
        }
        String _className = className.replaceAll("\\.", "/");
        for (String exclude : includePackage) {
            if (_className.startsWith(exclude)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isNotNeedInjectClassLoader(String classLoader) {
        return classLoader == null || classLoader.isEmpty() || excludeClassLoader.contains(classLoader);
    }
}
